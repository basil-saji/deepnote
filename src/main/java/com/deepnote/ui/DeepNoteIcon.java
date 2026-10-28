package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Precision vector line icons drawn on a 24-unit grid.
 * Rendered with anti-aliasing and rounded caps/joins for a crisp native 2026 finish.
 */
public final class DeepNoteIcon implements Icon {
    public enum Kind {
        SEARCH, PLUS, SAVE, EDIT, TRASH, SPARK, CLOSE, SEND, DOCUMENT, CHEVRON,
        SIDEBAR_RIGHT, CHECK, LOCK, REFRESH, KEYBOARD, CLOCK
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
        g.setStroke(new BasicStroke(1.85f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (kind) {
            case SEARCH -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 13.5, 13.5));
                poly(g, false, 14.5, 14.5, 20.5, 20.5);
            }
            case PLUS -> {
                poly(g, false, 5, 12, 19, 12);
                poly(g, false, 12, 5, 12, 19);
            }
            case SAVE -> {
                Path2D.Double disk = new Path2D.Double();
                disk.moveTo(4, 4);
                disk.lineTo(16, 4);
                disk.lineTo(20, 8);
                disk.lineTo(20, 20);
                disk.lineTo(4, 20);
                disk.closePath();
                g.draw(disk);
                poly(g, false, 8, 4, 8, 9, 16, 9, 16, 4);
                g.draw(new RoundRectangle2D.Double(7, 13, 10, 7, 2, 2));
            }
            case EDIT -> {
                poly(g, true, 4, 20, 4.8, 15.6, 15.6, 4.8, 19.2, 8.4, 8.4, 19.2);
                poly(g, false, 13.8, 6.6, 17.4, 10.2);
            }
            case TRASH -> {
                poly(g, false, 4, 6.5, 20, 6.5);
                poly(g, false, 6.5, 6.5, 7.5, 19.5, 16.5, 19.5, 17.5, 6.5);
                poly(g, false, 9, 6.5, 9, 4, 15, 4, 15, 6.5);
                poly(g, false, 10, 10.5, 10, 15.5);
                poly(g, false, 14, 10.5, 14, 15.5);
            }
            case SPARK -> {
                // Primary 4-point star
                Path2D.Double star = new Path2D.Double();
                star.moveTo(11, 2.5);
                star.quadTo(11, 10.5, 3, 10.5);
                star.quadTo(11, 10.5, 11, 18.5);
                star.quadTo(11, 10.5, 19, 10.5);
                star.quadTo(11, 10.5, 11, 2.5);
                star.closePath();
                g.draw(star);
                // Micro secondary spark
                poly(g, false, 18, 16, 18, 21);
                poly(g, false, 15.5, 18.5, 20.5, 18.5);
            }
            case CLOSE -> {
                poly(g, false, 6.5, 6.5, 17.5, 17.5);
                poly(g, false, 17.5, 6.5, 6.5, 17.5);
            }
            case SEND -> {
                // Modern clean upward-pointing messaging arrow
                poly(g, false, 12, 19.5, 12, 4.5);
                poly(g, false, 5.5, 11, 12, 4.5, 18.5, 11);
            }
            case DOCUMENT -> {
                Path2D.Double page = new Path2D.Double();
                page.moveTo(14, 3);
                page.lineTo(6.5, 3);
                page.quadTo(4.5, 3, 4.5, 5);
                page.lineTo(4.5, 19);
                page.quadTo(4.5, 21, 6.5, 21);
                page.lineTo(17.5, 21);
                page.quadTo(19.5, 21, 19.5, 19);
                page.lineTo(19.5, 8.5);
                page.closePath();
                g.draw(page);
                poly(g, false, 14, 3, 14, 8.5, 19.5, 8.5);
                poly(g, false, 8.5, 13, 15.5, 13);
                poly(g, false, 8.5, 17, 13.5, 17);
            }
            case CHEVRON -> poly(g, false, 8, 10, 12, 14.5, 16, 10);
            case SIDEBAR_RIGHT -> {
                g.draw(new RoundRectangle2D.Double(3, 4, 18, 16, 4, 4));
                g.draw(new Line2D.Double(14.5, 4, 14.5, 20));
            }
            case CHECK -> poly(g, false, 4.5, 12.5, 9.5, 17.5, 19.5, 6.5);
            case LOCK -> {
                g.draw(new RoundRectangle2D.Double(5, 11, 14, 10, 3, 3));
                Path2D.Double shackle = new Path2D.Double();
                shackle.moveTo(8.5, 11);
                shackle.lineTo(8.5, 7.5);
                shackle.quadTo(8.5, 4.5, 12, 4.5);
                shackle.quadTo(15.5, 4.5, 15.5, 7.5);
                shackle.lineTo(15.5, 11);
                g.draw(shackle);
            }
            case REFRESH -> {
                // Circular refresh arrow
                Path2D.Double arc = new Path2D.Double();
                arc.moveTo(12, 4);
                arc.curveTo(17, 4, 20, 7.5, 20, 12);
                arc.curveTo(20, 16.5, 16.5, 20, 12, 20);
                arc.curveTo(7.5, 20, 4, 16.5, 4, 12);
                arc.curveTo(4, 9, 5.5, 6.5, 8, 5);
                g.draw(arc);
                poly(g, false, 8, 2, 8, 5.5, 4.5, 5.5);
            }
            case KEYBOARD -> {
                g.draw(new RoundRectangle2D.Double(3, 6, 18, 12, 3, 3));
                poly(g, false, 7, 10, 7.5, 10);
                poly(g, false, 11, 10, 11.5, 10);
                poly(g, false, 15, 10, 15.5, 10);
                poly(g, false, 8, 14, 16, 14);
            }
            case CLOCK -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                poly(g, false, 12, 7.5, 12, 12, 15.5, 14);
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
