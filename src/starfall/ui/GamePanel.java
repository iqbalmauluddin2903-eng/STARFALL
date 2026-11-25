package starfall.ui;

import starfall.core.GameEngine;

import javax.swing.*;
import java.awt.*;

public class GamePanel extends JPanel {

    private final GameEngine engine;

    public GamePanel(GameEngine engine, int width, int height) {
        this.engine = engine;
        setPreferredSize(new Dimension(width, height));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            engine.render(g2, getWidth(), getHeight());
        } finally {
            g2.dispose();
        }
        Toolkit.getDefaultToolkit().sync();
    }
}