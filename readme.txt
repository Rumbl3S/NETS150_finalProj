NETS 1500 - HW5 Final Project
Graph-Based Movie Recommender (Java)

Team:
  Sahiti Dasari   sahitid@wharton.upenn.edu
  Raghav Garimella   raghav07@seas.upenn.edu

Description
-----------
This is a Java application that recommends movies using a weighted similarity graph.
Each node is a movie, and edges connect movies that are similar based on genre
overlap (Jaccard) and Pearson correlation on shared user ratings. Edge weight is
1 minus similarity, so more similar movies are closer in the graph. The user picks
one or more movies they like through a Swing UI, and the app runs multi-source
Dijkstra from those seeds to rank the remaining movies by shortest-path distance.
The UI also displays BFS and DFS traversal orders from a chosen start movie. The
project uses the MovieLens dataset, with a small bundled CSV sample for fast
grading and a download script for the full MovieLens 20M release.

Category from the homework list
-------------------------------
Graph and graph algorithms (BFS, DFS, and multi-source Dijkstra on a weighted
similarity graph). The project also relates to the "recommendations" advanced
topic from the class list.

Work breakdown
--------------
Sahiti - graph data structure, similarity / edge-weighting logic (genre Jaccard
plus rating correlation), CSV loading, and graph cache serialization.
Raghav - Java Swing UI (search, multi-select "Your picks", output panel),
integration with the service layer, and demo / screenshot preparation.
Shared - BFS, DFS, and the multi-source Dijkstra recommendation algorithm,
plus debugging and testing.

The third teammate listed in our Step 1 proposal (Anoushka) did not end up
working on the project. The submission was completed by Sahiti and Raghav.

AI/LLM usage
------------
We used Claude Code (Anthropic) to scaffold the Maven project layout and
generate initial Java class skeletons for the graph, similarity metrics, BFS/DFS
traversals, Dijkstra recommender, CSV loader, graph cache, and Swing UI. We
also used it to help draft the documentation files. The team made all design
decisions (graph schema, similarity weighting, choice of algorithms, UI layout)
and reviewed and edited all generated code before integrating it.

Changes from proposal
---------------------
The demo runs on the small bundled CSV sample (data/movies.csv, data/ratings.csv,
data/movie_people.csv) rather than the full MovieLens 20M download, so the app
starts quickly for grading. The full MovieLens 20M dataset is fully supported
through scripts/download_movielens.py - see USER_MANUAL.txt and DATASET_SOURCE.txt
for setup instructions.

How to run
----------
Requires Java 17+ and Maven. From the project root:
  mvn compile exec:java
Or run the provided ./run.sh script. See USER_MANUAL.txt for the full setup,
controls, and screenshots (screenshot1-launch.png through screenshot4-dfs.png).
