package com.nets150.recommender.graph;

import com.nets150.recommender.model.Movie;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Builds a sparse similarity graph suitable for MovieLens 20M scale: for each movie, consider genre neighbors,
 * refine with a two-stage score, then keep the strongest few edges per movie (merged globally by minimum weight).
 */
public final class SparseMovieGraphBuilder {
    private final int maxGenreNeighbors;
    private final int refineTopByGenre;
    private final int topEdgesPerMovie;
    private final int minCommonRaters;
    private final double minGenreJaccardQuick;
    private final double minCombinedSimilarity;

    public SparseMovieGraphBuilder(
            int maxGenreNeighbors,
            int refineTopByGenre,
            int topEdgesPerMovie,
            int minCommonRaters,
            double minGenreJaccardQuick,
            double minCombinedSimilarity
    ) {
        this.maxGenreNeighbors = maxGenreNeighbors;
        this.refineTopByGenre = refineTopByGenre;
        this.topEdgesPerMovie = topEdgesPerMovie;
        this.minCommonRaters = minCommonRaters;
        this.minGenreJaccardQuick = minGenreJaccardQuick;
        this.minCombinedSimilarity = minCombinedSimilarity;
    }

    public static SparseMovieGraphBuilder movieLens20MDefault() {
        // Denser neighborhood + stricter overlap on ratings → stronger, more discriminative edges.
        return new SparseMovieGraphBuilder(3200, 220, 55, 3, 0.04, 0.052);
    }

    public WeightedGraph build(
            List<Movie> movies,
            Map<Integer, Map<Integer, Double>> ratingsByMovie,
            Consumer<String> status
    ) {
        Map<String, List<Integer>> genreIndex = buildGenreIndex(movies);
        Map<Integer, Movie> byId = new HashMap<>(Math.max(16, movies.size() * 2));
        for (Movie m : movies) {
            byId.put(m.getId(), m);
        }

        WeightedGraph g = new WeightedGraph();
        for (Movie m : movies) {
            g.addNode(m.getId());
        }

        int total = movies.size();
        for (int mi = 0; mi < movies.size(); mi++) {
            Movie m = movies.get(mi);
            if (mi % 300 == 0) {
                status.accept(String.format(Locale.ROOT, "Building sparse graph: %d / %d movies", mi + 1, total));
            }
            int mid = m.getId();
            Map<Integer, Integer> overlap = new HashMap<>();
            for (String genre : m.getGenres()) {
                List<Integer> ids = genreIndex.get(genre);
                if (ids == null) {
                    continue;
                }
                for (int oid : ids) {
                    if (oid == mid) {
                        continue;
                    }
                    overlap.merge(oid, 1, Integer::sum);
                }
            }
            List<Integer> cands = overlap.entrySet().stream()
                    .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                    .limit(maxGenreNeighbors)
                    .map(Map.Entry::getKey)
                    .toList();

            List<int[]> quick = new ArrayList<>();
            for (int oid : cands) {
                Movie o = byId.get(oid);
                if (o == null) {
                    continue;
                }
                double gj = SimilarityMetrics.jaccard(m.getGenres(), o.getGenres());
                if (gj < minGenreJaccardQuick) {
                    continue;
                }
                quick.add(new int[]{oid, (int) (gj * 1_000_000)});
            }
            quick.sort(Comparator.comparingInt(a -> -a[1]));
            int refine = Math.min(refineTopByGenre, quick.size());

            record Scored(int other, double sim) {
            }
            List<Scored> scored = new ArrayList<>();
            for (int i = 0; i < refine; i++) {
                int oid = quick.get(i)[0];
                Movie o = byId.get(oid);
                double sim = SimilarityMetrics.combinedGenresAndRatings(m, o, ratingsByMovie, minCommonRaters);
                if (sim >= minCombinedSimilarity) {
                    scored.add(new Scored(oid, sim));
                }
            }
            scored.sort(Comparator.comparingDouble(Scored::sim).reversed());
            int added = 0;
            for (Scored s : scored) {
                if (added >= topEdgesPerMovie) {
                    break;
                }
                double w = SimilarityMetrics.similarityToEdgeWeight(s.sim());
                g.addUndirectedEdgeMergeMin(mid, s.other(), w);
                added++;
            }
        }
        status.accept(String.format(Locale.ROOT, "Sparse graph ready (%d movie nodes).", total));
        return g;
    }

    private static Map<String, List<Integer>> buildGenreIndex(List<Movie> movies) {
        Map<String, List<Integer>> ix = new HashMap<>();
        for (Movie m : movies) {
            for (String genre : m.getGenres()) {
                ix.computeIfAbsent(genre, k -> new ArrayList<>()).add(m.getId());
            }
        }
        return ix;
    }
}
