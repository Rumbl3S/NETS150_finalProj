package com.nets150.recommender.ui;

import com.nets150.recommender.algo.ShortestPathRecommender;
import com.nets150.recommender.model.Movie;
import com.nets150.recommender.service.RecommenderService;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interactive Swing UI: pick liked movies, run Dijkstra-based recommendations, inspect BFS/DFS traversal orders.
 */
public final class MovieRecommenderFrame extends JFrame {
    private final PathHolder dataDir;
    private RecommenderService service;
    private final DefaultListModel<Movie> listModel = new DefaultListModel<>();
    private final JList<Movie> movieList = new JList<>(listModel);
    private final JTextField filterField = new JTextField(24);
    private final JSpinner topKSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 50, 1));
    private final JTextArea output = new JTextArea(16, 60);

    private List<Movie> fullList = List.of();

    public MovieRecommenderFrame(RecommenderService service, PathHolder dataDir) {
        super("Graph Movie Recommender — NETS 1500");
        this.service = service;
        this.dataDir = dataDir;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        movieList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        movieList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                java.awt.Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Movie m) {
                    setText(m.getId() + " — " + m.getTitle());
                }
                return c;
            }
        });

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT));
        north.add(new JLabel("Filter:"));
        north.add(filterField);
        JButton applyFilter = new JButton("Apply filter");
        applyFilter.addActionListener(e -> applyFilter());
        north.add(applyFilter);
        JButton reload = new JButton("Reload data (rebuild graph)");
        reload.addActionListener(e -> reloadFromDisk());
        north.add(reload);

        JPanel centerTop = new JPanel(new BorderLayout());
        centerTop.setBorder(BorderFactory.createTitledBorder("Movies (Ctrl/Cmd-click to select several)"));
        centerTop.add(new JScrollPane(movieList), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(new JLabel("Top K:"));
        actions.add(topKSpinner);
        JButton recommend = new JButton("Recommend (shortest-path / Dijkstra)");
        recommend.addActionListener(e -> runRecommendations());
        actions.add(recommend);
        JButton bfs = new JButton("BFS order from first selection");
        bfs.addActionListener(e -> runBfs());
        actions.add(bfs);
        JButton dfs = new JButton("DFS order from first selection");
        dfs.addActionListener(e -> runDfs());
        actions.add(dfs);

        output.setEditable(false);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel south = new JPanel(new BorderLayout());
        south.setBorder(BorderFactory.createTitledBorder("Output"));
        south.add(new JScrollPane(output), BorderLayout.CENTER);
        south.add(actions, BorderLayout.NORTH);

        add(north, BorderLayout.NORTH);
        add(centerTop, BorderLayout.CENTER);
        add(south, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(920, 640));
        pack();
        setLocationRelativeTo(null);

        fullList = service.allMovies();
        applyFilter();
        appendWelcome();
    }

    private void appendWelcome() {
        output.setText("Select one or more movies you like, then click \"Recommend\".\n"
                + "The app ranks other movies by multi-source Dijkstra shortest-path distance on the similarity graph.\n"
                + "Use BFS/DFS to see traversal visit order from the first selected movie (graph algorithms requirement).\n"
                + "Data directory: " + service.dataDirectory().toAbsolutePath() + "\n\n");
    }

    private void applyFilter() {
        String q = filterField.getText().trim().toLowerCase(Locale.ROOT);
        listModel.clear();
        for (Movie m : fullList) {
            if (q.isEmpty() || m.getTitle().toLowerCase(Locale.ROOT).contains(q)
                    || String.valueOf(m.getId()).contains(q)) {
                listModel.addElement(m);
            }
        }
    }

    private void reloadFromDisk() {
        try {
            this.service = RecommenderService.load(dataDir.path(), false);
            this.fullList = service.allMovies();
            applyFilter();
            appendWelcome();
            output.append("Reloaded CSV files and rebuilt graph; cache overwritten.\n\n");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Reload failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Set<Integer> selectedIds() {
        return movieList.getSelectedValuesList().stream().map(Movie::getId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private void runRecommendations() {
        Set<Integer> seeds = selectedIds();
        if (seeds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one liked movie.", "Input", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int topK = ((Number) topKSpinner.getValue()).intValue();
        List<ShortestPathRecommender.ScoredMovie> recs = service.recommend(seeds, topK);
        StringBuilder sb = new StringBuilder();
        sb.append("Seeds: ").append(describe(seeds)).append("\n");
        sb.append("Top ").append(recs.size()).append(" by shortest-path distance (lower is closer):\n");
        int rank = 1;
        for (ShortestPathRecommender.ScoredMovie sm : recs) {
            Movie m = service.movieById(sm.movieId());
            String title = m != null ? m.getTitle() : ("id " + sm.movieId());
            sb.append(String.format(Locale.ROOT, "  %2d. %s — distance %.4f%n", rank++, title, sm.distance()));
        }
        if (recs.isEmpty()) {
            sb.append("(No other movies reachable in the graph from these seeds — try lowering the similarity threshold in code or add ratings.)\n");
        }
        sb.append("\n");
        output.append(sb.toString());
        output.setCaretPosition(output.getDocument().getLength());
    }

    private String describe(Set<Integer> ids) {
        List<String> parts = new ArrayList<>();
        for (int id : ids) {
            Movie m = service.movieById(id);
            parts.add(m != null ? m.getTitle() : String.valueOf(id));
        }
        return String.join("; ", parts);
    }

    private void runBfs() {
        List<Movie> sel = movieList.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one movie (first is start).", "BFS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int start = sel.get(0).getId();
        List<Integer> order = service.bfsFrom(start, 40);
        output.append("BFS from: " + service.movieById(start).getTitle() + "\n");
        output.append(formatOrder(order) + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void runDfs() {
        List<Movie> sel = movieList.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one movie (first is start).", "DFS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int start = sel.get(0).getId();
        List<Integer> order = service.dfsFrom(start, 40);
        output.append("DFS from: " + service.movieById(start).getTitle() + "\n");
        output.append(formatOrder(order) + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private String formatOrder(List<Integer> order) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int id : order) {
            Movie m = service.movieById(id);
            sb.append(++i).append(". ").append(m != null ? m.getTitle() : id).append("\n");
        }
        return sb.toString();
    }

    public static void show(RecommenderService service, java.nio.file.Path dataDir) {
        SwingUtilities.invokeLater(() -> {
            MovieRecommenderFrame f = new MovieRecommenderFrame(service, new PathHolder(dataDir));
            f.setVisible(true);
        });
    }

    /** Holds path for reload; mutable holder not needed but keeps reference clear. */
    public record PathHolder(java.nio.file.Path path) {
    }
}
