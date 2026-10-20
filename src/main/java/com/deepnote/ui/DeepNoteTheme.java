package com.deepnote.ui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.font.TextAttribute;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Shared design system for the DeepNote desktop shell: palette, type scale and Look-and-Feel tuning. */
public final class DeepNoteTheme {
    // Surfaces: sidebar is the darkest layer, the canvas is the writing surface, raised sits above both.
    public static final Color SIDEBAR = hex("#0F1015");
    public static final Color CANVAS = hex("#15161C");
    public static final Color RAISED = hex("#1C1E26");
    public static final Color HOVER = hex("#242630");
    public static final Color BORDER = hex("#2D303C");
    public static final Color BORDER_SOFT = hex("#22242E");

    // Text
    public static final Color TEXT = hex("#ECEDF3");
    public static final Color TEXT_SECONDARY = hex("#B7BAC9");
    public static final Color MUTED = hex("#8B8FA1");
    public static final Color FAINT = hex("#5E6173");

    // Accent (indigo -> violet) and semantic colours
    public static final Color ACCENT = hex("#6C70F2");
    public static final Color ACCENT_HOVER = hex("#7E82FF");
    public static final Color ACCENT_PRESSED = hex("#5B5FDB");
    public static final Color ACCENT_TEXT = hex("#AEB1FF");
    public static final Color ACCENT_SOFT = hex("#23264F");
    public static final Color VIOLET = hex("#A66BFF");
    public static final Color SUCCESS = hex("#4ADE9B");
    public static final Color WARNING = hex("#F5B94A");
    public static final Color DANGER = hex("#FF6B7F");
    public static final Color DANGER_SOFT = hex("#38202A");

    // Type scale. Inter is used when present (see tryInstallInter), otherwise the best system UI font.
    private static final String FAMILY = resolveFamily();
    public static final Font BODY = font(13f, TextAttribute.WEIGHT_REGULAR);
    public static final Font BODY_MEDIUM = font(13f, TextAttribute.WEIGHT_MEDIUM);
    public static final Font CHAT = font(14f, TextAttribute.WEIGHT_REGULAR);
    public static final Font SMALL = font(12f, TextAttribute.WEIGHT_REGULAR);
    public static final Font SMALL_MEDIUM = font(12f, TextAttribute.WEIGHT_MEDIUM);
    public static final Font LABEL = font(11f, TextAttribute.WEIGHT_DEMIBOLD)
            .deriveFont(Map.of(TextAttribute.TRACKING, 0.08f));
    public static final Font TITLE_SM = font(15f, TextAttribute.WEIGHT_DEMIBOLD);
    public static final Font HEADING = font(20f, TextAttribute.WEIGHT_BOLD);
    public static final Font DISPLAY = font(34f, TextAttribute.WEIGHT_BOLD);
    public static final Font EDITOR = font(16f, TextAttribute.WEIGHT_REGULAR);
    public static final Font EDITOR_TITLE = font(30f, TextAttribute.WEIGHT_BOLD);

    private DeepNoteTheme() { }

    public static void install() {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", BODY);
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.disabledForeground", MUTED);
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.borderColor", BORDER);
        UIManager.put("TextField.background", RAISED);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", ACCENT_TEXT);
        UIManager.put("TextField.selectionBackground", ACCENT_SOFT);
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("TextField.padding", new Insets(9, 12, 9, 12));
        UIManager.put("TextArea.background", CANVAS);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", ACCENT_TEXT);
        UIManager.put("TextArea.selectionBackground", hex("#33376E"));
        UIManager.put("TextArea.selectionForeground", TEXT);
        UIManager.put("ScrollPane.background", CANVAS);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(3, 3, 3, 3));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.track", alpha(SIDEBAR, 0));
        UIManager.put("ScrollBar.thumb", hex("#30333F"));
        UIManager.put("ScrollBar.hoverThumbColor", hex("#444859"));
        UIManager.put("ScrollBar.pressedThumbColor", hex("#585D72"));
        UIManager.put("ToolTip.background", RAISED);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), padding(6, 10, 6, 10)));
        UIManager.put("OptionPane.background", CANVAS);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("TitlePane.background", SIDEBAR);
        UIManager.put("TitlePane.inactiveBackground", SIDEBAR);
        UIManager.put("TitlePane.foreground", MUTED);
        UIManager.put("TitlePane.inactiveForeground", FAINT);
        UIManager.put("TitlePane.buttonHoverBackground", HOVER);
        UIManager.put("TitlePane.buttonPressedBackground", BORDER);
    }

    public static Font font(float size, float weight) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.FAMILY, FAMILY);
        attributes.put(TextAttribute.SIZE, size);
        attributes.put(TextAttribute.WEIGHT, weight);
        return new Font(attributes);
    }

    public static Color hex(String value) { return Color.decode(value); }

    public static Color alpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    public static Border divider(int top, int left, int bottom, int right) {
        return new MatteBorder(top, left, bottom, right, BORDER_SOFT);
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /**
     * Uses the optional {@code flatlaf-fonts-inter} artifact when it is on the classpath. It is loaded reflectively so
     * the project still builds and runs without it.
     */
    private static void tryInstallInter() {
        try {
            Class.forName("com.formdev.flatlaf.fonts.inter.FlatInterFont").getMethod("install").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // Optional dependency: fall back to system fonts below.
        }
    }

    private static String resolveFamily() {
        tryInstallInter();
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[]{"Inter", "Segoe UI Variable Text", "Segoe UI", "SF Pro Text",
                "Helvetica Neue", "Roboto", "Ubuntu"}) {
            if (available.contains(candidate)) return candidate;
        }
        return Font.SANS_SERIF;
    }
}
