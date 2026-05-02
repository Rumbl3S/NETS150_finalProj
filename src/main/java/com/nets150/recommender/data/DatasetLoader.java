package com.nets150.recommender.data;

import com.nets150.recommender.model.Movie;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads MovieLens-style CSV files from a local directory.
 */
public final class DatasetLoader {
    private DatasetLoader() {
    }

    public static List<Movie> loadMovies(Path moviesCsv, Path moviePeopleCsv) throws IOException {
        Map<Integer, MovieRow> rows = new HashMap<>();
        try (BufferedReader br = Files.newBufferedReader(moviesCsv, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) {
                throw new IOException("empty movies file");
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = splitCsvLine(line);
                if (p.length < 3) {
                    continue;
                }
                int id = Integer.parseInt(p[0].trim());
                String title = unquote(p[1].trim());
                Set<String> genres = Arrays.stream(p[2].split("\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !"(no genres listed)".equalsIgnoreCase(s))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                rows.put(id, new MovieRow(id, title, genres));
            }
        }
        if (Files.exists(moviePeopleCsv)) {
            try (BufferedReader br = Files.newBufferedReader(moviePeopleCsv, StandardCharsets.UTF_8)) {
                br.readLine();
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] p = splitCsvLine(line);
                    if (p.length < 3) {
                        continue;
                    }
                    int id = Integer.parseInt(p[0].trim());
                    MovieRow row = rows.get(id);
                    if (row == null) {
                        continue;
                    }
                    String director = p[1].trim();
                    Set<String> actors = Arrays.stream(p[2].split("\\|"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .map(s -> s.toLowerCase(Locale.ROOT))
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    row.director = director;
                    row.actors = actors;
                }
            }
        }
        List<Movie> out = new ArrayList<>();
        for (MovieRow r : rows.values()) {
            out.add(new Movie(r.id, r.title, r.genres, r.actors, r.director));
        }
        out.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        return out;
    }

    public static List<RatingRecord> loadRatings(Path ratingsCsv) throws IOException {
        List<RatingRecord> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(ratingsCsv, StandardCharsets.UTF_8)) {
            br.readLine();
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = splitCsvLine(line);
                if (p.length < 3) {
                    continue;
                }
                int userId = Integer.parseInt(p[0].trim());
                int movieId = Integer.parseInt(p[1].trim());
                double rating = Double.parseDouble(p[2].trim());
                list.add(new RatingRecord(userId, movieId, rating));
            }
        }
        return list;
    }

    public static Map<Integer, Map<Integer, Double>> ratingsByMovie(List<RatingRecord> ratings) {
        Map<Integer, Map<Integer, Double>> map = new HashMap<>();
        for (RatingRecord r : ratings) {
            map.computeIfAbsent(r.movieId(), k -> new HashMap<>()).put(r.userId(), r.rating());
        }
        return map;
    }

    private static String[] splitCsvLine(String line) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                parts.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        parts.add(cur.toString());
        return parts.toArray(String[]::new);
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).replace("\"\"", "\"");
        }
        return s;
    }

    public record RatingRecord(int userId, int movieId, double rating) {
    }

    private static final class MovieRow {
        final int id;
        final String title;
        final Set<String> genres;
        String director = "";
        Set<String> actors = Set.of();

        MovieRow(int id, String title, Set<String> genres) {
            this.id = id;
            this.title = title;
            this.genres = genres;
        }
    }
}
