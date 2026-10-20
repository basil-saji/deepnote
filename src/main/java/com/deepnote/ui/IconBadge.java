package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/** Fixed-size rounded tile with a centred icon: the brand mark, the AI avatar and empty-state artwork. */
public class IconBadge extends JComponent {
    private final DeepNoteIcon.Kind kind;
    private final boolean solid;
    private final int size;

    public IconBadge(DeepNoteIcon.Kind kind, int size, boolean solid) {
        this.kind = kind;
        this.size = size;
        this.solid = solid;
        Dimension dimension = new Dimension(size, size);
        setPreferredSize(dimension);
        setMinimumSize(dimension);
        setMaximumSize(dimension);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        DeepNoteTheme.smooth(g);
        int arc = Math.round(size * 0.34f);
        if (solid) {
            g.setPaint(new GradientPaint(0, 0, DeepNoteTheme.ACCENT, size, size, DeepNoteTheme.VIOLET));
        } else {
            g.setColor(DeepNoteTheme.ACCENT_SOFT);
        }
        g.fillRoundRect(0, 0, size, size, arc, arc);
        int iconSize = Math.round(size * 0.56f);
        Color iconColor = solid ? Color.WHITE : DeepNoteTheme.ACCENT_TEXT;
        new DeepNoteIcon(kind, iconColor, iconSize).paintIcon(this, g, (size - iconSize) / 2, (size - iconSize) / 2);
        g.dispose();
    }
}
