Project name: Graph-Based Movie Recommender (Java)

This is a NETS 1500 implementation project that recommends movies by modeling films as nodes in a weighted undirected graph. For the **MovieLens 20M** release (Kaggle: `grouplens/movielens-20m-dataset`), edges form a **sparse** similarity graph: each movie links to its strongest neighbors using genre overlap plus rating correlation on a **streamed reservoir sample** of ratings (the full ~20M ratings file is never held in RAM). For the small toy CSVs, an optional `movie_people.csv` adds cast/director overlap. Edge weight is one minus similarity so shorter weighted paths mean closer movies. The Swing UI uses **FlatLaf** for a cleaner look. Users pick seeds and get **multi-source Dijkstra** rankings plus **BFS/DFS** demos. Data and `graph_cache.ser` live under the resolved dataset folder (see `DATASET_SOURCE.txt`).

Category from the homework list: Graph and graph algorithms (also touches recommendations as an application domain).

Work breakdown: Sahiti — graph data structure design, edge weighting logic (genre Jaccard + rating correlation), dataset integration and CSV loading, graph cache serialization. Raghav — Java Swing UI, multi-select seed list, output panel, integration with the recommender service, testing and demo prep. Shared — recommendation algorithm implementation (multi-source Dijkstra, BFS, DFS traversal, ranking logic).

AI usage (concrete): Claude Code (Anthropic) was used to scaffold the Maven project layout, generate boilerplate Java class stubs (graph, similarity metrics, traversals, Dijkstra recommender, CSV loader, cache, Swing UI), and help draft the documentation files. All graph design decisions, algorithm choices, similarity weighting logic, and debugging were reviewed and directed by the team.

Changes from proposal: The demo runs on the bundled toy CSV dataset (data/movies.csv, ratings.csv) rather than the full MovieLens 20M download, as the core graph algorithms are the same regardless of scale and the toy data allows faster startup for grading. The app fully supports MovieLens 20M via the download script in scripts/ (see USER_MANUAL.txt). The third team member listed in the proposal (Anoushka) did not end up participating; the project was completed by Sahiti and Raghav.

How to run (requires Java 17+): from the project root, `mvn compile exec:java`. See USER_MANUAL.txt for full setup instructions.

Code lives on the `main` branch of the repository.
