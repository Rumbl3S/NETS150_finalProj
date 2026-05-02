package com.nets150.recommender.graph;

import com.nets150.recommender.model.Movie;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds an undirected weighted graph from movies and user ratings.
 * An edge exists when combined similarity exceeds {@code minSimilarity}.
 */
public final class MovieGraphBuilder {
    private final double minSimilarity;
    private final int minCommonUsersForCorrelation;

    public MovieGraphBuilder(double minSimilarity, int minCommonUsersForCorrelation) {
        this.minSimilarity = minSimilarity;
        this.minCommonUsersForCorrelation = minCommonUsersForCorrelation;
    }

    public WeightedGraph build(List<Movie> movies, Map<Integer, Map<Integer, Double>> ratingsByMovie) {
        WeightedGraph g = new WeightedGraph();
        List<Movie> list = new ArrayList<>(movies);
        for (Movie m : list) {
            g.addNode(m.getId());
        }
        for (int i = 0; i < list.size(); i++) {
            for (int j = i + 1; j < list.size(); j++) {
                Movie a = list.get(i);
                Movie b = list.get(j);
                double sim = SimilarityMetrics.combinedSimilarity(
                        a, b, ratingsByMovie, minCommonUsersForCorrelation
                );
                if (sim >= minSimilarity) {
                    double w = SimilarityMetrics.similarityToEdgeWeight(sim);
                    g.addUndirectedEdge(a.getId(), b.getId(), w);
                }
            }
        }
        return g;
    }
}
