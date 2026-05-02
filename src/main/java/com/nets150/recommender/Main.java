package com.nets150.recommender;

import com.nets150.recommender.service.RecommenderService;
import com.nets150.recommender.ui.MovieRecommenderFrame;

import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.nio.file.Path;
import java.util.List;

public final class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception ignored) {
            // FlatLaf optional at runtime if classpath incomplete
        }

        Path configDir = Path.of(args.length > 0 ? args[0] : "data").toAbsolutePath().normalize();

        SwingUtilities.invokeLater(() -> {
            JDialog loading = new JDialog((java.awt.Frame) null);
            loading.setTitle("Loading");
            loading.setModal(false);
            loading.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            JLabel status = new JLabel("Resolving dataset…");
            status.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);

            JPanel inner = new JPanel(new BorderLayout(8, 8));
            inner.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));
            inner.add(new JLabel("<html><b>Movie graph recommender</b><br>"
                    + "<span style='color:#555'>Preparing data and similarity graph…</span></html>"), BorderLayout.NORTH);
            inner.add(status, BorderLayout.CENTER);
            inner.add(bar, BorderLayout.SOUTH);
            loading.setContentPane(inner);
            loading.pack();
            loading.setLocationRelativeTo(null);
            loading.setVisible(true);

            SwingWorker<RecommenderService, String> worker = new SwingWorker<>() {
                @Override
                protected RecommenderService doInBackground() throws Exception {
                    return RecommenderService.load(configDir, true, this::publish);
                }

                @Override
                protected void process(List<String> chunks) {
                    if (!chunks.isEmpty()) {
                        status.setText(chunks.get(chunks.size() - 1));
                    }
                }

                @Override
                protected void done() {
                    loading.dispose();
                    try {
                        RecommenderService service = get();
                        MovieRecommenderFrame.show(service, configDir);
                    } catch (Exception ex) {
                        Throwable t = ex;
                        if (ex instanceof java.util.concurrent.ExecutionException ee && ee.getCause() != null) {
                            t = ee.getCause();
                        }
                        JOptionPane.showMessageDialog(
                                null,
                                t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName(),
                                "Could not start",
                                JOptionPane.ERROR_MESSAGE
                        );
                        System.exit(1);
                    }
                }
            };
            worker.execute();
        });
    }
}
