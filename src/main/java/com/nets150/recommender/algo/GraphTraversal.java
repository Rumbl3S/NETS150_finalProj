package com.nets150.recommender.algo;

import com.nets150.recommender.graph.WeightedGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Breadth-first and depth-first traversals over the movie similarity graph (adjacency only; edge weights ignored).
 */
public final class GraphTraversal {
    private GraphTraversal() {
    }

    public static List<Integer> bfsOrder(WeightedGraph graph, int startMovieId, int maxNodes) {
        if (!graph.containsNode(startMovieId)) {
            return List.of();
        }
        List<Integer> order = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        Deque<Integer> q = new ArrayDeque<>();
        q.add(startMovieId);
        seen.add(startMovieId);
        while (!q.isEmpty() && order.size() < maxNodes) {
            int v = q.removeFirst();
            order.add(v);
            List<Integer> nbrs = neighborIdsSorted(graph, v);
            for (int w : nbrs) {
                if (seen.add(w)) {
                    q.addLast(w);
                }
            }
        }
        return order;
    }

    public static List<Integer> dfsOrder(WeightedGraph graph, int startMovieId, int maxNodes) {
        if (!graph.containsNode(startMovieId)) {
            return List.of();
        }
        List<Integer> order = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        dfsVisit(graph, startMovieId, seen, order, maxNodes);
        return order;
    }

    private static void dfsVisit(WeightedGraph graph, int v, Set<Integer> seen, List<Integer> order, int maxNodes) {
        if (order.size() >= maxNodes || !seen.add(v)) {
            return;
        }
        order.add(v);
        for (int w : neighborIdsSorted(graph, v)) {
            if (order.size() >= maxNodes) {
                break;
            }
            dfsVisit(graph, w, seen, order, maxNodes);
        }
    }

    private static List<Integer> neighborIdsSorted(WeightedGraph graph, int v) {
        List<Integer> ids = new ArrayList<>();
        for (WeightedGraph.Edge e : graph.neighbors(v)) {
            ids.add(e.to());
        }
        Collections.sort(ids);
        return ids;
    }
}
