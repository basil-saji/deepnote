package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Modern surface container that paints a rounded, optionally outlined card behind its children.
 * Includes subtle border and hover styling for cards, chips, and composer boxes.
 */
public class RoundedPanel extends JPanel {
    private Color fill;
    private Color stroke;
    private int arc;

    public RoundedPanel(LayoutManager layout, int arc, Color fill, Color stroke) {
        super(layout);
        this.arc = arc;
        this.fill = fill;
        this.stroke = stroke;
        setOpaque(false);
    }

    public RoundedPanel(int arc, Color fill, Color stroke) {
        this(new BorderLayout(), arc, fill, stroke);
    }

    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setStroke(Color stroke) {
        this.stroke = stroke;
        repaint();
    }

    public void setArc(int arc) {
        this.arc = arc;
        repaint();
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        DeepNoteTheme.smooth(g);

        int width = getWidth();
        int height = getHeight();

        if (fill != null) {
            g.setColor(fill);
            g.fillRoundRect(0, 0, width, height, arc, arc);
        }
        if (stroke != null) {
            g.setColor(stroke);
            g.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
        }
        g.dispose();
        super.paintComponent(graphics);
    }
}
