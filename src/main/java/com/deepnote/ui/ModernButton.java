package com.deepnote.ui;

import javax.swing.*;
import java.awt.*;

/** Flat, rounded button with hover and pressed states. One class covers every button style in the app. */
public class ModernButton extends JButton {
    public enum Variant { PRIMARY, SOFT, SECONDARY, GHOST, DANGER, CHIP }

    private static final int ICON_SIZE = 16;

    private final Variant variant;
    private final DeepNoteIcon.Kind iconKind;
    private final int iconSize;
    private boolean active;

    public ModernButton(String text, DeepNoteIcon.Kind iconKind, Variant variant) {
        this(text, iconKind, variant, ICON_SIZE);
    }

    public static ModernButton iconOnly(DeepNoteIcon.Kind iconKind, String tooltip, Variant variant, int iconSize) {
        ModernButton button = new ModernButton("", iconKind, variant, iconSize);
        button.setToolTipText(tooltip);
        return button;
    }

    private ModernButton(String text, DeepNoteIcon.Kind iconKind, Variant variant, int iconSize) {
        super(text);
        this.variant = variant;
        this.iconKind = iconKind;
        this.iconSize = iconSize;
        setFont(DeepNoteTheme.BODY_MEDIUM);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setIconTextGap(8);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        putClientProperty("JButton.buttonType", "borderless");
        boolean hasText = text != null && !text.isEmpty();
        int vertical = variant == Variant.CHIP ? 8 : hasText ? 9 : Math.max(4, (iconSize - 6) / 2);
        int horizontal = hasText ? (variant == Variant.CHIP ? 14 : 14) : vertical + 1;
        setBorder(BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
        addChangeListener(event -> refreshColors());
        refreshColors();
    }

    public void setActive(boolean active) {
        this.active = active;
        refreshColors();
        repaint();
    }

    private void refreshColors() {
        Color normal = normalForeground();
        Color hover = hoverForeground();
        setForeground(getModel().isRollover() || getModel().isPressed() ? hover : normal);
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
            case GHOST, DANGER -> active ? DeepNoteTheme.TEXT : DeepNoteTheme.MUTED;
            case CHIP -> DeepNoteTheme.TEXT_SECONDARY;
        };
    }

    private Color hoverForeground() {
        return switch (variant) {
            case PRIMARY -> Color.WHITE;
            case SOFT -> active ? Color.WHITE : DeepNoteTheme.ACCENT_TEXT;
            case DANGER -> DeepNoteTheme.DANGER;
            default -> DeepNoteTheme.TEXT;
        };
    }

    private Color background() {
        boolean hover = getModel().isRollover();
        boolean down = getModel().isPressed();
        return switch (variant) {
            case PRIMARY -> down ? DeepNoteTheme.ACCENT_PRESSED : hover ? DeepNoteTheme.ACCENT_HOVER : DeepNoteTheme.ACCENT;
            case SOFT -> active ? (hover ? DeepNoteTheme.ACCENT_HOVER : DeepNoteTheme.ACCENT)
                    : hover || down ? DeepNoteTheme.hex("#2D3163") : DeepNoteTheme.ACCENT_SOFT;
            case SECONDARY, CHIP -> down || hover ? DeepNoteTheme.HOVER : DeepNoteTheme.RAISED;
            case GHOST -> hover || down || active ? DeepNoteTheme.HOVER : null;
            case DANGER -> hover || down ? DeepNoteTheme.DANGER_SOFT : null;
        };
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        DeepNoteTheme.smooth(g);
        if (!isEnabled()) g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        int arc = variant == Variant.CHIP ? 16 : 12;
        Color background = background();
        if (background != null) {
            g.setColor(background);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        }
        if (variant == Variant.SECONDARY || variant == Variant.CHIP) {
            g.setColor(DeepNoteTheme.BORDER);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
        }
        super.paintComponent(g);
        g.dispose();
    }
}
