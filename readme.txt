Project name: Graph-Based Movie Recommender (Java)

This is a NETS 1500 implementation project that recommends movies by modeling films as nodes in a weighted undirected graph. Edges connect pairs of movies whose combined content and rating similarity exceeds a threshold; edge weight is one minus that similarity so that “closer” movies have shorter weighted paths. Users select one or more movies they like in a Swing desktop UI; the system ranks recommendations using multi-source Dijkstra shortest-path distances (shortest-path-style ranking). It also exposes breadth-first and depth-first traversal visit orders from a chosen movie to satisfy explicit graph traversal requirements. All tabular data and a serialized graph cache live on disk under `data/`.

Category from the homework list: Graph and graph algorithms (also touches recommendations as an application domain).

Work breakdown (simulated team roles as in the proposal): (1) Graph design and data — weighted graph schema, similarity weights for genres, actors, director, and rating correlation, local CSV loading and cache serialization. (2) Algorithms — BFS and DFS over the adjacency structure, multi-source Dijkstra for ranked recommendations. (3) Frontend — Swing UI for search, multi-select seeds, traversal demos, and integration with the service layer.

AI usage (concrete): An AI coding assistant was used to scaffold the Maven layout, implement the Java classes (graph, similarity metrics, traversals, Dijkstra recommender, CSV loader, cache, Swing UI), author the local sample dataset files in MovieLens-compatible format, and write readme.txt, USER_MANUAL.txt, REPORT.txt, and DATASET_SOURCE.txt to match course deliverables. Human review should verify correctness, citations, and that any course-specific TA instructions still apply.

If the proposal changes after TA feedback: replace CSVs under `data/` for a larger MovieLens slice, tune `MovieGraphBuilder` thresholds in `RecommenderService.java`, and delete `data/cache/graph_cache.ser` before demoing.

How to run (requires Java 17+): from the project root, `./run.sh` compiles into `out/` and launches the UI; or `mvn compile exec:java` if Maven is installed; or compile all sources to `out/` and run `java -cp out com.nets150.recommender.Main`. Keep the working directory at the project root so `data/` resolves. Optional argument: path to data directory (default `data`).

Branch: implementation lives on `feature/movie-recommender` as requested for version control.
