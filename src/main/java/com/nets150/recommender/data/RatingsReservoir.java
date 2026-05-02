package com.nets150.recommender.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One streaming pass over ratings.csv: keeps up to {@code maxRatingsPerMovie} (user,rating) pairs per movie
 * using reservoir sampling over the stream of ratings for that movie.
 */
public final class RatingsReservoir {
    private RatingsReservoir() {
    }

    public static Map<Integer, Map<Integer, Double>> buildPerMovieSample(Path ratingsCsv, int maxRatingsPerMovie) throws IOException {
        Map<Integer, List<int[]>> reservoirLists = new HashMap<>();
        Map<Integer, Integer> seenCount = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(ratingsCsv, StandardCharsets.UTF_8)) {
            String header = br.readLine();
            if (header == null) {
                throw new IOException("empty ratings file");
            }
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                int c1 = line.indexOf(',');
                int c2 = line.indexOf(',', c1 + 1);
                int c3 = line.indexOf(',', c2 + 1);
                if (c1 < 0 || c2 < 0) {
                    continue;
                }
                int userId = Integer.parseInt(line.substring(0, c1).trim());
                int movieId = Integer.parseInt(line.substring(c1 + 1, c2).trim());
                String ratingPart = c3 < 0 ? line.substring(c2 + 1).trim() : line.substring(c2 + 1, c3).trim();
                double rating = Double.parseDouble(ratingPart);
                int scaled = (int) Math.round(rating * 1000.0);

                int n = seenCount.merge(movieId, 1, Integer::sum);
                List<int[]> buf = reservoirLists.computeIfAbsent(
                        movieId,
                        k -> new ArrayList<>(maxRatingsPerMovie)
                );
                if (n <= maxRatingsPerMovie) {
                    buf.add(new int[]{userId, scaled});
                } else {
                    int j = ThreadLocalRandom.current().nextInt(n);
                    if (j < maxRatingsPerMovie) {
                        buf.set(j, new int[]{userId, scaled});
                    }
                }
            }
        }

        Map<Integer, Map<Integer, Double>> out = new HashMap<>();
        for (Map.Entry<Integer, List<int[]>> e : reservoirLists.entrySet()) {
            Map<Integer, Double> m = new HashMap<>(e.getValue().size());
            for (int[] ur : e.getValue()) {
                m.put(ur[0], ur[1] / 1000.0);
            }
            out.put(e.getKey(), m);
        }
        return out;
    }
}
