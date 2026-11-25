package starfall.ui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.*;
import starfall.core.GameConfig;
import starfall.utils.Logger;

public class GameWindow extends JFrame {

    private final GameConfig config;
    private final GamePanel gamePanel;

    public GameWindow(GameConfig config, GamePanel gamePanel, Runnable onCloseCallback) {
        super(GameConfig.GAME_TITLE);
        this.config = config;
        this.gamePanel = gamePanel;

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());
        add(gamePanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (onCloseCallback != null) {
                    onCloseCallback.run();
                }
                dispose();
                System.exit(0);
            }
        });
    }

    public void showWindow() {
        setVisible(true);
        gamePanel.requestFocusInWindow();
    }

    public void applyResolution(int width, int height) {
        SwingUtilities.invokeLater(() -> {
            gamePanel.setPreferredSize(new Dimension(width, height));
            if (!config.isFullscreen()) {
                pack();
                setLocationRelativeTo(null);
            }
            gamePanel.revalidate();
            gamePanel.requestFocusInWindow();
            Logger.info("Applied window resolution: " + width + "x" + height);
        });
    }

    public void toggleFullscreen() {
        SwingUtilities.invokeLater(() -> {
            boolean nextFullscreen = !config.isFullscreen();
            config.setFullscreen(nextFullscreen);

            dispose();
            setUndecorated(nextFullscreen);
            if (nextFullscreen) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            } else {
                setExtendedState(JFrame.NORMAL);
                gamePanel.setPreferredSize(new Dimension(config.getWindowWidth(), config.getWindowHeight()));
                pack();
                setLocationRelativeTo(null);
            }
            setVisible(true);
            gamePanel.requestFocusInWindow();
            Logger.info("Fullscreen mode: " + nextFullscreen);
        });
    }
}