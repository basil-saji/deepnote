package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/** Small line icons painted locally to keep the desktop UI dependency-light. */
public final class DeepNoteIcon implements Icon {
    public enum Kind { SEARCH, PLUS, SAVE, EDIT, TRASH, SPARK, CLOSE, SEND, DOCUMENT, CHEVRON }

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
        g.setColor(color);
        g.setStroke(new BasicStroke(Math.max(1.3f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int mid = size / 2;
        switch (kind) {
            case SEARCH -> { g.drawOval(x + 2, y + 2, size - 7, size - 7); g.drawLine(x + size - 6, y + size - 6, x + size - 2, y + size - 2); }
            case PLUS -> { g.drawLine(x + 3, y + mid, x + size - 3, y + mid); g.drawLine(x + mid, y + 3, x + mid, y + size - 3); }
            case SAVE -> { g.drawRoundRect(x + 2, y + 2, size - 4, size - 4, 3, 3); g.drawLine(x + 5, y + 3, x + 5, y + 8); g.drawRoundRect(x + 5, y + 10, size - 10, size - 7, 2, 2); }
            case EDIT -> { g.drawLine(x + 4, y + size - 4, x + 5, y + size - 8); g.drawLine(x + 5, y + size - 8, x + size - 5, y + 3); g.drawLine(x + size - 5, y + 3, x + size - 2, y + 6); g.drawLine(x + size - 2, y + 6, x + 6, y + size - 7); }
            case TRASH -> { g.drawLine(x + 4, y + 5, x + size - 4, y + 5); g.drawLine(x + 6, y + 5, x + 7, y + size - 3); g.drawLine(x + size - 6, y + 5, x + size - 7, y + size - 3); g.drawLine(x + 7, y + 3, x + size - 7, y + 3); g.drawLine(x + 8, y + 1, x + size - 8, y + 1); }
            case SPARK -> { g.drawLine(x + mid, y + 1, x + mid, y + 6); g.drawLine(x + mid, y + size - 6, x + mid, y + size - 1); g.drawLine(x + 1, y + mid, x + 6, y + mid); g.drawLine(x + size - 6, y + mid, x + size - 1, y + mid); g.drawLine(x + 4, y + 4, x + 7, y + 7); g.drawLine(x + size - 7, y + size - 7, x + size - 4, y + size - 4); }
            case CLOSE -> { g.drawLine(x + 4, y + 4, x + size - 4, y + size - 4); g.drawLine(x + size - 4, y + 4, x + 4, y + size - 4); }
            case SEND -> { Polygon arrow = new Polygon(new int[]{x + 2, x + size - 2, x + 2, x + 6}, new int[]{y + 2, y + mid, y + size - 2, y + mid}, 4); g.draw(arrow); g.drawLine(x + 3, y + mid, x + size - 3, y + mid); }
            case DOCUMENT -> { g.drawRoundRect(x + 3, y + 2, size - 6, size - 4, 2, 2); g.drawLine(x + 6, y + 6, x + size - 6, y + 6); g.drawLine(x + 6, y + 9, x + size - 6, y + 9); }
            case CHEVRON -> { g.drawLine(x + 4, y + 6, x + mid, y + size - 5); g.drawLine(x + mid, y + size - 5, x + size - 4, y + 6); }
        }
        g.dispose();
    }
}
