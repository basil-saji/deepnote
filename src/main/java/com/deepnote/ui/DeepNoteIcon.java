package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/** Rounded line icons drawn on a 24-unit grid, so they stay crisp at any size without image assets. */
public final class DeepNoteIcon implements Icon {
    public enum Kind {
        SEARCH, PLUS, SAVE, EDIT, TRASH, SPARK, CLOSE, SEND, DOCUMENT, CHEVRON,
        SIDEBAR_LEFT, SIDEBAR_RIGHT, CHECK, LOCK
    }

    private final Kind kind;
    private final Color color;
    private final int size;

    public DeepNoteIcon(Kind kind, Color color) { this(kind, color, 16); }

    public DeepNoteIcon(Kind kind, Color color, int size) {
        this.kind = kind;
        this.color = color;
        this.size = size;
    }

    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }

    @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        DeepNoteTheme.smooth(g);
        g.translate(x, y);
        double scale = size / 24.0;
        g.scale(scale, scale);
        g.setColor(color);
        g.setStroke(new BasicStroke(1.75f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case SEARCH -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 15, 15));
                poly(g, false, 16.5, 16.5, 21, 21);
            }
            case PLUS -> {
                poly(g, false, 5, 12, 19, 12);
                poly(g, false, 12, 5, 12, 19);
            }
            case SAVE -> {
                g.draw(new RoundRectangle2D.Double(4, 3, 16, 18, 4, 4));
                poly(g, false, 8, 3, 8, 8, 15, 8, 15, 3);
                g.draw(new RoundRectangle2D.Double(8, 13, 8, 8, 2, 2));
            }
            case EDIT -> {
                poly(g, true, 4, 20, 5.2, 15.5, 16, 4.7, 19.3, 8, 8.5, 18.8);
                poly(g, false, 14, 6.8, 17.2, 10);
            }
            case TRASH -> {
                poly(g, false, 4, 7, 20, 7);
                poly(g, false, 6, 7, 7, 20, 17, 20, 18, 7);
                poly(g, false, 9, 7, 9, 4, 15, 4, 15, 7);
                poly(g, false, 10, 11, 10, 16);
                poly(g, false, 14, 11, 14, 16);
            }
            case SPARK -> {
                poly(g, true, 12, 3.5, 13.9, 9.1, 19.5, 11, 13.9, 12.9, 12, 18.5, 10.1, 12.9, 4.5, 11, 10.1, 9.1);
                poly(g, false, 18.5, 16, 18.5, 21);
                poly(g, false, 16, 18.5, 21, 18.5);
            }
            case CLOSE -> {
                poly(g, false, 6, 6, 18, 18);
                poly(g, false, 18, 6, 6, 18);
            }
            case SEND -> {
                poly(g, false, 12, 19, 12, 5);
                poly(g, false, 6, 11, 12, 5, 18, 11);
            }
            case DOCUMENT -> {
                Path2D.Double page = new Path2D.Double();
                page.moveTo(14, 3);
                page.lineTo(7, 3);
                page.quadTo(5, 3, 5, 5);
                page.lineTo(5, 19);
                page.quadTo(5, 21, 7, 21);
                page.lineTo(17, 21);
                page.quadTo(19, 21, 19, 19);
                page.lineTo(19, 8);
                page.closePath();
                g.draw(page);
                poly(g, false, 14, 3, 14, 8, 19, 8);
                poly(g, false, 9, 13, 15, 13);
                poly(g, false, 9, 17, 15, 17);
            }
            case CHEVRON -> poly(g, false, 7, 10, 12, 15, 17, 10);
            case SIDEBAR_LEFT -> {
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
                g.draw(new Line2D.Double(9, 4, 9, 20));
            }
            case SIDEBAR_RIGHT -> {
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
                g.draw(new Line2D.Double(15, 4, 15, 20));
            }
            case CHECK -> poly(g, false, 5, 12.5, 10, 17.5, 19, 7);
            case LOCK -> {
                g.draw(new RoundRectangle2D.Double(5, 11, 14, 10, 3, 3));
                Path2D.Double shackle = new Path2D.Double();
                shackle.moveTo(8, 11);
                shackle.lineTo(8, 8);
                shackle.quadTo(8, 4, 12, 4);
                shackle.quadTo(16, 4, 16, 8);
                shackle.lineTo(16, 11);
                g.draw(shackle);
            }
        }
        g.dispose();
    }

    private static void poly(Graphics2D g, boolean closed, double... points) {
        Path2D.Double path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        if (closed) path.closePath();
        g.draw(path);
    }
}
