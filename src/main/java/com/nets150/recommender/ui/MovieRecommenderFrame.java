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
import javax.swing.border.EmptyBorder;
import javax.swing.border.CompoundBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
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
    private static final Color BG_PRIMARY = new Color(248, 249, 250);
    private static final Color BG_CARD = Color.WHITE;
    private static final Color TEXT_PRIMARY = new Color(33, 37, 41);
    private static final Color TEXT_SECONDARY = new Color(108, 117, 125);
    private static final Color ACCENT_BLUE = new Color(13, 110, 253);
    private static final Color ACCENT_GREEN = new Color(25, 135, 84);
    private static final Color ACCENT_PURPLE = new Color(111, 66, 193);
    private static final Color BORDER_COLOR = new Color(222, 226, 230);
    private static final Color HOVER_COLOR = new Color(248, 249, 250);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    private static final Font FONT_HEADING = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_SUBHEADING = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);

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
        super("Movie Recommendation System");
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
                    setFont(FONT_BODY);
                }
                if (isSelected) {
                    setBackground(ACCENT_BLUE);
                    setForeground(Color.WHITE);
                } else {
                    setBackground(index % 2 == 0 ? BG_CARD : HOVER_COLOR);
                    setForeground(TEXT_PRIMARY);
                }
                setBorder(new EmptyBorder(4, 8, 4, 8));
                return c;
            }
        };
        movieList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        movieList.setVisibleRowCount(12);
        movieList.setCellRenderer(cellRenderer);
        seedList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        seedList.setVisibleRowCount(5);
        seedList.setCellRenderer(cellRenderer);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 0));
        mainPanel.setBackground(BG_PRIMARY);

        JPanel header = createHeader();
        JPanel content = createMainContent();

        mainPanel.add(header, BorderLayout.NORTH);
        mainPanel.add(content, BorderLayout.CENTER);

        setContentPane(mainPanel);
        setPreferredSize(new Dimension(1200, 800));
        pack();
        setLocationRelativeTo(null);

        fullList = service.allMovies();
        applyFilter();
        appendWelcome();
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setBackground(BG_CARD);
        header.setBorder(new CompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
            new EmptyBorder(15, 25, 15, 25)
        ));

        JLabel title = new JLabel("Movie Recommendation System");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        searchPanel.setBackground(BG_CARD);
        JLabel searchLabel = new JLabel("Search:");
        searchLabel.setFont(FONT_BODY);
        filterField.setPreferredSize(new Dimension(250, 30));
        filterField.setFont(FONT_BODY);
        filterField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Title or movie id...");
        JButton applyBtn = createStyledButton("Apply", ACCENT_BLUE, true);
        applyBtn.addActionListener(e -> applyFilter());
        searchPanel.add(searchLabel);
        searchPanel.add(filterField);
        searchPanel.add(applyBtn);

        header.add(title, BorderLayout.WEST);
        header.add(searchPanel, BorderLayout.CENTER);

        return header;
    }

    private JPanel createMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 0));
        content.setBackground(BG_PRIMARY);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setBackground(BG_PRIMARY);
        split.setBorder(null);
        split.setDividerSize(8);
        split.setResizeWeight(0.45);

        JPanel leftPanel = createLeftPanel();
        JPanel rightPanel = createRightPanel();

        split.setLeftComponent(leftPanel);
        split.setRightComponent(rightPanel);

        content.add(split, BorderLayout.CENTER);
        content.setBorder(new EmptyBorder(20, 20, 20, 20));

        return content;
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BG_PRIMARY);

        JPanel instructionsCard = createCard();
        instructionsCard.setLayout(new BorderLayout());
        JLabel instructions = new JLabel("<html><body style='width:380px; padding:10px;'>"
                + "<h3 style='color:#212529;'>How to Use:</h3>"
                + "<p style='margin-top:8px;'><b>1.</b> Search and select movies from the browse list</p>"
                + "<p><b>2.</b> Click 'Add to my picks' to build your selection</p>"
                + "<p><b>3.</b> Use 'Remove selected' or 'Clear all' to manage picks</p>"
                + "<p><b>4.</b> Click 'Get recommendations' for personalized suggestions</p>"
                + "<p><b>5.</b> Use BFS/DFS to explore the movie graph</p>"
                + "</body></html>");
        instructions.setFont(FONT_BODY);
        instructionsCard.add(instructions, BorderLayout.CENTER);

        JPanel browseCard = createCard();
        browseCard.setLayout(new BorderLayout(0, 10));

        JLabel browseTitle = new JLabel("Browse Movies");
        browseTitle.setFont(FONT_HEADING);
        browseTitle.setForeground(TEXT_PRIMARY);
        browseTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        JScrollPane movieScroll = new JScrollPane(movieList);
        movieScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        movieScroll.setPreferredSize(new Dimension(0, 250));

        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        addPanel.setBackground(BG_CARD);
        JButton addButton = createStyledButton("Add to my picks", ACCENT_GREEN, true);
        addButton.setToolTipText("Add selected movies to your picks list");
        addButton.addActionListener(e -> addSelectionToSeeds());
        addPanel.add(addButton);

        browseCard.add(browseTitle, BorderLayout.NORTH);
        browseCard.add(movieScroll, BorderLayout.CENTER);
        browseCard.add(addPanel, BorderLayout.SOUTH);

        JPanel picksCard = createCard();
        picksCard.setLayout(new BorderLayout(0, 10));

        JLabel picksTitle = new JLabel("Your Picks (Seeds)");
        picksTitle.setFont(FONT_HEADING);
        picksTitle.setForeground(TEXT_PRIMARY);
        picksTitle.setBorder(new EmptyBorder(0, 0, 5, 0));

        JScrollPane seedScroll = new JScrollPane(seedList);
        seedScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        seedScroll.setPreferredSize(new Dimension(0, 120));

        JPanel pickActions = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        pickActions.setBackground(BG_CARD);
        JButton removeBtn = createStyledButton("Remove selected", new Color(220, 53, 69), false);
        removeBtn.addActionListener(e -> removeSelectedSeeds());
        JButton clearBtn = createStyledButton("Clear all", new Color(108, 117, 125), false);
        clearBtn.addActionListener(e -> seedModel.clear());
        pickActions.add(removeBtn);
        pickActions.add(clearBtn);

        picksCard.add(picksTitle, BorderLayout.NORTH);
        picksCard.add(seedScroll, BorderLayout.CENTER);
        picksCard.add(pickActions, BorderLayout.SOUTH);

        panel.add(instructionsCard, BorderLayout.NORTH);
        panel.add(browseCard, BorderLayout.CENTER);
        panel.add(picksCard, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 15));
        panel.setBackground(BG_PRIMARY);

        JPanel outputCard = createCard();
        outputCard.setLayout(new BorderLayout(0, 10));

        JLabel outputTitle = new JLabel("Output");
        outputTitle.setFont(FONT_HEADING);
        outputTitle.setForeground(TEXT_PRIMARY);

        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        output.setBackground(new Color(248, 249, 250));
        output.setForeground(TEXT_PRIMARY);

        JScrollPane outputScroll = new JScrollPane(output);
        outputScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));

        outputCard.add(outputTitle, BorderLayout.NORTH);
        outputCard.add(outputScroll, BorderLayout.CENTER);

        JPanel controlsCard = createCard();
        controlsCard.setLayout(new BorderLayout(0, 10));

        JPanel recommendPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        recommendPanel.setBackground(BG_CARD);
        recommendPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Recommendations",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            FONT_SUBHEADING,
            TEXT_PRIMARY
        ));

        JLabel topKLabel = new JLabel("How many recommendations?");
        topKLabel.setFont(FONT_BODY);
        topKSpinner.setFont(FONT_BODY);
        topKSpinner.setPreferredSize(new Dimension(60, 28));

        JButton recommendBtn = createStyledButton("Get recommendations", ACCENT_BLUE, true);
        recommendBtn.setToolTipText("Get movie recommendations based on your picks");
        recommendBtn.addActionListener(e -> runRecommendations());
        recommendBtn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_ROUND_RECT);

        recommendPanel.add(topKLabel);
        recommendPanel.add(topKSpinner);
        recommendPanel.add(recommendBtn);

        JPanel traversalPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        traversalPanel.setBackground(BG_CARD);
        traversalPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER_COLOR),
            "Graph Traversal (Class Requirement)",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            FONT_SUBHEADING,
            TEXT_PRIMARY
        ));

        JButton bfsBtn = createStyledButton("Show BFS order", ACCENT_PURPLE, false);
        bfsBtn.setToolTipText("Breadth-First Search from first pick or selection");
        bfsBtn.addActionListener(e -> runBfs());

        JButton dfsBtn = createStyledButton("Show DFS order", ACCENT_PURPLE, false);
        dfsBtn.setToolTipText("Depth-First Search from first pick or selection");
        dfsBtn.addActionListener(e -> runDfs());

        traversalPanel.add(bfsBtn);
        traversalPanel.add(dfsBtn);

        JPanel reloadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 5));
        reloadPanel.setBackground(BG_CARD);
        JButton reloadBtn = createStyledButton("Reload dataset & rebuild graph", new Color(108, 117, 125), false);
        reloadBtn.setToolTipText("Reload the dataset and rebuild the similarity graph");
        reloadBtn.addActionListener(e -> reloadFromDisk());
        reloadPanel.add(reloadBtn);

        controlsCard.add(recommendPanel, BorderLayout.NORTH);
        controlsCard.add(traversalPanel, BorderLayout.CENTER);
        controlsCard.add(reloadPanel, BorderLayout.SOUTH);

        panel.add(outputCard, BorderLayout.CENTER);
        panel.add(controlsCard, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCard() {
        JPanel card = new JPanel();
        card.setBackground(BG_CARD);
        card.setBorder(new CompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
            new EmptyBorder(15, 15, 15, 15)
        ));
        return card;
    }

    private JButton createStyledButton(String text, Color color, boolean filled) {
        JButton button = new JButton(text);
        button.setFont(FONT_BODY);
        button.setFocusPainted(false);

        if (filled) {
            button.setBackground(color);
            button.setForeground(Color.WHITE);
            button.setBorderPainted(false);
            button.setOpaque(true);
        } else {
            button.setBackground(BG_CARD);
            button.setForeground(color);
            button.setBorder(BorderFactory.createLineBorder(color, 1));
        }

        button.setPreferredSize(new Dimension(button.getPreferredSize().width + 20, 32));
        return button;
    }

    private void appendWelcome() {
        String kind = service.isMovieLens20M() ? "MovieLens 20M (Kaggle / GroupLens)" : "Small Sample Dataset";
        output.setText(
                "═══════════════════════════════════════\n"
                        + "  MOVIE RECOMMENDATION SYSTEM\n"
                        + "═══════════════════════════════════════\n\n"
                        + "Dataset: " + kind + "\n"
                        + "Config: " + configDir.toAbsolutePath() + "\n\n"
                        + "Ready to help you find your next favorite movie!\n"
                        + "Start by searching and adding movies to your picks.\n\n"
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
