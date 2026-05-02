package com.nets150.recommender.data;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Resolves where MovieLens CSVs live: bundled toy data, env override, or data/movielens-20m/.
 */
public final class DatasetPaths {
    private final Path root;
    private final Path moviesCsv;
    private final Path ratingsCsv;
    private final Path moviePeopleCsv;
    private final boolean movieLens20M;

    private DatasetPaths(Path root, Path moviesCsv, Path ratingsCsv, Path moviePeopleCsv, boolean movieLens20M) {
        this.root = root;
        this.moviesCsv = moviesCsv;
        this.ratingsCsv = ratingsCsv;
        this.moviePeopleCsv = moviePeopleCsv;
        this.movieLens20M = movieLens20M;
    }

    public static DatasetPaths resolve(Path dataDir) {
        Path env = envPath();
        if (env != null) {
            return fromRoot(env, true);
        }
        Path ml = dataDir.resolve("movielens-20m");
        if (Files.isRegularFile(ml.resolve("movies.csv")) && Files.isRegularFile(ml.resolve("ratings.csv"))) {
            return fromRoot(ml.normalize(), true);
        }
        Path toyMovies = dataDir.resolve("movies.csv");
        Path toyRatings = dataDir.resolve("ratings.csv");
        if (Files.isRegularFile(toyMovies) && Files.isRegularFile(toyRatings)) {
            Path people = dataDir.resolve("movie_people.csv");
            return new DatasetPaths(
                    dataDir.normalize(),
                    toyMovies,
                    toyRatings,
                    people,
                    false
            );
        }
        throw new IllegalArgumentException(
                "No dataset found. Either:\n"
                        + "  (1) Run: python3 scripts/download_movielens.py  (creates data/movielens-20m/), or\n"
                        + "  (2) Put movies.csv + ratings.csv under " + dataDir.toAbsolutePath() + ", or\n"
                        + "  (3) Set MOVIELENS_20M_DIR to a folder containing movies.csv and ratings.csv."
        );
    }

    private static Path envPath() {
        String p = System.getenv("MOVIELENS_20M_DIR");
        if (p == null || p.isBlank()) {
            return null;
        }
        Path root = Path.of(p).toAbsolutePath().normalize();
        if (Files.isRegularFile(root.resolve("movies.csv")) && Files.isRegularFile(root.resolve("ratings.csv"))) {
            return root;
        }
        return null;
    }

    private static DatasetPaths fromRoot(Path root, boolean ml20m) {
        Path people = root.resolve("movie_people.csv");
        return new DatasetPaths(root, root.resolve("movies.csv"), root.resolve("ratings.csv"), people, ml20m);
    }

    public Path root() {
        return root;
    }

    public Path moviesCsv() {
        return moviesCsv;
    }

    public Path ratingsCsv() {
        return ratingsCsv;
    }

    public Path moviePeopleCsv() {
        return moviePeopleCsv;
    }

    public boolean movieLens20M() {
        return movieLens20M;
    }

    /** Heuristic: use sparse graph + streaming ratings when this is the large Kaggle release. */
    public boolean useLargeScalePipeline() {
        return movieLens20M;
    }
}
