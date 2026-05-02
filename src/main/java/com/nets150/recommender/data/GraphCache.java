package com.nets150.recommender.data;

import com.nets150.recommender.graph.WeightedGraph;
import com.nets150.recommender.model.Movie;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists the built graph and movie list under {@code data/cache/} for faster restarts.
 */
public final class GraphCache {
    private GraphCache() {
    }

    public static void save(Path cacheFile, WeightedGraph graph, List<Movie> movies) throws IOException {
        Files.createDirectories(cacheFile.getParent());
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(cacheFile))) {
            oos.writeObject(new CachedState(graph, new ArrayList<>(movies)));
        }
    }

    public static CachedState load(Path cacheFile) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(cacheFile))) {
            Object o = ois.readObject();
            if (o instanceof CachedState s) {
                return s;
            }
            throw new IOException("unexpected cache format");
        }
    }

    public record CachedState(WeightedGraph graph, ArrayList<Movie> movies) implements Serializable {
        private static final long serialVersionUID = 1L;
    }
}
