package com.nets150.recommender.ui;

import com.nets150.recommender.model.Movie;
import com.nets150.recommender.graph.WeightedGraph;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;

public class GraphVisualizationPanel extends JPanel {
    private static final Color NODE_COLOR = new Color(13, 110, 253);
    private static final Color SELECTED_NODE_COLOR = new Color(255, 87, 34);
    private static final Color EDGE_COLOR = new Color(200, 200, 200);
    private static final Color TEXT_COLOR = new Color(33, 37, 41);
    private static final Color HIGHLIGHT_COLOR = new Color(25, 135, 84);
    private static final Color BG_COLOR = new Color(248, 249, 250);

    private static final int NODE_SIZE = 20;
    private static final int MAX_NODES = 50;

    private Map<Integer, NodeInfo> nodes = new HashMap<>();
    private List<EdgeInfo> edges = new ArrayList<>();
    private Set<Integer> highlightedNodes = new HashSet<>();
    private Integer hoveredNode = null;

    private double zoom = 1.0;
    private Point panOffset = new Point(0, 0);
    private Point lastMousePos = null;

    private WeightedGraph graph;
    private Map<Integer, Movie> movieMap;

    private static class NodeInfo {
        int movieId;
        String title;
        double x, y;
        double vx, vy;
        Color color;

        NodeInfo(int id, String title, double x, double y) {
            this.movieId = id;
            this.title = title;
            this.x = x;
            this.y = y;
            this.vx = 0;
            this.vy = 0;
            this.color = NODE_COLOR;
        }
    }

    private static class EdgeInfo {
        int from, to;
        double weight;

        EdgeInfo(int from, int to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }
    }

    public GraphVisualizationPanel() {
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createLineBorder(new Color(222, 226, 230)));
        setPreferredSize(new Dimension(800, 600));

        setupInteraction();
    }

    private void setupInteraction() {
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMousePos = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMousePos != null) {
                    int dx = e.getX() - lastMousePos.x;
                    int dy = e.getY() - lastMousePos.y;
                    panOffset.x += dx;
                    panOffset.y += dy;
                    lastMousePos = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                Point screenPoint = e.getPoint();
                hoveredNode = null;

                for (NodeInfo node : nodes.values()) {
                    Point nodeScreen = worldToScreen(node.x, node.y);
                    double dist = nodeScreen.distance(screenPoint);
                    if (dist < NODE_SIZE) {
                        hoveredNode = node.movieId;
                        setToolTipText("<html><b>" + node.title + "</b><br>ID: " + node.movieId + "</html>");
                        break;
                    }
                }

                if (hoveredNode == null) {
                    setToolTipText(null);
                }
                repaint();
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                // Get precise scroll amount
                double preciseRotation = e.getPreciseWheelRotation();

                // Very small zoom factor for smooth scrolling
                double zoomFactor = 1.0 + (preciseRotation * 0.02);

                // Apply zoom with dampening
                double newZoom = zoom * zoomFactor;

                // Clamp zoom values
                zoom = Math.max(0.3, Math.min(3.0, newZoom));

                repaint();
            }
        };

        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(mouseHandler);
    }

    public void updateGraph(WeightedGraph graph, Map<Integer, Movie> movieMap, Set<Integer> seedMovies) {
        this.graph = graph;
        this.movieMap = movieMap;
        this.highlightedNodes = new HashSet<>(seedMovies);

        buildVisualizationData(seedMovies);
        layoutNodes();
        repaint();
    }

    private void buildVisualizationData(Set<Integer> seedMovies) {
        nodes.clear();
        edges.clear();

        if (graph == null || movieMap == null || seedMovies.isEmpty()) {
            return;
        }

        Set<Integer> nodesToShow = new HashSet<>(seedMovies);

        for (int seed : seedMovies) {
            List<WeightedGraph.Edge> neighbors = graph.neighbors(seed);
            if (neighbors != null && !neighbors.isEmpty()) {
                List<Integer> topNeighbors = neighbors.stream()
                    .sorted((a, b) -> Double.compare(a.weight(), b.weight()))
                    .limit(8)
                    .map(edge -> edge.to())
                    .toList();
                nodesToShow.addAll(topNeighbors);
            }
        }

        if (nodesToShow.size() > MAX_NODES) {
            List<Integer> nodeList = new ArrayList<>(nodesToShow);
            nodesToShow = new HashSet<>(nodeList.subList(0, MAX_NODES));
        }

        final Set<Integer> finalNodesToShow = nodesToShow;
        Random rand = new Random(42);
        for (int movieId : finalNodesToShow) {
            Movie movie = movieMap.get(movieId);
            if (movie != null) {
                double x = (rand.nextDouble() - 0.5) * 400;
                double y = (rand.nextDouble() - 0.5) * 400;
                NodeInfo node = new NodeInfo(movieId, movie.getTitle(), x, y);
                if (highlightedNodes.contains(movieId)) {
                    node.color = HIGHLIGHT_COLOR;
                }
                nodes.put(movieId, node);
            }
        }

        for (int from : nodes.keySet()) {
            List<WeightedGraph.Edge> neighbors = graph.neighbors(from);
            if (neighbors != null) {
                for (WeightedGraph.Edge edge : neighbors) {
                    int to = edge.to();
                    if (nodes.containsKey(to) && from < to) {
                        edges.add(new EdgeInfo(from, to, edge.weight()));
                    }
                }
            }
        }
    }

    private void layoutNodes() {
        if (nodes.isEmpty()) return;

        int iterations = 150;
        double k = 100.0;
        double c = 0.01;

        for (int iter = 0; iter < iterations; iter++) {
            for (NodeInfo node : nodes.values()) {
                node.vx = 0;
                node.vy = 0;

                for (NodeInfo other : nodes.values()) {
                    if (node != other) {
                        double dx = node.x - other.x;
                        double dy = node.y - other.y;
                        double dist = Math.sqrt(dx * dx + dy * dy);
                        if (dist > 0 && dist < 300) {
                            double force = k * k / dist;
                            node.vx += (dx / dist) * force * c;
                            node.vy += (dy / dist) * force * c;
                        }
                    }
                }
            }

            for (EdgeInfo edge : edges) {
                NodeInfo n1 = nodes.get(edge.from);
                NodeInfo n2 = nodes.get(edge.to);
                if (n1 != null && n2 != null) {
                    double dx = n2.x - n1.x;
                    double dy = n2.y - n1.y;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    if (dist > 0) {
                        double force = (dist - k) / k * 0.5;
                        double fx = (dx / dist) * force * c;
                        double fy = (dy / dist) * force * c;
                        n1.vx += fx;
                        n1.vy += fy;
                        n2.vx -= fx;
                        n2.vy -= fy;
                    }
                }
            }

            for (NodeInfo node : nodes.values()) {
                node.x += node.vx;
                node.y += node.vy;
                node.x = Math.max(-300, Math.min(300, node.x));
                node.y = Math.max(-250, Math.min(250, node.y));
            }
        }
    }

    private Point worldToScreen(double x, double y) {
        int screenX = (int)(getWidth() / 2 + (x * zoom) + panOffset.x);
        int screenY = (int)(getHeight() / 2 + (y * zoom) + panOffset.y);
        return new Point(screenX, screenY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setStroke(new BasicStroke(1.5f));
        for (EdgeInfo edge : edges) {
            NodeInfo n1 = nodes.get(edge.from);
            NodeInfo n2 = nodes.get(edge.to);
            if (n1 != null && n2 != null) {
                Point p1 = worldToScreen(n1.x, n1.y);
                Point p2 = worldToScreen(n2.x, n2.y);

                float alpha = (float) Math.max(0.2, Math.min(1.0, 2.0 - edge.weight));
                g2.setColor(new Color(EDGE_COLOR.getRed(), EDGE_COLOR.getGreen(),
                                     EDGE_COLOR.getBlue(), (int)(alpha * 150)));
                g2.draw(new Line2D.Double(p1.x, p1.y, p2.x, p2.y));
            }
        }

        for (NodeInfo node : nodes.values()) {
            Point p = worldToScreen(node.x, node.y);

            Color nodeColor = node.color;
            if (hoveredNode != null && hoveredNode == node.movieId) {
                nodeColor = SELECTED_NODE_COLOR;
            }

            int size = (int)(NODE_SIZE * Math.min(2.0, zoom));

            g2.setColor(Color.WHITE);
            g2.fill(new Ellipse2D.Double(p.x - size/2 - 2, p.y - size/2 - 2, size + 4, size + 4));

            g2.setColor(nodeColor);
            g2.fill(new Ellipse2D.Double(p.x - size/2, p.y - size/2, size, size));

            g2.setColor(nodeColor.darker());
            g2.setStroke(new BasicStroke(2.0f));
            g2.draw(new Ellipse2D.Double(p.x - size/2, p.y - size/2, size, size));

            if (highlightedNodes.contains(node.movieId) ||
                (hoveredNode != null && hoveredNode == node.movieId)) {
                g2.setColor(TEXT_COLOR);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String label = node.title;
                if (label.length() > 25) {
                    label = label.substring(0, 22) + "...";
                }
                g2.drawString(label, p.x + size/2 + 5, p.y + 4);
            }
        }

        g2.setColor(TEXT_COLOR);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        g2.drawString("Drag to pan | Scroll to zoom | Green = Your picks", 10, getHeight() - 10);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.drawString("Movie Similarity Graph", 10, 20);
    }

    public static void showGraphVisualization(JFrame parent, WeightedGraph graph,
                                             Map<Integer, Movie> movieMap, Set<Integer> seedMovies) {
        JDialog dialog = new JDialog(parent, "Graph Visualization", false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        GraphVisualizationPanel panel = new GraphVisualizationPanel();
        panel.updateGraph(graph, movieMap, seedMovies);

        dialog.add(panel);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}