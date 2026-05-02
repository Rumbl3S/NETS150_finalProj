package com.nets150.recommender.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Undirected weighted graph: movies are integer node ids matching {@link com.nets150.recommender.model.Movie#getId}.
 * Edge weights are non-negative (suitable for Dijkstra). Lower weight means stronger similarity along that edge.
 */
public final class WeightedGraph implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<Integer, List<Edge>> adjacency = new HashMap<>();

    public void addNode(int id) {
        adjacency.computeIfAbsent(id, k -> new ArrayList<>());
    }

    public void addUndirectedEdge(int a, int b, double weight) {
        if (a == b) {
            return;
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weights must be non-negative");
        }
        adjacency.computeIfAbsent(a, k -> new ArrayList<>()).add(new Edge(b, weight));
        adjacency.computeIfAbsent(b, k -> new ArrayList<>()).add(new Edge(a, weight));
    }

    /**
     * Adds or tightens an undirected edge: if an edge already exists between the same endpoints, keeps the
     * smaller weight (stronger similarity / cheaper path for Dijkstra).
     */
    public void addUndirectedEdgeMergeMin(int a, int b, double weight) {
        if (a == b) {
            return;
        }
        if (weight < 0) {
            throw new IllegalArgumentException("weights must be non-negative");
        }
        mergeHalf(a, b, weight);
        mergeHalf(b, a, weight);
    }

    private void mergeHalf(int from, int to, double weight) {
        List<Edge> list = adjacency.computeIfAbsent(from, k -> new ArrayList<>());
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).to() == to) {
                if (weight < list.get(i).weight()) {
                    list.set(i, new Edge(to, weight));
                }
                return;
            }
        }
        list.add(new Edge(to, weight));
    }

    public List<Edge> neighbors(int node) {
        return adjacency.getOrDefault(node, List.of());
    }

    public Set<Integer> nodes() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    public boolean containsNode(int id) {
        return adjacency.containsKey(id);
    }

    /**
     * For each directed half-edge list: merge parallel edges to the same target keeping the minimum weight,
     * then clamp every weight to at least {@code minWeight}. Use after deserializing older caches that may
     * contain zero-weight edges or duplicate adjacency entries.
     */
    public void mergeParallelEdgesAndClamp(double minWeight) {
        if (minWeight < 0) {
            throw new IllegalArgumentException("minWeight must be non-negative");
        }
        Map<Integer, List<Edge>> rebuilt = new HashMap<>();
        for (Map.Entry<Integer, List<Edge>> e : new HashMap<>(adjacency).entrySet()) {
            int from = e.getKey();
            Map<Integer, Double> best = new HashMap<>();
            for (Edge ed : e.getValue()) {
                double w = Math.max(minWeight, ed.weight());
                best.merge(ed.to(), w, Math::min);
            }
            List<Edge> out = new ArrayList<>(best.size());
            for (Map.Entry<Integer, Double> te : best.entrySet()) {
                out.add(new Edge(te.getKey(), te.getValue()));
            }
            rebuilt.put(from, out);
        }
        adjacency.clear();
        adjacency.putAll(rebuilt);
    }

    public record Edge(int to, double weight) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
