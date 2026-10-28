package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/**
 * Modern tactile button with sophisticated hover, pressed, active, focus, and shortcut badge states.
 * Covers primary actions, subtle secondary controls, ghost icon buttons, and prompt chips.
 */
public class ModernButton extends JButton {
    public enum Variant { PRIMARY, SOFT, SECONDARY, GHOST, DANGER, CHIP }

    private static final int ICON_SIZE = 16;

    private final Variant variant;
    private final DeepNoteIcon.Kind iconKind;
    private final int iconSize;
    private boolean active;
    private String shortcutText;

    public ModernButton(String text, DeepNoteIcon.Kind iconKind, Variant variant) {
        this(text, iconKind, variant, ICON_SIZE);
    }

    public static ModernButton iconOnly(DeepNoteIcon.Kind iconKind, String tooltip, Variant variant, int iconSize) {
        ModernButton button = new ModernButton("", iconKind, variant, iconSize);
        button.setToolTipText(tooltip);
        return button;
    }

    public ModernButton(String text, DeepNoteIcon.Kind iconKind, Variant variant, int iconSize) {
        super(text);
        this.variant = variant;
        this.iconKind = iconKind;
        this.iconSize = iconSize;

        setFont(variant == Variant.CHIP ? DeepNoteTheme.SMALL_MEDIUM : DeepNoteTheme.BODY_MEDIUM);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setIconTextGap(8);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty("JButton.buttonType", "borderless");

        updatePadding();
        addChangeListener(event -> refreshColors());
        refreshColors();
    }

    private void updatePadding() {
        boolean hasText = getText() != null && !getText().isEmpty();
        int vertical = variant == Variant.CHIP ? 7 : hasText ? 8 : Math.max(5, (iconSize - 6) / 2);
        int horizontalLeft = hasText ? (variant == Variant.CHIP ? 12 : 14) : vertical + 2;
        int horizontalRight = horizontalLeft;

        if (shortcutText != null && !shortcutText.isEmpty()) {
            FontMetrics fm = getFontMetrics(DeepNoteTheme.KEYCAP);
            int badgeWidth = fm.stringWidth(shortcutText) + 12;
            horizontalRight += badgeWidth + 8;
            setHorizontalAlignment(SwingConstants.LEFT);
        } else {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        setBorder(BorderFactory.createEmptyBorder(vertical, horizontalLeft, vertical, horizontalRight));
    }

    public void setActive(boolean active) {
        this.active = active;
        refreshColors();
        repaint();
    }

    public boolean isActive() {
        return active;
    }

    public void setShortcutText(String text) {
        this.shortcutText = text;
        updatePadding();
        revalidate();
        repaint();
    }

    @Override public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (shortcutText != null && !shortcutText.isEmpty()) {
            FontMetrics fm = getFontMetrics(DeepNoteTheme.KEYCAP);
            int badgeWidth = fm.stringWidth(shortcutText) + 12;
            d.width += badgeWidth + 8;
        }
        return d;
    }

    private void refreshColors() {
        Color normal = normalForeground();
        Color hover = hoverForeground();
        Color current = (getModel().isRollover() || getModel().isPressed() || active) ? hover : normal;
        setForeground(current);
        if (iconKind != null) {
            setIcon(new DeepNoteIcon(iconKind, normal, iconSize));
            setRolloverIcon(new DeepNoteIcon(iconKind, hover, iconSize));
            setPressedIcon(new DeepNoteIcon(iconKind, hover, iconSize));
        }
    }

    private Color normalForeground() {
        return switch (variant) {
            case PRIMARY -> Color.WHITE;
            case SOFT -> active ? Color.WHITE : DeepNoteTheme.ACCENT_TEXT;
            case SECONDARY -> DeepNoteTheme.TEXT;
            case GHOST -> active ? DeepNoteTheme.TEXT : DeepNoteTheme.TEXT_SECONDARY;
            case DANGER -> active ? DeepNoteTheme.DANGER : DeepNoteTheme.MUTED;
            case CHIP -> DeepNoteTheme.TEXT_SECONDARY;
        };
    }

    private Color hoverForeground() {
        return switch (variant) {
            case PRIMARY -> Color.WHITE;
            case SOFT -> active ? Color.WHITE : DeepNoteTheme.TEXT;
            case DANGER -> DeepNoteTheme.DANGER;
            default -> DeepNoteTheme.TEXT;
        };
    }

    private Color backgroundColor() {
        boolean hover = getModel().isRollover();
        boolean down = getModel().isPressed();

        return switch (variant) {
            case PRIMARY -> down ? DeepNoteTheme.ACCENT_PRESSED
                    : hover ? DeepNoteTheme.ACCENT_HOVER
                    : DeepNoteTheme.ACCENT;
            case SOFT -> active ? (hover ? DeepNoteTheme.ACCENT_HOVER : DeepNoteTheme.ACCENT)
                    : hover ? hexAlpha(DeepNoteTheme.ACCENT_SOFT, 1.25)
                    : down ? DeepNoteTheme.hex("#292C4E")
                    : DeepNoteTheme.ACCENT_SOFT;
            case SECONDARY -> down ? DeepNoteTheme.SURFACE_ACTIVE
                    : hover ? DeepNoteTheme.SURFACE_HOVER
                    : DeepNoteTheme.SURFACE;
            case CHIP -> down ? DeepNoteTheme.SURFACE_ACTIVE
                    : hover ? DeepNoteTheme.SURFACE_HOVER
                    : DeepNoteTheme.SURFACE;
            case GHOST -> down ? DeepNoteTheme.SURFACE_ACTIVE
                    : hover || active ? DeepNoteTheme.SURFACE_HOVER
                    : null;
            case DANGER -> down ? DeepNoteTheme.hex("#381F26")
                    : hover || active ? DeepNoteTheme.DANGER_SOFT
                    : null;
        };
    }

    private Color borderColor() {
        boolean hover = getModel().isRollover();
        return switch (variant) {
            case PRIMARY -> hover ? DeepNoteTheme.hex("#8B8DFB") : DeepNoteTheme.ACCENT_HOVER;
            case SOFT -> active ? DeepNoteTheme.ACCENT_HOVER
                    : hover ? DeepNoteTheme.ACCENT_BORDER_SOFT
                    : DeepNoteTheme.BORDER_SUBTLE;
            case SECONDARY, CHIP -> hover ? DeepNoteTheme.BORDER_LIGHT : DeepNoteTheme.BORDER;
            case DANGER -> hover ? DeepNoteTheme.DANGER_BORDER : null;
            case GHOST -> null;
        };
    }

    private static Color hexAlpha(Color c, double factor) {
        int r = Math.min(255, (int) (c.getRed() * factor));
        int g = Math.min(255, (int) (c.getGreen() * factor));
        int b = Math.min(255, (int) (c.getBlue() * factor));
        return new Color(r, g, b, c.getAlpha());
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        DeepNoteTheme.smooth(g);

        if (!isEnabled()) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.40f));
        }

        int arc = variant == Variant.CHIP ? 14 : 9;
        int width = getWidth();
        int height = getHeight();

        Color bg = backgroundColor();
        if (bg != null) {
            g.setColor(bg);
            g.fillRoundRect(0, 0, width, height, arc, arc);
        }

        Color border = borderColor();
        if (border != null) {
            g.setColor(border);
            g.drawRoundRect(0, 0, width - 1, height - 1, arc, arc);
        }

        // Draw keyboard shortcut badge on the far right if present
        if (shortcutText != null && !shortcutText.isEmpty()) {
            g.setFont(DeepNoteTheme.KEYCAP);
            FontMetrics fm = g.getFontMetrics();
            int badgeWidth = fm.stringWidth(shortcutText) + 8;
            int badgeHeight = fm.getHeight() - 2;
            int badgeX = width - badgeWidth - 8;
            int badgeY = (height - badgeHeight) / 2;

            g.setColor(variant == Variant.PRIMARY ? DeepNoteTheme.alpha(Color.BLACK, 70) : DeepNoteTheme.RAISED);
            g.fillRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 5, 5);
            g.setColor(variant == Variant.PRIMARY ? DeepNoteTheme.alpha(Color.WHITE, 60) : DeepNoteTheme.BORDER);
            g.drawRoundRect(badgeX, badgeY, badgeWidth, badgeHeight, 5, 5);

            g.setColor(variant == Variant.PRIMARY ? Color.WHITE : DeepNoteTheme.MUTED);
            g.drawString(shortcutText, badgeX + 4, badgeY + fm.getAscent() - 2);
        }

        // Keyboard focus ring
        if (isFocusOwner()) {
            g.setColor(DeepNoteTheme.alpha(DeepNoteTheme.ACCENT, 140));
            g.drawRoundRect(1, 1, width - 3, height - 3, arc, arc);
        }

        super.paintComponent(g);
        g.dispose();
    }
}
