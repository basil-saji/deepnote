package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Modern tactile icon badge for brand marks, AI avatars, and section artwork.
 * Rendered with subtle depth, rounded geometry, and ambient tonal borders.
 */
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

        int arc = Math.round(size * 0.32f);

        if (solid) {
            // Refined diagonal gradient from modern indigo to violet
            GradientPaint gradient = new GradientPaint(
                    0, 0, DeepNoteTheme.ACCENT,
                    size, size, DeepNoteTheme.VIOLET
            );
            g.setPaint(gradient);
            g.fillRoundRect(0, 0, size, size, arc, arc);

            // Subtle top-left highlight border
            g.setColor(DeepNoteTheme.alpha(Color.WHITE, 40));
            g.drawRoundRect(0, 0, size - 1, size - 1, arc, arc);
        } else {
            g.setColor(DeepNoteTheme.ACCENT_SOFT);
            g.fillRoundRect(0, 0, size, size, arc, arc);

            g.setColor(DeepNoteTheme.ACCENT_BORDER_SOFT);
            g.drawRoundRect(0, 0, size - 1, size - 1, arc, arc);
        }

        int iconSize = Math.max(12, Math.round(size * 0.52f));
        Color iconColor = solid ? Color.WHITE : DeepNoteTheme.ACCENT_TEXT;
        int iconOffset = (size - iconSize) / 2;
        new DeepNoteIcon(kind, iconColor, iconSize).paintIcon(this, g, iconOffset, iconOffset);

        g.dispose();
    }
}
