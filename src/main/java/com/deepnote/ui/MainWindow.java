package com.deepnote.ui;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.ai.ContextManager;
import com.deepnote.ai.OllamaChatModelClient;
import com.deepnote.config.AppConfig;
import com.deepnote.exception.NoteStorageException;
import com.deepnote.model.Note;
import com.deepnote.service.LocalSearchService;
import com.deepnote.service.NoteService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Main DeepNote workspace shell:
 * Homepage-led note browser, responsive tabbed writing surface, adjustable AI assistant split pane,
 * and a cohesive 2026 dark graphite aesthetic.
 */
public class MainWindow extends JFrame {
    private static final int MOD = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    private static final String MOD_LABEL = MOD == InputEvent.META_DOWN_MASK ? "⌘" : "Ctrl";
    private static final int DEFAULT_ASSISTANT_WIDTH = 390;
    private static final int READING_WIDTH = 780;

    private final NoteService noteService;
    private final LocalSearchService searchService = new LocalSearchService();
    private final AIAssistantService assistantService;
    private final String chatModelName;

    private final JTextField searchField = new JTextField();
    private final JPanel contentCards = new JPanel(new CardLayout());
    private final JPanel tabStrip = new JPanel();
    private final JPanel editorStack = new JPanel(new CardLayout());
    private final JPanel homePanel = new JPanel(new BorderLayout());
    private final List<EditorTab> tabOrder = new ArrayList<>();
    private final Map<String, EditorTab> openTabs = new HashMap<>();

    private EditorTab selectedTab;

    public MainWindow(AppConfig config) {
        super("DeepNote");
        this.chatModelName = config.chatModelName();
        this.noteService = new NoteService(config.notesDirectory());
        this.assistantService = new AIAssistantService(
                new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()),
                new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(),
                        Duration.ofSeconds(config.requestTimeoutSeconds())));

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(960, 620));
        setSize(1320, 840);
        setLocationByPlatform(true);

        buildUi();
        reloadList();

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { closeApplication(); }
        });
    }

    // ---------------------------------------------------------------- Layout

    private void buildUi() {
        getContentPane().setBackground(DeepNoteTheme.BACKGROUND);
        getRootPane().putClientProperty("JRootPane.titleBarBackground", DeepNoteTheme.HEADER);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", DeepNoteTheme.MUTED);
        setLayout(new BorderLayout());

        contentCards.setBackground(DeepNoteTheme.CANVAS);
        contentCards.add(buildWorkspace(), "workspace");
        contentCards.add(buildHome(), "home");
        showHome();

        add(contentCards, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { reloadList(); }
            @Override public void removeUpdate(DocumentEvent event) { reloadList(); }
            @Override public void changedUpdate(DocumentEvent event) { reloadList(); }
        });

        installShortcuts();
    }

    private JPanel buildWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(DeepNoteTheme.CANVAS);

        JPanel tabHeader = new JPanel(new BorderLayout());
        tabHeader.setBackground(DeepNoteTheme.HEADER);
        tabHeader.setBorder(DeepNoteTheme.divider(0, 0, 1, 0));

        tabStrip.setOpaque(false);
        tabStrip.setLayout(new BoxLayout(tabStrip, BoxLayout.X_AXIS));
        tabStrip.setBorder(DeepNoteTheme.padding(6, 10, 6, 4));

        JScrollPane tabScroll = new JScrollPane(tabStrip, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabScroll.setBorder(null);
        tabScroll.getViewport().setBackground(DeepNoteTheme.HEADER);
        tabScroll.setBackground(DeepNoteTheme.HEADER);
        tabScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 4));

        ModernButton newTabBtn = ModernButton.iconOnly(DeepNoteIcon.Kind.PLUS, "New note (" + MOD_LABEL + "+N)",
                ModernButton.Variant.GHOST, 14);
        newTabBtn.addActionListener(event -> createNote());
        JPanel newTabHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        newTabHolder.setOpaque(false);
        ModernButton homeButton = new ModernButton("Home", DeepNoteIcon.Kind.DOCUMENT,
                ModernButton.Variant.GHOST);
        homeButton.setToolTipText("Return to home (" + MOD_LABEL + "+K)");
        homeButton.addActionListener(event -> showHome());
        newTabHolder.add(homeButton);
        newTabHolder.add(newTabBtn);

        tabHeader.add(tabScroll, BorderLayout.CENTER);
        tabHeader.add(newTabHolder, BorderLayout.EAST);

        editorStack.setBackground(DeepNoteTheme.CANVAS);
        workspace.add(tabHeader, BorderLayout.NORTH);
        workspace.add(editorStack, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel buildHome() {
        homePanel.setBackground(DeepNoteTheme.CANVAS);
        return homePanel;
    }

    private void rebuildHome(List<Note> notes) {
        homePanel.removeAll();

        ScrollableColumn column = new ScrollableColumn(new BorderLayout(0, 24), false);
        column.setBorder(DeepNoteTheme.padding(40, 48, 60, 48));

        JPanel container = new JPanel();
        container.setOpaque(false);
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));

        configureHomeSearch();
        JPanel searchRow = new JPanel(new BorderLayout(12, 0));
        searchRow.setOpaque(false);
        searchRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        searchRow.add(searchField, BorderLayout.CENTER);
        JLabel resultLabel = new JLabel(notes.size() + (notes.size() == 1 ? " note" : " notes"));
        resultLabel.setFont(DeepNoteTheme.SMALL);
        resultLabel.setForeground(DeepNoteTheme.MUTED);
        searchRow.add(resultLabel, BorderLayout.EAST);
        container.add(searchRow);
        container.add(Box.createVerticalStrut(28));

        // Hero Brand & Heading Block
        JPanel hero = new JPanel();
        hero.setOpaque(false);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);

        IconBadge mark = new IconBadge(DeepNoteIcon.Kind.SPARK, 46, true);
        mark.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel heading = new JLabel("Good ideas start as plain text.");
        heading.setFont(DeepNoteTheme.DISPLAY);
        heading.setForeground(DeepNoteTheme.TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel copy = new JLabel("<html><div style='width:560px;line-height:1.45;color:#9AA0B6'>"
                + "Write freely in lightweight .txt files on your local machine, then query an on-device AI about any note. "
                + "Zero cloud lock-in, zero telemetry, instant offline performance.</div></html>");
        copy.setFont(DeepNoteTheme.BODY);
        copy.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Actions Row
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton create = new ModernButton("Create new note", DeepNoteIcon.Kind.PLUS, ModernButton.Variant.PRIMARY);
        create.setShortcutText(MOD_LABEL + "+N");
        create.addActionListener(event -> createNote());

        ModernButton searchBtn = new ModernButton("Search notes", DeepNoteIcon.Kind.SEARCH, ModernButton.Variant.SECONDARY);
        searchBtn.setShortcutText(MOD_LABEL + "+K");
        searchBtn.addActionListener(event -> focusSearch());

        actions.add(create);
        actions.add(searchBtn);

        // Shortcut Hints Container
        RoundedPanel shortcutPanel = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 16, 6), 10,
                DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER_SUBTLE);
        shortcutPanel.setBorder(DeepNoteTheme.padding(6, 12, 6, 12));
        shortcutPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[][] shortcuts = {
                {MOD_LABEL + "+N", "New note"},
                {MOD_LABEL + "+K", "Search"},
                {MOD_LABEL + "+J", "Ask AI"},
                {MOD_LABEL + "+S", "Save note"}
        };
        for (String[] shortcut : shortcuts) {
            shortcutPanel.add(shortcutHint(shortcut[0], shortcut[1]));
        }

        hero.add(mark);
        hero.add(Box.createVerticalStrut(18));
        hero.add(heading);
        hero.add(Box.createVerticalStrut(10));
        hero.add(copy);
        hero.add(Box.createVerticalStrut(22));
        hero.add(actions);
        hero.add(Box.createVerticalStrut(16));
        hero.add(shortcutPanel);

        container.add(hero);

        // Homepage notes collection: this is the sole browsing surface.
        if (!notes.isEmpty()) {
            JPanel recentSection = new JPanel();
            recentSection.setOpaque(false);
            recentSection.setLayout(new BoxLayout(recentSection, BoxLayout.Y_AXIS));
            recentSection.setAlignmentX(Component.LEFT_ALIGNMENT);

            boolean filtering = !searchField.getText().isBlank();
            JLabel recentLabel = new JLabel(filtering ? "SEARCH RESULTS" : "ALL NOTES");
            recentLabel.setFont(DeepNoteTheme.LABEL);
            recentLabel.setForeground(DeepNoteTheme.FAINT);
            recentLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel grid = new JPanel(new GridLayout(0, 2, 14, 14));
            grid.setOpaque(false);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);

            notes.stream()
                    .sorted(Comparator.comparingLong((Note note) -> lastModified(note.path())).reversed())
                    .forEach(note -> grid.add(recentCard(note)));

            recentSection.add(Box.createVerticalStrut(32));
            recentSection.add(recentLabel);
            recentSection.add(Box.createVerticalStrut(12));
            recentSection.add(grid);

            container.add(recentSection);
        } else {
            RoundedPanel emptyCard = new RoundedPanel(new BorderLayout(0, 8), 12,
                    DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER_SUBTLE);
            emptyCard.setBorder(DeepNoteTheme.padding(20, 24, 20, 24));
            emptyCard.setAlignmentX(Component.LEFT_ALIGNMENT);
            emptyCard.setMaximumSize(new Dimension(560, 96));

            boolean filtering = !searchField.getText().isBlank();
            JLabel emptyTitle = new JLabel(filtering ? "No notes match your search" : "Your workspace is clear");
            emptyTitle.setFont(DeepNoteTheme.BODY_MEDIUM);
            emptyTitle.setForeground(DeepNoteTheme.TEXT);

            JLabel emptyDesc = new JLabel(filtering ? "Try a different title or keyword."
                    : "Create your first plain-text note with the button above or press " + MOD_LABEL + "+N.");
            emptyDesc.setFont(DeepNoteTheme.SMALL);
            emptyDesc.setForeground(DeepNoteTheme.MUTED);

            emptyCard.add(emptyTitle, BorderLayout.NORTH);
            emptyCard.add(emptyDesc, BorderLayout.CENTER);

            JPanel emptyHolder = new JPanel();
            emptyHolder.setOpaque(false);
            emptyHolder.setLayout(new BoxLayout(emptyHolder, BoxLayout.Y_AXIS));
            emptyHolder.setAlignmentX(Component.LEFT_ALIGNMENT);
            emptyHolder.add(Box.createVerticalStrut(28));
            emptyHolder.add(emptyCard);
            container.add(emptyHolder);
        }

        column.add(container, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(column);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(DeepNoteTheme.CANVAS);
        scroll.getVerticalScrollBar().setUnitIncrement(18);

        homePanel.add(scroll, BorderLayout.CENTER);
        homePanel.revalidate();
        homePanel.repaint();
    }

    private JComponent shortcutHint(String keys, String label) {
        JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hint.setOpaque(false);

        RoundedPanel keycap = new RoundedPanel(new BorderLayout(), 6, DeepNoteTheme.RAISED, DeepNoteTheme.BORDER);
        keycap.setBorder(DeepNoteTheme.padding(2, 6, 2, 6));

        JLabel keyText = new JLabel(keys);
        keyText.setFont(DeepNoteTheme.KEYCAP);
        keyText.setForeground(DeepNoteTheme.TEXT_SECONDARY);
        keycap.add(keyText, BorderLayout.CENTER);

        JLabel text = new JLabel(label);
        text.setFont(DeepNoteTheme.SMALL);
        text.setForeground(DeepNoteTheme.MUTED);

        hint.add(keycap);
        hint.add(text);
        return hint;
    }

    private void configureHomeSearch() {
        searchField.putClientProperty("JTextField.placeholderText", "Search all notes by title or content");
        searchField.putClientProperty("JTextField.leadingIcon",
                new DeepNoteIcon(DeepNoteIcon.Kind.SEARCH, DeepNoteTheme.MUTED, 16));
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setFont(DeepNoteTheme.BODY);
        searchField.setToolTipText("Search titles and content (" + MOD_LABEL + "+K)");

        RoundedPanel searchBadge = new RoundedPanel(new BorderLayout(), 5,
                DeepNoteTheme.RAISED, DeepNoteTheme.BORDER_SUBTLE);
        searchBadge.setBorder(DeepNoteTheme.padding(2, 6, 2, 6));
        JLabel searchBadgeText = new JLabel(MOD_LABEL + " K");
        searchBadgeText.setFont(DeepNoteTheme.KEYCAP);
        searchBadgeText.setForeground(DeepNoteTheme.FAINT);
        searchBadge.add(searchBadgeText, BorderLayout.CENTER);
        searchField.putClientProperty("JTextField.trailingComponent", searchBadge);
    }

    private JComponent recentCard(Note note) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), 12,
                DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER_SUBTLE);
        card.setBorder(DeepNoteTheme.padding(14, 16, 14, 16));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Header: Document Icon + Title + Modified Time
        JPanel top = new JPanel(new BorderLayout(8, 0));
        top.setOpaque(false);

        JLabel docIcon = new JLabel(new DeepNoteIcon(DeepNoteIcon.Kind.DOCUMENT, DeepNoteTheme.ACCENT_TEXT, 15));
        JLabel title = new JLabel(note.title());
        title.setFont(DeepNoteTheme.BODY_MEDIUM);
        title.setForeground(DeepNoteTheme.TEXT);

        JPanel titleBlock = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        titleBlock.setOpaque(false);
        titleBlock.add(docIcon);
        titleBlock.add(title);

        JLabel dateLabel = new JLabel(formatRelativeTime(lastModified(note.path())));
        dateLabel.setFont(DeepNoteTheme.SMALL);
        dateLabel.setForeground(DeepNoteTheme.MUTED);

        top.add(titleBlock, BorderLayout.WEST);
        top.add(dateLabel, BorderLayout.EAST);

        // Snippet Preview
        JLabel preview = new JLabel(cleanPreview(note.content(), note.title(), 72));
        preview.setFont(DeepNoteTheme.SMALL);
        preview.setForeground(DeepNoteTheme.TEXT_SECONDARY);

        // Bottom stats
        int words = countWords(note.content());
        JLabel stats = new JLabel(words + (words == 1 ? " word" : " words"));
        stats.setFont(DeepNoteTheme.KEYCAP);
        stats.setForeground(DeepNoteTheme.FAINT);

        card.add(top, BorderLayout.NORTH);
        card.add(preview, BorderLayout.CENTER);
        card.add(stats, BorderLayout.SOUTH);

        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                card.setStroke(DeepNoteTheme.ACCENT);
                card.setFill(DeepNoteTheme.SURFACE_HOVER);
            }
            @Override public void mouseExited(MouseEvent event) {
                if (!card.contains(event.getPoint())) {
                    card.setStroke(DeepNoteTheme.BORDER_SUBTLE);
                    card.setFill(DeepNoteTheme.SURFACE);
                }
            }
            @Override public void mouseClicked(MouseEvent event) {
                openNote(noteService.readNote(note.path()));
            }
        });
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(DeepNoteTheme.HEADER);
        status.setBorder(BorderFactory.createCompoundBorder(
                DeepNoteTheme.divider(1, 0, 0, 0),
                DeepNoteTheme.padding(5, 14, 5, 14)));

        JLabel left = new JLabel("Local workspace", dot(DeepNoteTheme.SUCCESS, 7), SwingConstants.LEFT);
        left.setIconTextGap(7);
        left.setFont(DeepNoteTheme.SMALL);
        left.setForeground(DeepNoteTheme.MUTED);

        JLabel right = new JLabel("Ollama · " + chatModelName + "  ·  UTF-8");
        right.setFont(DeepNoteTheme.SMALL);
        right.setForeground(DeepNoteTheme.FAINT);

        status.add(left, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);
        return status;
    }

    // ---------------------------------------------------------------- Shortcuts

    private void installShortcuts() {
        bind(KeyEvent.VK_N, "newNote", this::createNote);
        bind(KeyEvent.VK_S, "saveNote", this::saveCurrent);
        bind(KeyEvent.VK_K, "focusSearch", this::focusSearch);
        bind(KeyEvent.VK_J, "toggleAssistant", this::toggleCurrentAssistant);
        bind(KeyEvent.VK_W, "closeTab", () -> { if (selectedTab != null) closeTab(selectedTab); });
    }

    private void bind(int keyCode, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, MOD), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { action.run(); }
        });
    }

    private void focusSearch() {
        showHome();
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    private void saveCurrent() {
        if (selectedTab != null) selectedTab.save();
    }

    private void toggleCurrentAssistant() {
        if (selectedTab != null) selectedTab.toggleAssistant(!selectedTab.isAssistantVisible());
    }

    // ---------------------------------------------------------------- Homepage note collection

    private void reloadList() {
        List<Note> all = noteService.listNotes();
        String query = searchField.getText();
        List<Note> shown = searchService.search(all, query);

        rebuildHome(shown);
    }

    // ---------------------------------------------------------------- Tabs

    private void showHome() {
        ((CardLayout) contentCards.getLayout()).show(contentCards, "home");
        reloadList();
    }

    private void showWorkspace() {
        ((CardLayout) contentCards.getLayout()).show(contentCards, "workspace");
    }

    private void createNote() {
        String title = JOptionPane.showInputDialog(this, "Note title:", "Create note",
                JOptionPane.PLAIN_MESSAGE);
        if (title == null || title.trim().isEmpty()) return;
        try {
            Note note = noteService.createNote(title.trim());
            reloadList();
            openNote(note);
        } catch (NoteStorageException exception) {
            showError(exception.getMessage());
        }
    }

    private void openNote(Note note) {
        EditorTab existing = openTabs.get(note.path().toString());
        if (existing != null) {
            selectTab(existing);
            return;
        }
        EditorTab tab = new EditorTab(note);
        openTabs.put(note.path().toString(), tab);
        tabOrder.add(tab);
        tabStrip.add(tab.chipHolder);
        editorStack.add(tab, tab.id);
        selectTab(tab);
    }

    private void selectTab(EditorTab tab) {
        selectedTab = tab;
        ((CardLayout) editorStack.getLayout()).show(editorStack, tab.id);
        tabOrder.forEach(other -> other.chip.setSelectedState(other == tab));
        showWorkspace();
        tabStrip.revalidate();
        tabStrip.repaint();
        SwingUtilities.invokeLater(() -> {
            tab.chipHolder.scrollRectToVisible(new Rectangle(0, 0, tab.chipHolder.getWidth(),
                    tab.chipHolder.getHeight()));
            tab.editor.requestFocusInWindow();
        });
    }

    private void closeTab(EditorTab tab) {
        if (tab.dirty) {
            selectTab(tab);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Save changes to “" + tab.note.title() + "” before closing?", "Unsaved changes",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
            if (choice == JOptionPane.YES_OPTION && !tab.save()) return;
        }
        removeTab(tab);
    }

    private void removeTab(EditorTab tab) {
        int index = tabOrder.indexOf(tab);
        tabOrder.remove(tab);
        openTabs.remove(tab.note.path().toString());
        tabStrip.remove(tab.chipHolder);
        editorStack.remove(tab);
        tabStrip.revalidate();
        tabStrip.repaint();

        if (selectedTab != tab) return;
        selectedTab = null;
        if (tabOrder.isEmpty()) {
            showHome();
        } else {
            selectTab(tabOrder.get(Math.min(index, tabOrder.size() - 1)));
        }
    }

    private void closeApplication() {
        for (EditorTab tab : List.copyOf(tabOrder)) {
            if (!tab.dirty) continue;
            selectTab(tab);
            int choice = JOptionPane.showConfirmDialog(this,
                    "Save changes to “" + tab.note.title() + "” before closing?", "Unsaved changes",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION || choice == JOptionPane.CLOSED_OPTION) return;
            if (choice == JOptionPane.YES_OPTION && !tab.save()) return;
        }
        dispose();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "DeepNote", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------- Helpers

    private static Icon dot(Color color, int diameter) {
        return new Icon() {
            @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
                Graphics2D g = (Graphics2D) graphics.create();
                DeepNoteTheme.smooth(g);
                g.setColor(color);
                g.fillOval(x, y, diameter, diameter);
                g.dispose();
            }
            @Override public int getIconWidth() { return diameter; }
            @Override public int getIconHeight() { return diameter; }
        };
    }

    private static String cleanPreview(String content, String title, int maxLen) {
        if (content == null || content.isBlank()) return "Empty note";
        for (String line : content.split("\\R", 30)) {
            String trimmed = line.replaceAll("^#+\\s*", "").trim();
            if (trimmed.isEmpty()) continue;
            if (title != null && trimmed.equalsIgnoreCase(title.trim())) continue;
            return trimmed.length() > maxLen ? trimmed.substring(0, maxLen) + "…" : trimmed;
        }
        return "Empty note";
    }

    private static int countWords(String content) {
        if (content == null || content.isBlank()) return 0;
        return content.trim().split("\\s+").length;
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L;
        }
    }

    private static String formatRelativeTime(long millis) {
        if (millis <= 0) return "";
        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000) return "Just now";
        if (diff < 3600_000) return (diff / 60_000) + "m ago";
        if (diff < 86400_000) return (diff / 3600_000) + "h ago";
        return DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(millis));
    }

    private static JPanel hbox(int gap, Component... items) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.X_AXIS));
        for (int i = 0; i < items.length; i++) {
            if (i > 0) box.add(Box.createHorizontalStrut(gap));
            box.add(items[i]);
        }
        return box;
    }

    // ---------------------------------------------------------------- Tab Chip

    private final class TabChip extends RoundedPanel {
        private final EditorTab tab;
        private final JLabel title = new JLabel();
        private final JLabel dirtyDot = new JLabel(dot(DeepNoteTheme.WARNING, 6));
        private final ModernButton close;
        private boolean selected;
        private boolean hovered;

        TabChip(EditorTab tab) {
            super(new BorderLayout(8, 0), 8, null, null);
            this.tab = tab;
            setBorder(DeepNoteTheme.padding(5, 10, 5, 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            title.setFont(DeepNoteTheme.BODY_MEDIUM);
            dirtyDot.setVisible(false);

            close = ModernButton.iconOnly(DeepNoteIcon.Kind.CLOSE, "Close (" + MOD_LABEL + "+W)",
                    ModernButton.Variant.GHOST, 12);
            close.addActionListener(event -> closeTab(tab));

            JLabel document = new JLabel(new DeepNoteIcon(DeepNoteIcon.Kind.DOCUMENT, DeepNoteTheme.MUTED, 14));
            add(document, BorderLayout.WEST);
            add(title, BorderLayout.CENTER);
            add(hbox(4, dirtyDot, close), BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent event) { hovered = true; restyle(); }
                @Override public void mouseExited(MouseEvent event) {
                    if (contains(event.getPoint())) return;
                    hovered = false;
                    restyle();
                }
                @Override public void mouseClicked(MouseEvent event) { selectTab(tab); }
            });

            setTitle(tab.note.title());
            restyle();
        }

        void setTitle(String text) { title.setText(text); }
        void setDirty(boolean dirty) { dirtyDot.setVisible(dirty); }

        void setSelectedState(boolean value) {
            selected = value;
            restyle();
        }

        private void restyle() {
            setFill(selected ? DeepNoteTheme.CANVAS : hovered ? DeepNoteTheme.SURFACE_HOVER : DeepNoteTheme.HEADER);
            setStroke(selected ? DeepNoteTheme.BORDER : hovered ? DeepNoteTheme.BORDER_SUBTLE : null);
            title.setForeground(selected ? DeepNoteTheme.TEXT : DeepNoteTheme.MUTED);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (selected) {
                // Top accent line for active tab
                Graphics2D g = (Graphics2D) graphics.create();
                DeepNoteTheme.smooth(g);
                g.setColor(DeepNoteTheme.ACCENT);
                g.fillRoundRect(2, 0, getWidth() - 4, 2, 2, 2);
                g.dispose();
            }
        }
    }

    // ---------------------------------------------------------------- Editor Tab

    private final class EditorTab extends JPanel {
        final String id = UUID.randomUUID().toString();
        final JTextArea editor;
        final AIAssistantPanel assistantPanel;
        final TabChip chip;
        final JPanel chipHolder = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        };

        private final JSplitPane editorSplitPane;
        private final JPanel assistantHost = new JPanel(new BorderLayout());
        private final ScrollableColumn column = new ScrollableColumn(new BorderLayout(0, 20), true);
        private final JLabel breadcrumb = new JLabel();
        private final JLabel titleLabel = new JLabel();
        private final JLabel metaLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();
        private final ModernButton askButton = new ModernButton("Ask AI", DeepNoteIcon.Kind.SPARK,
                ModernButton.Variant.SOFT);

        private int lastAssistantWidth = DEFAULT_ASSISTANT_WIDTH;
        Note note;
        boolean dirty;

        EditorTab(Note note) {
            this.note = note;
            setLayout(new BorderLayout());
            setBackground(DeepNoteTheme.CANVAS);

            chip = new TabChip(this);
            chipHolder.setOpaque(false);
            chipHolder.setBorder(DeepNoteTheme.padding(0, 0, 0, 6));
            chipHolder.add(chip, BorderLayout.CENTER);

            editor = new JTextArea(note.content()) {
                @Override protected void paintComponent(Graphics graphics) {
                    super.paintComponent(graphics);
                    if (getDocument().getLength() > 0) return;
                    Graphics2D g = (Graphics2D) graphics.create();
                    DeepNoteTheme.smooth(g);
                    g.setColor(DeepNoteTheme.FAINT);
                    g.setFont(getFont());
                    g.drawString("Start writing your note…", getInsets().left,
                            getInsets().top + g.getFontMetrics().getAscent());
                    g.dispose();
                }
            };

            editor.setLineWrap(true);
            editor.setWrapStyleWord(true);
            editor.setTabSize(4);
            editor.setFont(DeepNoteTheme.EDITOR);
            editor.setForeground(DeepNoteTheme.TEXT);
            editor.setBackground(DeepNoteTheme.CANVAS);
            editor.setCaretColor(DeepNoteTheme.ACCENT_TEXT);
            editor.setBorder(null);

            assistantPanel = new AIAssistantPanel(assistantService, editor::getText, () -> toggleAssistant(false));
            assistantPanel.setNote(note.title());

            assistantHost.add(assistantPanel, BorderLayout.CENTER);
            assistantHost.setBorder(DeepNoteTheme.divider(0, 1, 0, 0));
            assistantHost.setVisible(false);

            JScrollPane documentScroll = buildDocument();

            // Resizable split pane between writing surface and AI panel
            editorSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, documentScroll, assistantHost);
            editorSplitPane.setDividerSize(0);
            editorSplitPane.setResizeWeight(1.0);
            editorSplitPane.setContinuousLayout(true);
            editorSplitPane.setBorder(null);
            editorSplitPane.setBackground(DeepNoteTheme.BORDER_SUBTLE);

            add(buildToolbar(), BorderLayout.NORTH);
            add(editorSplitPane, BorderLayout.CENTER);

            refreshHeader();
            refreshStatus();

            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { markDirty(); }
                @Override public void removeUpdate(DocumentEvent event) { markDirty(); }
                @Override public void changedUpdate(DocumentEvent event) { markDirty(); }
            });
        }

        private JPanel buildToolbar() {
            JPanel toolbar = new JPanel(new BorderLayout(12, 0));
            toolbar.setBackground(DeepNoteTheme.CANVAS);
            toolbar.setBorder(BorderFactory.createCompoundBorder(
                    DeepNoteTheme.divider(0, 0, 1, 0),
                    DeepNoteTheme.padding(8, 16, 8, 16)));

            breadcrumb.setFont(DeepNoteTheme.BODY_MEDIUM);
            breadcrumb.setForeground(DeepNoteTheme.TEXT_SECONDARY);

            statusLabel.setFont(DeepNoteTheme.SMALL);
            statusLabel.setForeground(DeepNoteTheme.MUTED);
            statusLabel.setIconTextGap(6);

            RoundedPanel statusCapsule = new RoundedPanel(new FlowLayout(FlowLayout.LEFT, 8, 2), 6,
                    DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER_SUBTLE);
            statusCapsule.add(statusLabel);

            toolbar.add(hbox(10, breadcrumb, statusCapsule), BorderLayout.WEST);

            // Right: Rename, Delete, Save, Ask AI
            ModernButton save = new ModernButton("Save", DeepNoteIcon.Kind.SAVE, ModernButton.Variant.SECONDARY);
            save.setToolTipText("Save (" + MOD_LABEL + "+S)");
            save.addActionListener(event -> save());

            ModernButton rename = ModernButton.iconOnly(DeepNoteIcon.Kind.EDIT, "Rename note",
                    ModernButton.Variant.GHOST, 16);
            rename.addActionListener(event -> rename());

            ModernButton delete = ModernButton.iconOnly(DeepNoteIcon.Kind.TRASH, "Delete note",
                    ModernButton.Variant.DANGER, 16);
            delete.addActionListener(event -> delete());

            askButton.setToolTipText("Ask AI about this note (" + MOD_LABEL + "+J)");
            askButton.addActionListener(event -> toggleAssistant(!isAssistantVisible()));

            toolbar.add(hbox(6, rename, delete, Box.createHorizontalStrut(6), save, askButton), BorderLayout.EAST);
            return toolbar;
        }

        private JScrollPane buildDocument() {
            JPanel titleBlock = new JPanel();
            titleBlock.setOpaque(false);
            titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));

            titleLabel.setFont(DeepNoteTheme.EDITOR_TITLE);
            titleLabel.setForeground(DeepNoteTheme.TEXT);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            metaLabel.setFont(DeepNoteTheme.SMALL);
            metaLabel.setForeground(DeepNoteTheme.MUTED);
            metaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            titleBlock.add(titleLabel);
            titleBlock.add(Box.createVerticalStrut(6));
            titleBlock.add(metaLabel);

            column.add(titleBlock, BorderLayout.NORTH);
            column.add(editor, BorderLayout.CENTER);

            JScrollPane scroll = new JScrollPane(column);
            scroll.setBorder(null);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scroll.getViewport().setBackground(DeepNoteTheme.CANVAS);
            scroll.getVerticalScrollBar().setUnitIncrement(18);

            scroll.getViewport().addComponentListener(new ComponentAdapter() {
                @Override public void componentResized(ComponentEvent event) {
                    int available = scroll.getViewport().getWidth();
                    int pad = Math.max(36, (available - READING_WIDTH) / 2);
                    column.setBorder(DeepNoteTheme.padding(32, pad, 90, pad));
                }
            });
            return scroll;
        }

        boolean isAssistantVisible() {
            return assistantHost.isVisible();
        }

        void toggleAssistant(boolean show) {
            assistantHost.setVisible(show);
            askButton.setActive(show);

            if (show) {
                editorSplitPane.setDividerSize(1);
                int desiredLocation = Math.max(300, getWidth() - lastAssistantWidth);
                editorSplitPane.setDividerLocation(desiredLocation);
                SwingUtilities.invokeLater(assistantPanel::focusInput);
            } else {
                lastAssistantWidth = Math.max(DEFAULT_ASSISTANT_WIDTH, getWidth() - editorSplitPane.getDividerLocation());
                editorSplitPane.setDividerSize(0);
            }

            revalidate();
            repaint();
        }

        private void refreshHeader() {
            titleLabel.setText(note.title());
            breadcrumb.setText("Notes / " + note.title());
            chip.setTitle(note.title());
            updateMeta();
        }

        private void updateMeta() {
            String text = editor.getText();
            int words = countWords(text);
            int minutes = Math.max(1, (int) Math.ceil(words / 220.0));
            metaLabel.setText(words + (words == 1 ? " word" : " words") + "  ·  "
                    + text.length() + " characters"
                    + (words > 0 ? "  ·  " + minutes + " min read" : ""));
        }

        private void refreshStatus() {
            statusLabel.setIcon(dot(dirty ? DeepNoteTheme.WARNING : DeepNoteTheme.SUCCESS, 7));
            statusLabel.setText(dirty ? "Unsaved changes" : "Saved");
            chip.setDirty(dirty);
        }

        private void markDirty() {
            updateMeta();
            if (dirty) return;
            dirty = true;
            refreshStatus();
        }

        boolean save() {
            try {
                note = noteService.saveNote(note, editor.getText());
                dirty = false;
                refreshStatus();
                reloadList();
                return true;
            } catch (NoteStorageException exception) {
                showError(exception.getMessage());
                return false;
            }
        }

        void rename() {
            String title = JOptionPane.showInputDialog(this, "New title:", note.title());
            if (title == null || title.trim().isEmpty()) return;
            try {
                Note old = note;
                note = noteService.renameNote(note, title.trim());
                openTabs.remove(old.path().toString());
                openTabs.put(note.path().toString(), this);
                assistantPanel.setNote(note.title());
                refreshHeader();
                reloadList();
            } catch (NoteStorageException exception) {
                showError(exception.getMessage());
            }
        }

        void delete() {
            if (JOptionPane.showConfirmDialog(this, "Delete “" + note.title() + "”? This cannot be undone.",
                    "Delete note", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try {
                noteService.deleteNote(note);
                removeTab(this);
                reloadList();
            } catch (NoteStorageException exception) {
                showError(exception.getMessage());
            }
        }
    }
}
