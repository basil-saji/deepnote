package com.deepnote.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;

/** Shared visual language for the DeepNote desktop shell. */
public final class DeepNoteTheme {
    public static final Color BACKGROUND = Color.decode("#0D1016");
    public static final Color SURFACE = Color.decode("#141923");
    public static final Color ELEVATED = Color.decode("#1B2230");
    public static final Color BORDER = Color.decode("#2A3240");
    public static final Color TEXT = Color.decode("#EDF1F7");
    public static final Color MUTED = Color.decode("#9AA6B8");
    public static final Color ACCENT = Color.decode("#8B8CF8");
    public static final Color ACCENT_HOVER = Color.decode("#A7A0FF");
    public static final Color SUCCESS = Color.decode("#5ED6A0");
    public static final Color WARNING = Color.decode("#F3C969");
    public static final Color DANGER = Color.decode("#F0788B");
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font BODY_LARGE = new Font("Segoe UI", Font.PLAIN, 15);
    public static final Font LABEL = new Font("Segoe UI", Font.BOLD, 11);
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font DISPLAY = new Font("Segoe UI", Font.BOLD, 28);

    private DeepNoteTheme() { }

    public static void install() {
        FlatDarkLaf.setup();
        UIManager.put("Panel.background", BACKGROUND);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.disabledForeground", MUTED);
        UIManager.put("TextArea.background", ELEVATED);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", ACCENT_HOVER);
        UIManager.put("TextArea.selectionBackground", Color.decode("#3A3B70"));
        UIManager.put("TextArea.selectionForeground", TEXT);
        UIManager.put("TextField.background", ELEVATED);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", ACCENT_HOVER);
        UIManager.put("TextField.selectionBackground", Color.decode("#3A3B70"));
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("TabbedPane.contentAreaColor", SURFACE);
        UIManager.put("TabbedPane.selectedBackground", SURFACE);
        UIManager.put("TabbedPane.underlineColor", ACCENT);
        UIManager.put("TabbedPane.selectedLabelColor", TEXT);
        UIManager.put("ScrollPane.background", BACKGROUND);
        UIManager.put("ScrollBar.track", SURFACE);
        UIManager.put("ScrollBar.thumb", BORDER);
        UIManager.put("ScrollBar.hoverThumbColor", MUTED);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 8);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("TextField.padding", new Insets(8, 11, 8, 11));
        UIManager.put("Button.font", BODY);
        UIManager.put("defaultFont", BODY);
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    public static Border divider(int top, int left, int bottom, int right) {
        return new MatteBorder(top, left, bottom, right, BORDER);
    }
}
