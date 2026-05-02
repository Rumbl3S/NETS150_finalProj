package com.nets150.recommender.service;

import com.nets150.recommender.algo.GraphTraversal;
import com.nets150.recommender.algo.ShortestPathRecommender;
import com.nets150.recommender.data.DatasetLoader;
import com.nets150.recommender.data.GraphCache;
import com.nets150.recommender.graph.MovieGraphBuilder;
import com.nets150.recommender.graph.WeightedGraph;
import com.nets150.recommender.model.Movie;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads local CSV data, builds or restores the weighted graph, and runs traversal and recommendation algorithms.
 */
public final class RecommenderService {
    private final Path dataDir;
    private final Map<Integer, Movie> moviesById;
    private final WeightedGraph graph;
    private final Map<Integer, Map<Integer, Double>> ratingsByMovie;

    public RecommenderService(Path dataDir, List<Movie> movies, WeightedGraph graph, Map<Integer, Map<Integer, Double>> ratingsByMovie) {
        this.dataDir = dataDir;
        this.moviesById = movies.stream().collect(Collectors.toMap(Movie::getId, m -> m, (a, b) -> a, HashMap::new));
        this.graph = graph;
        this.ratingsByMovie = ratingsByMovie;
    }

    public static RecommenderService load(Path dataDir, boolean useCacheIfPresent) throws IOException, ClassNotFoundException {
        Path moviesCsv = dataDir.resolve("movies.csv");
        Path ratingsCsv = dataDir.resolve("ratings.csv");
        Path peopleCsv = dataDir.resolve("movie_people.csv");
        Path cacheFile = dataDir.resolve("cache").resolve("graph_cache.ser");

        if (useCacheIfPresent && Files.exists(cacheFile)) {
            GraphCache.CachedState state = GraphCache.load(cacheFile);
            List<Movie> movies = state.movies();
            Map<Integer, Map<Integer, Double>> ratingsByMovie = DatasetLoader.ratingsByMovie(DatasetLoader.loadRatings(ratingsCsv));
            return new RecommenderService(dataDir, movies, state.graph(), ratingsByMovie);
        }

        List<Movie> movies = DatasetLoader.loadMovies(moviesCsv, peopleCsv);
        List<DatasetLoader.RatingRecord> ratings = DatasetLoader.loadRatings(ratingsCsv);
        Map<Integer, Map<Integer, Double>> ratingsByMovie = DatasetLoader.ratingsByMovie(ratings);
        MovieGraphBuilder builder = new MovieGraphBuilder(0.08, 2);
        WeightedGraph graph = builder.build(movies, ratingsByMovie);
        GraphCache.save(cacheFile, graph, movies);
        return new RecommenderService(dataDir, movies, graph, ratingsByMovie);
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

    public Path dataDirectory() {
        return dataDir;
    }
}
