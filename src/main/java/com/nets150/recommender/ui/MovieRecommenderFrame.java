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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Browse + persistent "Your picks" seed list (survives search/filter), Dijkstra recommendations, BFS/DFS.
 */
public final class MovieRecommenderFrame extends JFrame {
    private final Path configDir;
    private RecommenderService service;
    private final DefaultListModel<Movie> listModel = new DefaultListModel<>();
    private final JList<Movie> movieList = new JList<>(listModel);
    private final DefaultListModel<Movie> seedModel = new DefaultListModel<>();
    private final JList<Movie> seedList = new JList<>(seedModel);
    private final JTextField filterField = new JTextField(22);
    private final JSpinner topKSpinner = new JSpinner(new SpinnerNumberModel(15, 1, 100, 1));
    private final JTextArea output = new JTextArea(18, 52);

    private List<Movie> fullList = List.of();

    public MovieRecommenderFrame(RecommenderService service, Path configDir) {
        super("Movie recommender — similarity graph");
        this.service = service;
        this.configDir = configDir;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        var cellRenderer = new DefaultListCellRenderer() {
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
        };
        movieList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        movieList.setVisibleRowCount(14);
        movieList.setCellRenderer(cellRenderer);
        seedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        seedList.setVisibleRowCount(6);
        seedList.setCellRenderer(cellRenderer);

        JLabel headline = new JLabel("What should I watch?");
        headline.setFont(headline.getFont().deriveFont(Font.BOLD, 20f));

        JLabel sub = new JLabel("<html><body style='width:440px'>"
                + "<b>1.</b> Search the list below, select one or more rows, click <b>Add to my picks</b> (repeat after new searches — picks are kept).<br>"
                + "<b>2.</b> <b>Your picks</b> is what recommendations use. Remove or clear rows there if you change your mind.<br>"
                + "<b>3.</b> <b>Get recommendations</b> runs multi-source Dijkstra (lower distance = closer in the graph; values use a small edge floor so ties are rare).<br>"
                + "<b>4.</b> <b>BFS / DFS</b> start from the <u>first movie in Your picks</u> (or the browse list if picks are empty)."
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
        listPanel.setBorder(BorderFactory.createTitledBorder("Browse movies"));
        listPanel.add(filterRow, BorderLayout.NORTH);
        listPanel.add(new JScrollPane(movieList), BorderLayout.CENTER);

        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton addPicks = new JButton("Add to my picks");
        addPicks.setToolTipText("Append the current browse selection to Your picks (skips duplicates).");
        addPicks.addActionListener(e -> addSelectionToSeeds());
        addRow.add(addPicks);

        JPanel pickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton removePicks = new JButton("Remove selected");
        removePicks.addActionListener(e -> removeSelectedSeeds());
        JButton clearPicks = new JButton("Clear all");
        clearPicks.addActionListener(e -> seedModel.clear());
        pickActions.add(removePicks);
        pickActions.add(clearPicks);

        JPanel seedPanel = new JPanel(new BorderLayout(0, 4));
        seedPanel.setBorder(BorderFactory.createTitledBorder("Your picks (seeds)"));
        seedPanel.add(new JScrollPane(seedList), BorderLayout.CENTER);
        seedPanel.add(pickActions, BorderLayout.SOUTH);

        JPanel mid = new JPanel(new BorderLayout(0, 8));
        mid.add(listPanel, BorderLayout.CENTER);
        mid.add(addRow, BorderLayout.SOUTH);

        JLabel hint = new JLabel("<html><small style='color:#555'>Searching only filters the browse list — it does not remove movies from Your picks.</small></html>");

        JPanel leftColumn = new JPanel(new BorderLayout(0, 8));
        leftColumn.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
        leftColumn.add(leftHeader, BorderLayout.NORTH);
        JPanel stack = new JPanel(new BorderLayout(0, 6));
        stack.add(mid, BorderLayout.CENTER);
        stack.add(seedPanel, BorderLayout.SOUTH);
        leftColumn.add(stack, BorderLayout.CENTER);
        leftColumn.add(hint, BorderLayout.SOUTH);

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
        recommend.setToolTipText("Rank movies by shortest-path distance from all titles in Your picks.");
        recommend.addActionListener(e -> runRecommendations());
        recRow.add(recommend);

        JPanel exploreRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        exploreRow.setBorder(BorderFactory.createTitledBorder("Graph traversal (class requirement)"));
        JButton bfs = new JButton("Show BFS order");
        bfs.setToolTipText("BFS from the first movie in Your picks, or first browse selection if picks are empty.");
        bfs.addActionListener(e -> runBfs());
        JButton dfs = new JButton("Show DFS order");
        dfs.setToolTipText("DFS from the first movie in Your picks, or first browse selection if picks are empty.");
        dfs.addActionListener(e -> runDfs());
        exploreRow.add(bfs);
        exploreRow.add(dfs);

        JPanel actions = new JPanel(new BorderLayout(0, 6));
        actions.add(recRow, BorderLayout.NORTH);
        actions.add(exploreRow, BorderLayout.CENTER);

        JButton reload = new JButton("Reload dataset & rebuild graph");
        reload.setToolTipText("Rebuilds graph (delete cache first for a clean rebuild).");
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

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftColumn, rightWrap);
        split.setResizeWeight(0.40);
        split.setDividerSize(6);
        split.setContinuousLayout(true);

        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(12, 14, 14, 14));
        root.add(split, BorderLayout.CENTER);
        setContentPane(root);

        setPreferredSize(new Dimension(1100, 720));
        pack();
        setLocationRelativeTo(null);

        fullList = service.allMovies();
        applyFilter();
        appendWelcome();
    }

    private void appendWelcome() {
        String kind = service.isMovieLens20M() ? "MovieLens 20M (Kaggle / GroupLens)" : "small sample CSVs";
        output.setText(
                "Dataset: " + kind + "\n"
                        + "Folder: " + service.dataDirectory().toAbsolutePath() + "\n"
                        + "Config root: " + configDir.toAbsolutePath() + "\n\n"
                        + "Add several movies to **Your picks** (they stay when you search again), then click **Get recommendations**.\n\n"
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

    private void addSelectionToSeeds() {
        List<Movie> sel = movieList.getSelectedValuesList();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Select one or more movies in the browse list first.", "Nothing selected", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        HashSet<Integer> have = new HashSet<>();
        for (int i = 0; i < seedModel.size(); i++) {
            have.add(seedModel.get(i).getId());
        }
        int added = 0;
        for (Movie m : sel) {
            if (have.add(m.getId())) {
                seedModel.addElement(m);
                added++;
            }
        }
        if (added == 0) {
            JOptionPane.showMessageDialog(this, "Those movies are already in Your picks.", "No change", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void removeSelectedSeeds() {
        List<Movie> sel = seedList.getSelectedValuesList();
        if (sel.isEmpty()) {
            return;
        }
        HashSet<Integer> remove = new HashSet<>();
        for (Movie m : sel) {
            remove.add(m.getId());
        }
        for (int i = seedModel.size() - 1; i >= 0; i--) {
            if (remove.contains(seedModel.get(i).getId())) {
                seedModel.remove(i);
            }
        }
    }

    private void reloadFromDisk() {
        try {
            this.service = RecommenderService.load(configDir, false, s -> output.append(s + "\n"));
            this.fullList = service.allMovies();
            seedModel.clear();
            applyFilter();
            appendWelcome();
            output.append("\nReload finished.\n\n");
            output.setCaretPosition(output.getDocument().getLength());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Reload failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Set<Integer> seedIdsOrdered() {
        LinkedHashSet<Integer> ids = new LinkedHashSet<>();
        for (int i = 0; i < seedModel.size(); i++) {
            ids.add(seedModel.get(i).getId());
        }
        return ids;
    }

    private Movie firstTraversalStart() {
        if (seedModel.size() > 0) {
            return seedModel.get(0);
        }
        List<Movie> sel = movieList.getSelectedValuesList();
        if (!sel.isEmpty()) {
            return sel.get(0);
        }
        return null;
    }

    private void runRecommendations() {
        Set<Integer> seeds = seedIdsOrdered();
        if (seeds.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Add at least one movie to **Your picks** (use \"Add to my picks\").",
                    "No seeds",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        int topK = ((Number) topKSpinner.getValue()).intValue();
        List<ShortestPathRecommender.ScoredMovie> recs = service.recommend(seeds, topK);
        StringBuilder sb = new StringBuilder();
        sb.append("── Recommendations (Dijkstra / shortest path) ──\n");
        sb.append("Your picks: ").append(describe(seeds)).append("\n\n");
        sb.append(String.format(Locale.ROOT, "Top %d (lower distance = closer; edge weights use a tiny minimum so values are rarely identical):\n\n", recs.size()));
        int rank = 1;
        for (ShortestPathRecommender.ScoredMovie sm : recs) {
            Movie m = service.movieById(sm.movieId());
            String title = m != null ? m.getTitle() : ("id " + sm.movieId());
            sb.append(String.format(Locale.ROOT, "%3d.  %-65s  distance %10.6f%n", rank++, title, sm.distance()));
        }
        if (recs.isEmpty()) {
            sb.append("\nNo reachable movies from your picks in this graph.\n");
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
        Movie start = firstTraversalStart();
        if (start == null) {
            JOptionPane.showMessageDialog(this, "Add a pick or select a movie in the browse list.", "BFS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Integer> order = service.bfsFrom(start.getId(), 50);
        output.append("── BFS visit order ──\n");
        output.append("Start: " + start.getTitle() + "\n");
        output.append(formatOrder(order) + "\n\n");
        output.setCaretPosition(output.getDocument().getLength());
    }

    private void runDfs() {
        Movie start = firstTraversalStart();
        if (start == null) {
            JOptionPane.showMessageDialog(this, "Add a pick or select a movie in the browse list.", "DFS", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<Integer> order = service.dfsFrom(start.getId(), 50);
        output.append("── DFS preorder ──\n");
        output.append("Start: " + start.getTitle() + "\n");
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
