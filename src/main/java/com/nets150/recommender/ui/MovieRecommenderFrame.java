package com.nets150.recommender.ui;

import com.formdev.flatlaf.FlatClientProperties;

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
import javax.swing.JSplitPane;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Interactive Swing UI with clearer sections: pick movies, run Dijkstra recommendations, explore BFS/DFS.
 */
public final class MovieRecommenderFrame extends JFrame {
    private final Path configDir;
    private RecommenderService service;
    private final DefaultListModel<Movie> listModel = new DefaultListModel<>();
    private final JList<Movie> movieList = new JList<>(listModel);
    private final JTextField filterField = new JTextField(22);
    private final JSpinner topKSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 100, 1));
    private final JTextArea output = new JTextArea(18, 52);

    private List<Movie> fullList = List.of();

    public MovieRecommenderFrame(RecommenderService service, Path configDir) {
        super("Movie recommender — similarity graph");
        this.service = service;
        this.configDir = configDir;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        movieList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        movieList.setVisibleRowCount(18);
        movieList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
            ) {
                java.awt.Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Movie m) {
                    setText(String.format(Locale.ROOT, "  %6d  %s", m.getId(), m.getTitle()));
                }
                return c;
            }
        });

        JLabel headline = new JLabel("What should I watch?");
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 20f));

        JLabel sub = new JLabel("<html><body style='width:420px'>"
                + "<b>Step 1.</b> Search and select one or more movies you already like (Ctrl/Cmd-click for several).<br>"
                + "<b>Step 2.</b> Use <b>Get recommendations</b> — the app runs <b>multi-source Dijkstra</b> on a weighted graph "
                + "of similar movies (lower distance = closer).<br>"
                + "<b>Step 3 (optional).</b> <b>BFS</b> / <b>DFS</b> show how the graph is traversed from the <u>first</u> selected movie."
                + "</body></html>");

        JPanel leftHeader = new JPanel(new BorderLayout(0, 8));
        leftHeader.add(headline, BorderLayout.NORTH);
        leftHeader.add(sub, BorderLayout.CENTER);

        JPanel filterRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        filterRow.add(new JLabel("Search"));
        filterField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Title or movie id…");
        filterRow.add(filterField);
        JButton applyFilter = new JButton("Apply");
        applyFilter.addActionListener(e -> applyFilter());
        filterRow.add(applyFilter);

        JPanel listPanel = new JPanel(new BorderLayout(0, 6));
        listPanel.setBorder(BorderFactory.createTitledBorder("Movie list"));
        listPanel.add(filterRow, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(movieList), BorderLayout.CENTER);

        JLabel hint = new JLabel("<html><small style='color:#555'>Tip: pick 2–3 seeds that capture your taste.</small></html>");
        JPanel left = new JPanel(new BorderLayout(0, 10));
        left.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        left.add(leftHeader, BorderLayout.NORTH);
        left.add(listPanel, BorderLayout.CENTER);
        left.add(hint, BorderLayout.SOUTH);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel outHeader = new JPanel(new BorderLayout());
        outHeader.add(new JLabel("Output"), BorderLayout.WEST);

        JPanel recRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        recRow.add(new JLabel("How many picks?"));
        recRow.add(topKSpinner);
        JButton recommend = new JButton("Get recommendations");
        recommend.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);
        recommend.setToolTipText("Rank other movies by shortest-path distance in the similarity graph (Dijkstra, multi-source).");
        recommend.addActionListener(e -> runRecommendations());
        recRow.add(recommend);

        JPanel exploreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        exploreRow.setBorder(BorderFactory.createTitledBorder("Graph traversal (class requirement)"));
        JButton bfs = new JButton("Show BFS order");
        bfs.setToolTipText("Breadth-first visit order from the first selected movie.");
        bfs.addActionListener(e -> runBfs());
        JButton dfs = new JButton("Show DFS order");
        dfs.setToolTipText("Depth-first preorder from the first selected movie.");
        dfs.addActionListener(e -> runDfs());
        exploreRow.add(bfs);
        exploreRow.add(dfs);

        JPanel actions = new JPanel(new BorderLayout(0, 6));
        actions.add(recRow, BorderLayout.NORTH);
        actions.add(exploreRow, BorderLayout.CENTER);

        JButton reload = new JButton("Reload dataset & rebuild graph");
        reload.setToolTipText("Ignores cache; re-reads CSVs and rebuilds edges (slow on MovieLens 20M).");
        reload.addActionListener(e -> reloadFromDisk());
        JPanel reloadRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        reloadRow.add(reload);

        JPanel right = new JPanel(new BorderLayout(0, 8));
        right.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        right.add(outHeader, BorderLayout.NORTH);
        right.add(new JScrollPane(output), BorderLayout.CENTER);
        right.add(actions, BorderLayout.SOUTH);

        JPanel rightWrap = new JPanel(new BorderLayout());
        rightWrap.add(right, BorderLayout.CENTER);
        rightWrap.add(reloadRow, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, rightWrap);
        split.setResizeWeight(0.38);
        split.setDividerSize(6);
        split.setContinuousLayout(true);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        root.add(split, BorderLayout.CENTER);
        setContentPane(root);

        setPreferredSize(new Dimension(1080, 700));
        pack();
        setLocationRelativeTo(null);

        fullList = service.allMovies();
        applyFilter();
        appendWelcome();
    }

    private void appendWelcome() {
        String kind = service.isMovieLens20M() ? "MovieLens 20M (full Kaggle / GroupLens release)" : "small sample CSVs";
        output.setText(
                "Dataset: " + kind + "\n"
                        + "Folder: " + service.dataDirectory().toAbsolutePath() + "\n"
                        + "Config root: " + configDir.toAbsolutePath() + "\n\n"
                        + "Select movies on the left, then click \"Get recommendations\".\n\n"
        );
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
            this.service = RecommenderService.load(configDir, false, s -> output.append(s + "\n"));
            this.fullList = service.allMovies();
            applyFilter();
            appendWelcome();
            output.append("\nReload finished.\n\n");
            output.setCaretPosition(output.getDocument().getLength());
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
            JOptionPane.showMessageDialog(this, "Select at least one movie you like.", "Pick movies", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int topK = ((Number) topKSpinner.getValue()).intValue();
        List<ShortestPathRecommender.ScoredMovie> recs = service.recommend(seeds, topK);
        StringBuilder sb = new StringBuilder();
        sb.append("── Recommendations (Dijkstra / shortest path) ──\n");
        sb.append("You liked: ").append(describe(seeds)).append("\n\n");
        sb.append(String.format(Locale.ROOT, "Top %d matches (lower distance = more similar in the graph):\n\n", recs.size()));
        int rank = 1;
        for (ShortestPathRecommender.ScoredMovie sm : recs) {
            Movie m = service.movieById(sm.movieId());
            String title = m != null ? m.getTitle() : ("id " + sm.movieId());
            sb.append(String.format(Locale.ROOT, "%3d.  %-70s  distance %6.4f%n", rank++, title, sm.distance()));
        }
        if (recs.isEmpty()) {
            sb.append("\nNo reachable movies from your selection in this graph. Try other seeds, or rebuild after changing data.\n");
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
        return String.join(" · ", parts);
    }

    private void runBfs() {
        List<Movie> sel = movieList.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one movie. The first one is the start node.", "BFS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int start = sel.get(0).getId();
        List<Integer> order = service.bfsFrom(start, 50);
        output.append("── BFS visit order ──\n");
        output.append("Start: " + service.movieById(start).getTitle() + "\n");
        output.append(formatOrder(order) + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void runDfs() {
        List<Movie> sel = movieList.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select at least one movie. The first one is the start node.", "DFS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int start = sel.get(0).getId();
        List<Integer> order = service.dfsFrom(start, 50);
        output.append("── DFS preorder ──\n");
        output.append("Start: " + service.movieById(start).getTitle() + "\n");
        output.append(formatOrder(order) + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private String formatOrder(List<Integer> order) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int id : order) {
            Movie m = service.movieById(id);
            sb.append(String.format(Locale.ROOT, "%3d. %s%n", ++i, m != null ? m.getTitle() : id));
        }
        return sb.toString();
    }

    public static void show(RecommenderService service, Path configDir) {
        SwingUtilities.invokeLater(() -> {
            MovieRecommenderFrame f = new MovieRecommenderFrame(service, configDir);
            f.setVisible(true);
        });
    }
}
