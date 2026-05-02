package com.nets150.recommender;

import com.nets150.recommender.service.RecommenderService;
import com.nets150.recommender.ui.MovieRecommenderFrame;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.nio.file.Path;

public final class Main {
    public static void main(String[] args) throws Exception {
        Path dataDir = Path.of(args.length > 0 ? args[0] : "data").toAbsolutePath().normalize();
        installLookAndFeel();
        RecommenderService service = RecommenderService.load(dataDir, true);
        MovieRecommenderFrame.show(service, dataDir);
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ignored) {
            // default L&F
        }
    }
}
