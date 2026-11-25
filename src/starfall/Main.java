package starfall;

import javax.swing.*;
import starfall.core.GameEngine;
import starfall.utils.Logger;

public class Main {

    public static void main(String[] args) {
        Logger.info("Booting STARFALL: SPACE MYSTERY...");
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {}

            GameEngine engine = new GameEngine();
            engine.start();
        });
    }
}