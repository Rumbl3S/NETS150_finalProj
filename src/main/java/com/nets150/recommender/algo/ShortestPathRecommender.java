package com.nets150.recommender.algo;

import com.nets150.recommender.graph.WeightedGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Multi-source Dijkstra shortest-path distances from liked movies; lower distance means closer in the similarity graph.
 */
public final class ShortestPathRecommender {
    private ShortestPathRecommender() {
    }

    public record ScoredMovie(int movieId, double distance) {
    }

    /**
     * Runs Dijkstra from all seed nodes in parallel (multi-source). Seeds are excluded from results.
     */
    public static List<ScoredMovie> recommend(
            WeightedGraph graph,
            Set<Integer> seedMovieIds,
            int topK
    ) {
        Map<Integer, Double> dist = new HashMap<>();
        PriorityQueue<NodeDist> pq = new PriorityQueue<>(Comparator.comparingDouble(NodeDist::dist));
        for (int s : seedMovieIds) {
            if (!graph.containsNode(s)) {
                continue;
            }
            if (!dist.containsKey(s) || dist.get(s) > 0) {
                dist.put(s, 0.0);
                pq.add(new NodeDist(s, 0.0));
            }
        }
        while (!pq.isEmpty()) {
            NodeDist cur = pq.poll();
            double d = cur.dist();
            if (d > dist.getOrDefault(cur.node(), Double.POSITIVE_INFINITY)) {
                continue;
            }
            for (WeightedGraph.Edge e : graph.neighbors(cur.node())) {
                double nd = d + e.weight();
                if (nd < dist.getOrDefault(e.to(), Double.POSITIVE_INFINITY)) {
                    dist.put(e.to(), nd);
                    pq.add(new NodeDist(e.to(), nd));
                }
            }
        }
        List<ScoredMovie> scored = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : dist.entrySet()) {
            if (seedMovieIds.contains(e.getKey())) {
                continue;
            }
            scored.add(new ScoredMovie(e.getKey(), e.getValue()));
        }
        scored.sort(Comparator.comparingDouble(ScoredMovie::distance));
        if (scored.size() > topK) {
            return scored.subList(0, topK);
        }
        return scored;
    }

    private record NodeDist(int node, double dist) {
    }
}
