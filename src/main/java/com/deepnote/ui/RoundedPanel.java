package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/** Transparent panel that paints a rounded, optionally outlined surface behind its children. */
public class RoundedPanel extends JPanel {
    private Color fill;
    private Color stroke;
    private final int arc;

    public RoundedPanel(LayoutManager layout, int arc, Color fill, Color stroke) {
        super(layout);
        this.arc = arc;
        this.fill = fill;
        this.stroke = stroke;
        setOpaque(false);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setStroke(Color stroke) {
        this.stroke = stroke;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        DeepNoteTheme.smooth(g);
        if (fill != null) {
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        }
        if (stroke != null) {
            g.setColor(stroke);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
