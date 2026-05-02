package com.nets150.recommender.service;

import com.nets150.recommender.algo.GraphTraversal;
import com.nets150.recommender.algo.ShortestPathRecommender;
import com.nets150.recommender.data.DatasetLoader;
import com.nets150.recommender.data.DatasetPaths;
import com.nets150.recommender.data.GraphCache;
import com.nets150.recommender.data.RatingsReservoir;
import com.nets150.recommender.graph.MovieGraphBuilder;
import com.nets150.recommender.graph.SimilarityMetrics;
import com.nets150.recommender.graph.SparseMovieGraphBuilder;
import com.nets150.recommender.graph.WeightedGraph;
import com.nets150.recommender.model.Movie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Loads local CSV data, builds or restores the weighted graph, and runs traversal and recommendation algorithms.
 */
public final class RecommenderService {
    private final Path configDir;
    private final Path datasetRoot;
    private final boolean movieLens20M;
    private final Map<Integer, Movie> moviesById;
    private final WeightedGraph graph;
    private final Map<Integer, Map<Integer, Double>> ratingsByMovie;

    public RecommenderService(
            Path configDir,
            Path datasetRoot,
            List<Movie> movies,
            WeightedGraph graph,
            Map<Integer, Map<Integer, Double>> ratingsByMovie,
            boolean movieLens20M
    ) {
        this.configDir = configDir;
        this.datasetRoot = datasetRoot;
        this.movieLens20M = movieLens20M;
        this.moviesById = movies.stream().collect(Collectors.toMap(Movie::getId, m -> m, (a, b) -> a, HashMap::new));
        this.graph = graph;
        this.ratingsByMovie = ratingsByMovie;
    }

    public static RecommenderService load(Path configDir, boolean useCacheIfPresent) throws IOException, ClassNotFoundException {
        return load(configDir, useCacheIfPresent, s -> {});
    }

    public static RecommenderService load(Path configDir, boolean useCacheIfPresent, Consumer<String> status) throws IOException, ClassNotFoundException {
        Consumer<String> st = status != null ? status : s -> {};
        DatasetPaths paths = DatasetPaths.resolve(configDir);
        Path cacheFile = paths.root().resolve("cache").resolve("graph_cache.ser");

        if (paths.movieLens20M()) {
            st.accept("MovieLens 20M detected at: " + paths.root().toAbsolutePath());
        } else {
            st.accept("Dataset folder: " + paths.root().toAbsolutePath());
        }

        if (useCacheIfPresent && Files.exists(cacheFile)) {
            st.accept("Loading graph from cache (graph_cache.ser)…");
            GraphCache.CachedState state = GraphCache.load(cacheFile);
            WeightedGraph graph = state.graph();
            st.accept("Normalizing edge weights (fixes old caches with 0-weight edges)…");
            graph.mergeParallelEdgesAndClamp(SimilarityMetrics.MIN_EDGE_WEIGHT);
            List<Movie> movies = state.movies();
            Map<Integer, Map<Integer, Double>> ratings;
            if (paths.useLargeScalePipeline()) {
                long cap = RatingsReservoir.ratingFileLineLimit();
                st.accept(cap > 0
                        ? "Streaming ratings.csv (up to " + cap + " rows) into per-movie samples…"
                        : "Streaming full ratings.csv into per-movie samples…");
                ratings = RatingsReservoir.buildPerMovieSample(paths.ratingsCsv(), 500, cap);
            } else {
                ratings = DatasetLoader.ratingsByMovie(DatasetLoader.loadRatings(paths.ratingsCsv()));
            }
            st.accept("Ready — " + movies.size() + " movies.");
            return new RecommenderService(configDir, paths.root(), movies, graph, ratings, paths.movieLens20M());
        }

        st.accept("Reading movies.csv…");
        List<Movie> movies = DatasetLoader.loadMovies(paths.moviesCsv(), paths.moviePeopleCsv());

        Map<Integer, Map<Integer, Double>> ratings;
        WeightedGraph graph;
        if (paths.useLargeScalePipeline()) {
            long cap = RatingsReservoir.ratingFileLineLimit();
            st.accept(cap > 0
                    ? "Streaming ratings (up to " + cap + " rows; faster subset)…"
                    : "Streaming full ratings file (may take several minutes)…");
            ratings = RatingsReservoir.buildPerMovieSample(paths.ratingsCsv(), 500, cap);
            st.accept("Building sparse similarity graph (first run: several minutes)…");
            graph = SparseMovieGraphBuilder.movieLens20MDefault().build(movies, ratings, st);
        } else {
            st.accept("Loading ratings.csv…");
            List<DatasetLoader.RatingRecord> records = DatasetLoader.loadRatings(paths.ratingsCsv());
            ratings = DatasetLoader.ratingsByMovie(records);
            MovieGraphBuilder builder = new MovieGraphBuilder(0.08, 2);
            graph = builder.build(movies, ratings);
        }
        graph.mergeParallelEdgesAndClamp(SimilarityMetrics.MIN_EDGE_WEIGHT);

        st.accept("Writing graph cache…");
        GraphCache.save(cacheFile, graph, movies);
        st.accept("Ready — " + movies.size() + " movies.");
        return new RecommenderService(configDir, paths.root(), movies, graph, ratings, paths.movieLens20M());
    }

    public Path configDirectory() {
        return configDir;
    }

    public Path dataDirectory() {
        return datasetRoot;
    }

    public boolean isMovieLens20M() {
        return movieLens20M;
    }

    public List<Movie> allMovies() {
        return moviesById.values().stream().sorted((a, b) -> Integer.compare(a.getId(), b.getId())).toList();
    }

    public Movie movieById(int id) {
        return moviesById.get(id);
    }

    public List<Integer> bfsFrom(int movieId, int maxNodes) {
        return GraphTraversal.bfsOrder(graph, movieId, maxNodes);
    }

    public List<Integer> dfsFrom(int movieId, int maxNodes) {
        return GraphTraversal.dfsOrder(graph, movieId, maxNodes);
    }

    public List<ShortestPathRecommender.ScoredMovie> recommend(Set<Integer> seedIds, int topK) {
        return ShortestPathRecommender.recommend(graph, seedIds, topK);
    }

    public WeightedGraph graph() {
        return graph;
    }
}
