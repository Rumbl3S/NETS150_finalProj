NETS 1500 - HW5 Final Project
Graph-Based Movie Recommender (Java)

Team:
  Sahiti Dasari       sahitid@wharton.upenn.edu
  Anoushka Nair       anoushkn@seas.upenn.edu
  Raghav Garimella    raghav07@seas.upenn.edu

Description
-----------
This is a Java application that recommends movies using a weighted similarity
graph. Each node is a movie, and edges connect movies that are similar based on
genre overlap (Jaccard) and Pearson correlation on shared user ratings. Edge
weight is 1 minus similarity, so more similar movies are closer in the graph.
The user picks one or more movies they like through a Swing UI and the app
runs multi-source Dijkstra from those seeds to rank the remaining movies by
shortest-path distance, while also exposing BFS, DFS, and an interactive graph
visualization. The project uses the full MovieLens 20M dataset from Kaggle /
GroupLens and reads ~20 million ratings via streaming with a per-movie reservoir
sample so the program runs comfortably in memory.

Categories from the homework list
---------------------------------
Graph and graph algorithms (BFS, DFS, and multi-source Dijkstra on a weighted
similarity graph). The project also relates to the "recommendations" advanced
topic from the class list.

Work breakdown
--------------
Sahiti - graph data structure, similarity / edge-weighting logic (genre Jaccard
and rating correlation), CSV loading, MovieLens 20M streaming and reservoir
sampling, and graph cache serialization.
Anoushka - recommendation algorithms: BFS, DFS, and multi-source Dijkstra
ranking on the weighted similarity graph, plus result formatting and tie-
breaking logic.
Raghav - card-based Swing UI (search, multi-select Your Picks, output panel,
graph visualization panel), integration with the service layer, and demo /
screenshot preparation.
All three contributed to debugging, testing, and final integration.

AI/LLM usage
------------
We used Claude Code (Anthropic) to scaffold the Maven project layout and
generate initial Java class skeletons for the graph, similarity metrics,
BFS/DFS traversals, the Dijkstra recommender, the CSV loader, the graph cache,
and the Swing UI. We also used it to help draft the documentation files. The
team made all design decisions (graph schema, similarity weighting, choice of
algorithms, MovieLens 20M streaming approach, UI layout) and reviewed and
edited all generated code before integrating it. The interactive graph
visualization panel and the card-based UI redesign were directed by us.

Changes from proposal
---------------------
The implementation matches the proposal: MovieLens (Kaggle / GroupLens) is the
dataset, similarity is built from genre overlap and rating correlation, and the
core algorithms are BFS, DFS, and shortest-path ranking. Two notable additions
beyond the proposal: an interactive graph visualization panel (Visualize Graph
button) that shows seeds in green and their neighborhood in blue with pan/zoom,
and a card-based UI redesign for clearer navigation.

How to run
----------
Requires Java 17+ and Maven. From the project root:
  mvn compile exec:java
Or run the provided ./run.sh script. See USER_MANUAL.txt for the full setup
(including how to download MovieLens 20M from Kaggle) and screenshots
(launch.png, recommendation.png, bfs.png, dfs.png, visualize.png).
