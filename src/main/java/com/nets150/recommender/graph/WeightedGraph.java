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

    public record Edge(int to, double weight) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
