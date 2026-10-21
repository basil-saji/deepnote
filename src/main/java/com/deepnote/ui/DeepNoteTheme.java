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

/**
 * Design system for the DeepNote desktop shell:
 * Refined 2026 dark graphite palette, precise typographic scale, and Look-and-Feel tuning.
 */
public final class DeepNoteTheme {
    // Surfaces: deep graphite/near-black hierarchy with subtle differentiation
    public static final Color BACKGROUND = hex("#0C0D11");       // Base window shell
    public static final Color HEADER = hex("#111318");           // Header, tabs, and secondary surfaces
    public static final Color CANVAS = hex("#15171F");           // Note editor writing surface
    public static final Color SURFACE = hex("#1B1D26");          // Cards, toolbars, elevated elements
    public static final Color SURFACE_HOVER = hex("#222532");    // Interactive hover state
    public static final Color SURFACE_ACTIVE = hex("#2A2D3D");   // Active / pressed surface
    public static final Color SURFACE_SELECTED = hex("#25283B"); // Selected list item background
    public static final Color RAISED = hex("#1E202B");           // Raised blocks, input fields
    public static final Color HOVER = hex("#252837");            // General hover

    // Micro-borders
    public static final Color BORDER = hex("#262937");           // Standard 1px structural border
    public static final Color BORDER_SUBTLE = hex("#1C1E2A");    // Secondary / divider border
    public static final Color BORDER_LIGHT = hex("#33374A");     // Hovered border
    public static final Color BORDER_ACCENT = hex("#43467F");    // Restrained violet border

    // Typography & Content
    public static final Color TEXT = hex("#F3F4F8");             // High contrast primary text
    public static final Color TEXT_SECONDARY = hex("#B4B8CB");   // Secondary body text, previews
    public static final Color MUTED = hex("#7B8097");            // Metadata, inactive icons, labels
    public static final Color FAINT = hex("#4E5267");            // Placeholders, disabled text

    // Restrained Violet/Indigo Accent
    public static final Color ACCENT = hex("#6366F1");           // Modern Indigo/Violet
    public static final Color ACCENT_HOVER = hex("#7578F8");     // Hover accent
    public static final Color ACCENT_PRESSED = hex("#4F52D6");   // Pressed accent
    public static final Color ACCENT_TEXT = hex("#A5B4FC");      // Accent text on dark surfaces
    public static final Color ACCENT_SOFT = hex("#1E2038");      // Subtle tinted surface
    public static final Color ACCENT_BORDER_SOFT = hex("#373A6B");
    public static final Color VIOLET = hex("#8B5CF6");           // Violet gradient pair

    // Semantic States
    public static final Color SUCCESS = hex("#34D399");          // Mint / emerald for saved status
    public static final Color WARNING = hex("#FBBF24");          // Amber for unsaved changes
    public static final Color DANGER = hex("#F87171");           // Rose red for destructive actions
    public static final Color DANGER_SOFT = hex("#2D1A22");      // Soft red surface
    public static final Color DANGER_BORDER = hex("#5C2632");    // Soft red border

    // Typography
    private static final String FAMILY = resolveFamily();
    private static final String MONO_FAMILY = resolveMonoFamily();

    public static final Font DISPLAY = font(28f, TextAttribute.WEIGHT_BOLD);
    public static final Font HEADING = font(20f, TextAttribute.WEIGHT_BOLD);
    public static final Font TITLE = font(16f, TextAttribute.WEIGHT_DEMIBOLD);
    public static final Font TITLE_SM = font(14f, TextAttribute.WEIGHT_DEMIBOLD);
    public static final Font BODY = font(13f, TextAttribute.WEIGHT_REGULAR);
    public static final Font BODY_MEDIUM = font(13f, TextAttribute.WEIGHT_MEDIUM);
    public static final Font CHAT = font(13.5f, TextAttribute.WEIGHT_REGULAR);
    public static final Font SMALL = font(12f, TextAttribute.WEIGHT_REGULAR);
    public static final Font SMALL_MEDIUM = font(12f, TextAttribute.WEIGHT_MEDIUM);
    public static final Font LABEL = font(10.5f, TextAttribute.WEIGHT_BOLD)
            .deriveFont(Map.of(TextAttribute.TRACKING, 0.08f));
    public static final Font KEYCAP = font(11f, TextAttribute.WEIGHT_DEMIBOLD);
    public static final Font EDITOR = font(15.5f, TextAttribute.WEIGHT_REGULAR);
    public static final Font EDITOR_TITLE = font(28f, TextAttribute.WEIGHT_BOLD);
    public static final Font CODE = new Font(MONO_FAMILY, Font.PLAIN, 12);

    private DeepNoteTheme() { }

    public static void install() {
        FlatDarkLaf.setup();
        UIManager.put("defaultFont", BODY);

        // Core component styling
        UIManager.put("Panel.background", CANVAS);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Label.disabledForeground", MUTED);

        // Corner rounding & focus
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.focusColor", ACCENT);
        UIManager.put("Component.borderColor", BORDER);

        // Text Fields
        UIManager.put("TextField.background", RAISED);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", ACCENT_TEXT);
        UIManager.put("TextField.selectionBackground", ACCENT_SOFT);
        UIManager.put("TextField.selectionForeground", TEXT);
        UIManager.put("TextField.padding", new Insets(8, 12, 8, 12));

        // Text Areas
        UIManager.put("TextArea.background", CANVAS);
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.caretForeground", ACCENT_TEXT);
        UIManager.put("TextArea.selectionBackground", hex("#2A2D52"));
        UIManager.put("TextArea.selectionForeground", TEXT);

        // Scrollbars - ultra minimal and polished
        UIManager.put("ScrollPane.background", CANVAS);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.track", alpha(HEADER, 0));
        UIManager.put("ScrollBar.thumb", hex("#2A2D3C"));
        UIManager.put("ScrollBar.hoverThumbColor", hex("#3C4054"));
        UIManager.put("ScrollBar.pressedThumbColor", hex("#505670"));

        // Split Panes - clean 1px divider lines
        UIManager.put("SplitPane.background", HEADER);
        UIManager.put("SplitPane.dividerSize", 1);
        UIManager.put("SplitPaneDivider.gripDotSize", 0);

        // Tooltips
        UIManager.put("ToolTip.background", SURFACE);
        UIManager.put("ToolTip.foreground", TEXT);
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), padding(6, 10, 6, 10)));

        // Option Panes / Dialogs
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("PopupMenu.background", SURFACE);
        UIManager.put("PopupMenu.borderColor", BORDER);

        // Title bar
        UIManager.put("TitlePane.background", HEADER);
        UIManager.put("TitlePane.inactiveBackground", HEADER);
        UIManager.put("TitlePane.foreground", MUTED);
        UIManager.put("TitlePane.inactiveForeground", FAINT);
        UIManager.put("TitlePane.buttonHoverBackground", SURFACE_HOVER);
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
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.clamp(alpha, 0, 255));
    }

    public static Border padding(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }

    public static Border divider(int top, int left, int bottom, int right) {
        return new MatteBorder(top, left, bottom, right, BORDER_SUBTLE);
    }

    public static Border dividerStrong(int top, int left, int bottom, int right) {
        return new MatteBorder(top, left, bottom, right, BORDER);
    }

    public static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    private static void tryInstallInter() {
        try {
            Class.forName("com.formdev.flatlaf.fonts.inter.FlatInterFont").getMethod("install").invoke(null);
        } catch (ReflectiveOperationException | LinkageError exception) {
            // Fall back to system fonts below.
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

    private static String resolveMonoFamily() {
        Set<String> available = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String candidate : new String[]{"JetBrains Mono", "Cascadia Code", "Fira Code", "Consolas", "Monaco"}) {
            if (available.contains(candidate)) return candidate;
        }
        return Font.MONOSPACED;
    }
}
