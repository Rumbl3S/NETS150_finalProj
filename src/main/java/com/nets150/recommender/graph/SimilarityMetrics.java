package com.nets150.recommender.graph;

import com.nets150.recommender.model.Movie;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Combines genre overlap, shared cast, director match, and rating correlation into a similarity in [0, 1].
 */
public final class SimilarityMetrics {
    private static final double W_GENRE = 0.35;
    private static final double W_ACTOR = 0.30;
    private static final double W_DIRECTOR = 0.10;
    private static final double W_RATING = 0.25;

    private SimilarityMetrics() {
    }

    public static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0.0 : (double) inter.size() / union.size();
    }

    /**
     * Pearson correlation on common users; returns value in [-1, 1], then mapped to [0, 1] for blending.
     */
    public static double ratingSimilarity(
            Map<Integer, Map<Integer, Double>> ratingsByMovie,
            int movieA,
            int movieB,
            int minCommonUsers
    ) {
        Map<Integer, Double> ra = ratingsByMovie.get(movieA);
        Map<Integer, Double> rb = ratingsByMovie.get(movieB);
        if (ra == null || rb == null) {
            return 0.0;
        }
        Set<Integer> common = new HashSet<>(ra.keySet());
        common.retainAll(rb.keySet());
        if (common.size() < minCommonUsers) {
            return 0.0;
        }
        double sumA = 0;
        double sumB = 0;
        int n = common.size();
        for (int u : common) {
            sumA += ra.get(u);
            sumB += rb.get(u);
        }
        double meanA = sumA / n;
        double meanB = sumB / n;
        double num = 0;
        double denA = 0;
        double denB = 0;
        for (int u : common) {
            double da = ra.get(u) - meanA;
            double db = rb.get(u) - meanB;
            num += da * db;
            denA += da * da;
            denB += db * db;
        }
        if (denA == 0 || denB == 0) {
            return 0.5;
        }
        double r = num / Math.sqrt(denA * denB);
        r = Math.max(-1.0, Math.min(1.0, r));
        return (r + 1.0) / 2.0;
    }

    public static double combinedSimilarity(
            Movie a,
            Movie b,
            Map<Integer, Map<Integer, Double>> ratingsByMovie,
            int minCommonUsersForCorrelation
    ) {
        double g = jaccard(a.getGenres(), b.getGenres());
        double act = jaccard(a.getActors(), b.getActors());
        double dir = 0.0;
        if (!a.getDirector().isBlank() && a.getDirector().equalsIgnoreCase(b.getDirector())) {
            dir = 1.0;
        }
        double rat = ratingSimilarity(ratingsByMovie, a.getId(), b.getId(), minCommonUsersForCorrelation);
        return W_GENRE * g + W_ACTOR * act + W_DIRECTOR * dir + W_RATING * rat;
    }

    /**
     * MovieLens core files have no cast; use genre overlap plus rating correlation only.
     */
    public static double combinedGenresAndRatings(
            Movie a,
            Movie b,
            Map<Integer, Map<Integer, Double>> ratingsByMovie,
            int minCommonUsersForCorrelation
    ) {
        double g = jaccard(a.getGenres(), b.getGenres());
        double rat = ratingSimilarity(ratingsByMovie, a.getId(), b.getId(), minCommonUsersForCorrelation);
        return 0.40 * g + 0.60 * rat;
    }

    /**
     * Minimum edge weight so Dijkstra distances stay positive and comparable (avoids many exact 0.0 ties
     * when similarity is ~1 on an edge from a seed).
     */
    public static final double MIN_EDGE_WEIGHT = 2e-4;

    /**
     * Non-negative edge weight for Dijkstra: dissimilar movies have larger weight.
     */
    public static double similarityToEdgeWeight(double similarity) {
        double s = Math.max(0.0, Math.min(1.0, similarity));
        return Math.max(MIN_EDGE_WEIGHT, 1.0 - s);
    }
}
