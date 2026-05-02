Project name: Graph-Based Movie Recommender (Java)

This is a NETS 1500 implementation project that recommends movies by modeling films as nodes in a weighted undirected graph. For the **MovieLens 20M** release (Kaggle: `grouplens/movielens-20m-dataset`), edges form a **sparse** similarity graph: each movie links to its strongest neighbors using genre overlap plus rating correlation on a **streamed reservoir sample** of ratings (the full ~20M ratings file is never held in RAM). For the small toy CSVs, an optional `movie_people.csv` adds cast/director overlap. Edge weight is one minus similarity so shorter weighted paths mean closer movies. The Swing UI uses **FlatLaf** for a cleaner look. Users pick seeds and get **multi-source Dijkstra** rankings plus **BFS/DFS** demos. Data and `graph_cache.ser` live under the resolved dataset folder (see `DATASET_SOURCE.txt`).

Category from the homework list: Graph and graph algorithms (also touches recommendations as an application domain).

Work breakdown (simulated team roles as in the proposal): (1) Graph design and data — weighted graph schema, similarity weights for genres, actors, director, and rating correlation, local CSV loading and cache serialization. (2) Algorithms — BFS and DFS over the adjacency structure, multi-source Dijkstra for ranked recommendations. (3) Frontend — Swing UI for search, multi-select seeds, traversal demos, and integration with the service layer.

AI usage (concrete): An AI coding assistant was used to scaffold the Maven layout, implement the Java classes (graph, similarity metrics, traversals, Dijkstra recommender, CSV loader, cache, Swing UI), author the local sample dataset files in MovieLens-compatible format, and write readme.txt, USER_MANUAL.txt, REPORT.txt, and DATASET_SOURCE.txt to match course deliverables. Human review should verify correctness, citations, and that any course-specific TA instructions still apply.

If the proposal changes after TA feedback: tune `SparseMovieGraphBuilder.movieLens20MDefault()` or `MovieGraphBuilder` (toy mode), and delete `<dataset>/cache/graph_cache.ser` before demoing.

Full MovieLens 20M: `python3 -m pip install -r scripts/requirements-movielens.txt` then `python3 scripts/download_movielens.py` (writes `data/movielens-20m/`). By default only the **first 1M rating rows** are copied to `ratings.csv` (set `MOVIELENS_RATINGS_MAX_LINES=0` for all rows; `MOVIELENS_RATINGS_ORDER=tail` for the last N rows). Java uses the same 1M line cap unless `-Dmovielens.ratingLines=0`. First Java launch builds the sparse graph then caches it.

How to run (requires Java 17+): from the project root, `./run.sh` downloads FlatLaf if needed, compiles into `out/`, and launches the UI (`java -cp out:lib/flatlaf-3.4.1.jar …`). With Maven: `mvn compile exec:java`. Optional first argument: config directory (default `data`) where the app looks for `movielens-20m/` or toy `movies.csv`.

Branch: implementation lives on `feature/movie-recommender` as requested for version control.
