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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Main DeepNote workspace: sidebar, tabbed writing surface, and the optional AI panel. */
public class MainWindow extends JFrame {
    private static final int MOD = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    private static final String MOD_LABEL = MOD == InputEvent.META_DOWN_MASK ? "⌘" : "Ctrl";
    private static final int SIDEBAR_WIDTH = 288;
    private static final int ASSISTANT_WIDTH = 392;
    private static final int READING_WIDTH = 760;

    private final NoteService noteService;
    private final LocalSearchService searchService = new LocalSearchService();
    private final AIAssistantService assistantService;
    private final String chatModelName;
    private final DefaultListModel<Note> noteListModel = new DefaultListModel<>();
    private final JList<Note> noteList = new JList<>(noteListModel) {
        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (getModel().getSize() > 0) return;
            Graphics2D g = (Graphics2D) graphics.create();
            DeepNoteTheme.smooth(g);
            g.setFont(DeepNoteTheme.SMALL);
            g.setColor(DeepNoteTheme.FAINT);
            String message = searchField.getText().isBlank() ? "No notes yet" : "No matching notes";
            int width = g.getFontMetrics().stringWidth(message);
            g.drawString(message, (getWidth() - width) / 2, 40);
            g.dispose();
        }
    };
    private final JTextField searchField = new JTextField();
    private final JLabel noteCountLabel = new JLabel();
    private final JPanel contentCards = new JPanel(new CardLayout());
    private final JPanel tabStrip = new JPanel();
    private final JPanel editorStack = new JPanel(new CardLayout());
    private final JPanel homePanel = new JPanel(new GridBagLayout());
    private final List<EditorTab> tabOrder = new ArrayList<>();
    private final Map<String, EditorTab> openTabs = new HashMap<>();
    private JPanel navigation;
    private EditorTab selectedTab;
    private boolean syncingSelection;
    private int hoverIndex = -1;

    public MainWindow(AppConfig config) {
        super("DeepNote");
        chatModelName = config.chatModelName();
        noteService = new NoteService(config.notesDirectory());
        assistantService = new AIAssistantService(
                new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()),
                new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(),
                        Duration.ofSeconds(config.requestTimeoutSeconds())));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1000, 660));
        setSize(1320, 820);
        setLocationByPlatform(true);
        buildUi();
        reloadList();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { closeApplication(); }
        });
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        getContentPane().setBackground(DeepNoteTheme.CANVAS);
        getRootPane().putClientProperty("JRootPane.titleBarBackground", DeepNoteTheme.SIDEBAR);
        getRootPane().putClientProperty("JRootPane.titleBarForeground", DeepNoteTheme.MUTED);
        setLayout(new BorderLayout());
        navigation = buildNavigation();

        contentCards.setBackground(DeepNoteTheme.CANVAS);
        contentCards.add(buildWorkspace(), "workspace");
        contentCards.add(buildHome(), "home");
        showHome();

        add(navigation, BorderLayout.WEST);
        add(contentCards, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        noteList.addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || syncingSelection) return;
            Note chosen = noteList.getSelectedValue();
            if (chosen != null) openNote(noteService.readNote(chosen.path()));
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { reloadList(); }
            @Override public void removeUpdate(DocumentEvent event) { reloadList(); }
            @Override public void changedUpdate(DocumentEvent event) { reloadList(); }
        });
        installShortcuts();
    }

    private JPanel buildNavigation() {
        JPanel nav = new JPanel(new BorderLayout(0, 12));
        nav.setBackground(DeepNoteTheme.SIDEBAR);
        nav.setBorder(BorderFactory.createCompoundBorder(
                DeepNoteTheme.divider(0, 0, 0, 1), DeepNoteTheme.padding(14, 12, 12, 12)));
        nav.setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.setBorder(DeepNoteTheme.padding(0, 4, 0, 0));
        JLabel name = new JLabel("DeepNote");
        name.setFont(DeepNoteTheme.TITLE_SM);
        name.setForeground(DeepNoteTheme.TEXT);
        ModernButton hide = ModernButton.iconOnly(DeepNoteIcon.Kind.SIDEBAR_LEFT,
                "Hide sidebar (" + MOD_LABEL + "+B)", ModernButton.Variant.GHOST, 18);
        hide.addActionListener(event -> toggleSidebar());
        brand.add(new IconBadge(DeepNoteIcon.Kind.SPARK, 30, true), BorderLayout.WEST);
        brand.add(name, BorderLayout.CENTER);
        brand.add(hide, BorderLayout.EAST);
        c.gridy = 0;
        top.add(brand, c);

        ModernButton newNote = new ModernButton("New note", DeepNoteIcon.Kind.PLUS, ModernButton.Variant.PRIMARY);
        newNote.setToolTipText("Create a new TXT note (" + MOD_LABEL + "+N)");
        newNote.addActionListener(event -> createNote());
        c.gridy = 1;
        c.insets = new Insets(14, 0, 0, 0);
        top.add(newNote, c);

        searchField.putClientProperty("JTextField.placeholderText", "Search notes");
        searchField.putClientProperty("JTextField.leadingIcon",
                new DeepNoteIcon(DeepNoteIcon.Kind.SEARCH, DeepNoteTheme.MUTED, 16));
        searchField.putClientProperty("JTextField.showClearButton", true);
        searchField.setToolTipText("Search titles and content (" + MOD_LABEL + "+K)");
        c.gridy = 2;
        c.insets = new Insets(10, 0, 0, 0);
        top.add(searchField, c);
        nav.add(top, BorderLayout.NORTH);

        JPanel library = new JPanel(new BorderLayout(0, 6));
        library.setOpaque(false);
        JPanel libraryHeader = new JPanel(new BorderLayout());
        libraryHeader.setOpaque(false);
        libraryHeader.setBorder(DeepNoteTheme.padding(6, 6, 0, 6));
        JLabel libraryLabel = new JLabel("NOTES");
        libraryLabel.setFont(DeepNoteTheme.LABEL);
        libraryLabel.setForeground(DeepNoteTheme.FAINT);
        noteCountLabel.setFont(DeepNoteTheme.SMALL);
        noteCountLabel.setForeground(DeepNoteTheme.FAINT);
        libraryHeader.add(libraryLabel, BorderLayout.WEST);
        libraryHeader.add(noteCountLabel, BorderLayout.EAST);
        library.add(libraryHeader, BorderLayout.NORTH);

        noteList.setBackground(DeepNoteTheme.SIDEBAR);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.setFixedCellHeight(64);
        noteList.setBorder(null);
        noteList.setCellRenderer(new NoteCellRenderer());
        MouseAdapter hover = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) {
                int index = noteList.locationToIndex(event.getPoint());
                Rectangle bounds = index < 0 ? null : noteList.getCellBounds(index, index);
                setHoverIndex(bounds != null && bounds.contains(event.getPoint()) ? index : -1);
            }

            @Override public void mouseExited(MouseEvent event) { setHoverIndex(-1); }
        };
        noteList.addMouseMotionListener(hover);
        noteList.addMouseListener(hover);
        JScrollPane scroll = new JScrollPane(noteList);
        scroll.setBorder(null);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(DeepNoteTheme.SIDEBAR);
        scroll.setBackground(DeepNoteTheme.SIDEBAR);
        library.add(scroll, BorderLayout.CENTER);
        nav.add(library, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        footer.setOpaque(false);
        footer.setBorder(DeepNoteTheme.padding(4, 2, 0, 0));
        JLabel privacy = new JLabel("Stored locally as .txt files",
                new DeepNoteIcon(DeepNoteIcon.Kind.LOCK, DeepNoteTheme.FAINT, 14), SwingConstants.LEFT);
        privacy.setIconTextGap(6);
        privacy.setFont(DeepNoteTheme.SMALL);
        privacy.setForeground(DeepNoteTheme.FAINT);
        footer.add(privacy);
        nav.add(footer, BorderLayout.SOUTH);
        return nav;
    }

    private JPanel buildWorkspace() {
        JPanel workspace = new JPanel(new BorderLayout());
        workspace.setBackground(DeepNoteTheme.CANVAS);
        tabStrip.setOpaque(false);
        tabStrip.setLayout(new BoxLayout(tabStrip, BoxLayout.X_AXIS));
        tabStrip.setBorder(DeepNoteTheme.padding(8, 12, 8, 12));
        JScrollPane tabScroll = new JScrollPane(tabStrip, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tabScroll.setBorder(DeepNoteTheme.divider(0, 0, 1, 0));
        tabScroll.getViewport().setBackground(DeepNoteTheme.SIDEBAR);
        tabScroll.setBackground(DeepNoteTheme.SIDEBAR);
        tabScroll.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 8));
        editorStack.setBackground(DeepNoteTheme.CANVAS);
        workspace.add(tabScroll, BorderLayout.NORTH);
        workspace.add(editorStack, BorderLayout.CENTER);
        return workspace;
    }

    private JPanel buildHome() {
        homePanel.setBackground(DeepNoteTheme.CANVAS);
        return homePanel;
    }

    private void rebuildHome(List<Note> notes) {
        homePanel.removeAll();
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        IconBadge mark = new IconBadge(DeepNoteIcon.Kind.SPARK, 52, true);
        JLabel heading = new JLabel("Good ideas start as plain text.");
        heading.setFont(DeepNoteTheme.DISPLAY);
        heading.setForeground(DeepNoteTheme.TEXT);
        JLabel copy = new JLabel("<html><div style='width:480px'>Write freely in portable .txt files, then ask a "
                + "local AI about any note. Nothing ever leaves your machine.</div></html>");
        copy.setFont(DeepNoteTheme.CHAT);
        copy.setForeground(DeepNoteTheme.MUTED);
        ModernButton create = new ModernButton("New note", DeepNoteIcon.Kind.PLUS, ModernButton.Variant.PRIMARY);
        create.addActionListener(event -> createNote());
        JPanel hints = new JPanel();
        hints.setOpaque(false);
        hints.setLayout(new BoxLayout(hints, BoxLayout.X_AXIS));
        String[][] shortcuts = {{MOD_LABEL + " N", "New note"}, {MOD_LABEL + " K", "Search"},
                {MOD_LABEL + " J", "Ask AI"}, {MOD_LABEL + " B", "Sidebar"}};
        for (String[] shortcut : shortcuts) {
            hints.add(shortcutHint(shortcut[0], shortcut[1]));
            hints.add(Box.createHorizontalStrut(18));
        }
        for (JComponent component : new JComponent[]{mark, heading, copy, create, hints}) {
            component.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        column.add(mark);
        column.add(Box.createVerticalStrut(22));
        column.add(heading);
        column.add(Box.createVerticalStrut(12));
        column.add(copy);
        column.add(Box.createVerticalStrut(26));
        column.add(create);
        column.add(Box.createVerticalStrut(18));
        column.add(hints);

        if (!notes.isEmpty()) {
            JLabel recent = new JLabel("RECENT NOTES");
            recent.setFont(DeepNoteTheme.LABEL);
            recent.setForeground(DeepNoteTheme.FAINT);
            recent.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel grid = new JPanel(new GridLayout(0, 2, 12, 12));
            grid.setOpaque(false);
            grid.setAlignmentX(Component.LEFT_ALIGNMENT);
            notes.stream().sorted(Comparator.comparingLong((Note note) -> lastModified(note.path())).reversed())
                    .limit(4).forEach(note -> grid.add(recentCard(note)));
            column.add(Box.createVerticalStrut(44));
            column.add(recent);
            column.add(Box.createVerticalStrut(12));
            column.add(grid);
        }
        homePanel.add(column);
        homePanel.revalidate();
        homePanel.repaint();
    }

    private JComponent shortcutHint(String keys, String label) {
        JPanel hint = new JPanel();
        hint.setOpaque(false);
        hint.setLayout(new BoxLayout(hint, BoxLayout.X_AXIS));
        RoundedPanel keycap = new RoundedPanel(new BorderLayout(), 8, DeepNoteTheme.RAISED, DeepNoteTheme.BORDER);
        keycap.setBorder(DeepNoteTheme.padding(3, 7, 3, 7));
        JLabel keyText = new JLabel(keys);
        keyText.setFont(DeepNoteTheme.SMALL_MEDIUM);
        keyText.setForeground(DeepNoteTheme.TEXT_SECONDARY);
        keycap.add(keyText);
        keycap.setMaximumSize(keycap.getPreferredSize());
        JLabel text = new JLabel(label);
        text.setFont(DeepNoteTheme.SMALL);
        text.setForeground(DeepNoteTheme.MUTED);
        hint.add(keycap);
        hint.add(Box.createHorizontalStrut(8));
        hint.add(text);
        return hint;
    }

    private JComponent recentCard(Note note) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), 16, DeepNoteTheme.RAISED,
                DeepNoteTheme.BORDER_SOFT);
        card.setBorder(DeepNoteTheme.padding(16, 18, 16, 18));
        card.setPreferredSize(new Dimension(300, 84));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JLabel title = new JLabel(note.title());
        title.setFont(DeepNoteTheme.BODY_MEDIUM);
        title.setForeground(DeepNoteTheme.TEXT);
        JLabel preview = new JLabel(previewOf(note.content()));
        preview.setFont(DeepNoteTheme.SMALL);
        preview.setForeground(DeepNoteTheme.MUTED);
        card.add(title, BorderLayout.NORTH);
        card.add(preview, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) { card.setStroke(DeepNoteTheme.ACCENT); }

            @Override public void mouseExited(MouseEvent event) {
                if (!card.contains(event.getPoint())) card.setStroke(DeepNoteTheme.BORDER_SOFT);
            }

            @Override public void mouseClicked(MouseEvent event) { openNote(noteService.readNote(note.path())); }
        });
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(DeepNoteTheme.SIDEBAR);
        status.setBorder(BorderFactory.createCompoundBorder(
                DeepNoteTheme.divider(1, 0, 0, 0), DeepNoteTheme.padding(6, 16, 6, 16)));
        JLabel left = new JLabel("Local workspace", dot(DeepNoteTheme.SUCCESS, 8), SwingConstants.LEFT);
        left.setIconTextGap(8);
        left.setFont(DeepNoteTheme.SMALL);
        left.setForeground(DeepNoteTheme.MUTED);
        JLabel right = new JLabel("Ollama · " + chatModelName);
        right.setFont(DeepNoteTheme.SMALL);
        right.setForeground(DeepNoteTheme.FAINT);
        status.add(left, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);
        return status;
    }

    // ---------------------------------------------------------------- shortcuts

    private void installShortcuts() {
        bind(KeyEvent.VK_N, "newNote", this::createNote);
        bind(KeyEvent.VK_S, "saveNote", this::saveCurrent);
        bind(KeyEvent.VK_K, "focusSearch", this::focusSearch);
        bind(KeyEvent.VK_J, "toggleAssistant", this::toggleCurrentAssistant);
        bind(KeyEvent.VK_W, "closeTab", () -> { if (selectedTab != null) closeTab(selectedTab); });
        bind(KeyEvent.VK_B, "toggleSidebar", this::toggleSidebar);
    }

    private void bind(int keyCode, String name, Runnable action) {
        getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(keyCode, MOD), name);
        getRootPane().getActionMap().put(name, new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { action.run(); }
        });
    }

    private void toggleSidebar() {
        navigation.setVisible(!navigation.isVisible());
        getContentPane().revalidate();
        getContentPane().repaint();
    }

    private void focusSearch() {
        if (!navigation.isVisible()) toggleSidebar();
        searchField.requestFocusInWindow();
        searchField.selectAll();
    }

    private void saveCurrent() { if (selectedTab != null) selectedTab.save(); }

    private void toggleCurrentAssistant() {
        if (selectedTab != null) selectedTab.toggleAssistant(!selectedTab.assistantHost.isVisible());
    }

    // ---------------------------------------------------------------- note list

    private void reloadList() {
        List<Note> all = noteService.listNotes();
        String query = searchField.getText();
        List<Note> shown = searchService.search(all, query);
        syncingSelection = true;
        try {
            noteListModel.clear();
            shown.forEach(noteListModel::addElement);
        } finally {
            syncingSelection = false;
        }
        boolean filtering = query != null && !query.isBlank();
        noteCountLabel.setText(shown.size() + (filtering ? (shown.size() == 1 ? " result" : " results")
                : (shown.size() == 1 ? " note" : " notes")));
        syncListSelection();
        rebuildHome(all);
    }

    private void syncListSelection() {
        syncingSelection = true;
        try {
            noteList.clearSelection();
            if (selectedTab == null) return;
            for (int i = 0; i < noteListModel.size(); i++) {
                if (noteListModel.get(i).path().equals(selectedTab.note.path())) {
                    noteList.setSelectedIndex(i);
                    noteList.ensureIndexIsVisible(i);
                    return;
                }
            }
        } finally {
            syncingSelection = false;
        }
    }

    private void setHoverIndex(int index) {
        if (index == hoverIndex) return;
        hoverIndex = index;
        noteList.repaint();
    }

    // ---------------------------------------------------------------- tabs

    private void showHome() {
        navigation.setVisible(true);
        ((CardLayout) contentCards.getLayout()).show(contentCards, "home");
        syncListSelection();
    }

    private void showWorkspace() { ((CardLayout) contentCards.getLayout()).show(contentCards, "workspace"); }

    private void createNote() {
        String title = JOptionPane.showInputDialog(this, "Give your note a title:", "New note",
                JOptionPane.PLAIN_MESSAGE);
        if (title == null) return;
        try {
            Note note = noteService.createNote(title);
            reloadList();
            openNote(note);
        } catch (NoteStorageException exception) { showError(exception.getMessage()); }
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
        syncListSelection();
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
        if (tabOrder.isEmpty()) showHome();
        else selectTab(tabOrder.get(Math.min(index, tabOrder.size() - 1)));
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

    // ---------------------------------------------------------------- helpers

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

    private static String previewOf(String content) {
        for (String line : content.split("\\R", 40)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            return trimmed.length() > 58 ? trimmed.substring(0, 58) + "…" : trimmed;
        }
        return "Empty note";
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            return 0L; // A file that cannot be inspected simply sorts last.
        }
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

    // ---------------------------------------------------------------- note list cell

    private final class NoteCellRenderer extends JPanel implements ListCellRenderer<Note> {
        private final JLabel title = new JLabel();
        private final JLabel preview = new JLabel();
        private boolean selected;
        private boolean hovered;

        NoteCellRenderer() {
            setOpaque(false);
            setLayout(new BorderLayout(0, 3));
            setBorder(DeepNoteTheme.padding(12, 14, 12, 14));
            title.setFont(DeepNoteTheme.BODY_MEDIUM);
            preview.setFont(DeepNoteTheme.SMALL);
            add(title, BorderLayout.CENTER);
            add(preview, BorderLayout.SOUTH);
        }

        @Override public Component getListCellRendererComponent(JList<? extends Note> list, Note note, int index,
                                                                boolean isSelected, boolean focused) {
            selected = isSelected;
            hovered = index == hoverIndex;
            title.setText(note.title());
            title.setForeground(DeepNoteTheme.TEXT);
            preview.setText(previewOf(note.content()));
            preview.setForeground(isSelected ? DeepNoteTheme.ACCENT_TEXT : DeepNoteTheme.MUTED);
            return this;
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            DeepNoteTheme.smooth(g);
            if (selected || hovered) {
                g.setColor(selected ? DeepNoteTheme.ACCENT_SOFT : DeepNoteTheme.RAISED);
                g.fillRoundRect(0, 2, getWidth(), getHeight() - 4, 14, 14);
            }
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    // ---------------------------------------------------------------- tab chip

    private final class TabChip extends RoundedPanel {
        private final EditorTab tab;
        private final JLabel title = new JLabel();
        private final JLabel dirtyDot = new JLabel(dot(DeepNoteTheme.WARNING, 7));
        private boolean selected;
        private boolean hovered;

        TabChip(EditorTab tab) {
            super(new BorderLayout(8, 0), 12, null, null);
            this.tab = tab;
            setBorder(DeepNoteTheme.padding(5, 12, 5, 6));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            title.setFont(DeepNoteTheme.BODY_MEDIUM);
            dirtyDot.setVisible(false);
            ModernButton close = ModernButton.iconOnly(DeepNoteIcon.Kind.CLOSE, "Close (" + MOD_LABEL + "+W)",
                    ModernButton.Variant.GHOST, 12);
            close.addActionListener(event -> closeTab(tab));
            JLabel document = new JLabel(new DeepNoteIcon(DeepNoteIcon.Kind.DOCUMENT, DeepNoteTheme.MUTED, 15));
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
            setFill(selected ? DeepNoteTheme.RAISED : hovered ? DeepNoteTheme.HOVER : null);
            setStroke(selected ? DeepNoteTheme.BORDER : null);
            title.setForeground(selected ? DeepNoteTheme.TEXT : DeepNoteTheme.MUTED);
        }
    }

    // ---------------------------------------------------------------- editor tab

    private final class EditorTab extends JPanel {
        final String id = UUID.randomUUID().toString();
        final JTextArea editor;
        final JPanel assistantHost = new JPanel(new BorderLayout());
        final AIAssistantPanel assistantPanel;
        final TabChip chip;
        final JPanel chipHolder = new JPanel(new BorderLayout()) {
            @Override public Dimension getMaximumSize() { return getPreferredSize(); }
        };
        private final ScrollableColumn column = new ScrollableColumn(new BorderLayout(0, 22), true);
        private final JLabel titleLabel = new JLabel();
        private final JLabel metaLabel = new JLabel();
        private final JLabel statusLabel = new JLabel();
        private final ModernButton askButton = new ModernButton("Ask AI", DeepNoteIcon.Kind.SPARK,
                ModernButton.Variant.SOFT);
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
                    g.drawString("Start writing…", getInsets().left, getInsets().top + g.getFontMetrics().getAscent());
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
            assistantHost.setPreferredSize(new Dimension(ASSISTANT_WIDTH, 0));
            assistantHost.setBorder(DeepNoteTheme.divider(0, 1, 0, 0));
            assistantHost.setVisible(false);

            add(buildToolbar(), BorderLayout.NORTH);
            add(buildDocument(), BorderLayout.CENTER);
            add(assistantHost, BorderLayout.EAST);
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
            toolbar.setBorder(DeepNoteTheme.padding(10, 14, 10, 16));

            ModernButton sidebar = ModernButton.iconOnly(DeepNoteIcon.Kind.SIDEBAR_LEFT,
                    "Toggle sidebar (" + MOD_LABEL + "+B)", ModernButton.Variant.GHOST, 18);
            sidebar.addActionListener(event -> toggleSidebar());
            statusLabel.setFont(DeepNoteTheme.SMALL);
            statusLabel.setForeground(DeepNoteTheme.MUTED);
            statusLabel.setIconTextGap(8);
            toolbar.add(hbox(10, sidebar, statusLabel), BorderLayout.WEST);

            ModernButton save = new ModernButton("Save", DeepNoteIcon.Kind.SAVE, ModernButton.Variant.SECONDARY);
            save.setToolTipText("Save (" + MOD_LABEL + "+S)");
            ModernButton rename = ModernButton.iconOnly(DeepNoteIcon.Kind.EDIT, "Rename note",
                    ModernButton.Variant.GHOST, 18);
            ModernButton delete = ModernButton.iconOnly(DeepNoteIcon.Kind.TRASH, "Delete note",
                    ModernButton.Variant.DANGER, 18);
            askButton.setToolTipText("Ask AI about this note (" + MOD_LABEL + "+J)");
            save.addActionListener(event -> save());
            rename.addActionListener(event -> rename());
            delete.addActionListener(event -> delete());
            askButton.addActionListener(event -> toggleAssistant(!assistantHost.isVisible()));
            toolbar.add(hbox(6, rename, delete, Box.createHorizontalStrut(4), save, askButton), BorderLayout.EAST);
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
            titleBlock.add(Box.createVerticalStrut(8));
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
                    int pad = Math.max(32, (scroll.getViewport().getWidth() - READING_WIDTH) / 2);
                    column.setBorder(DeepNoteTheme.padding(30, pad, 80, pad));
                }
            });
            return scroll;
        }

        void toggleAssistant(boolean show) {
            assistantHost.setVisible(show);
            askButton.setActive(show);
            revalidate();
            repaint();
            if (show) SwingUtilities.invokeLater(assistantPanel::focusInput);
        }

        private void refreshHeader() {
            titleLabel.setText(note.title());
            chip.setTitle(note.title());
            updateMeta();
        }

        private void updateMeta() {
            String text = editor.getText();
            int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
            int minutes = Math.max(1, (int) Math.ceil(words / 220.0));
            metaLabel.setText(words + (words == 1 ? " word" : " words") + "  ·  " + text.length() + " characters"
                    + (words > 0 ? "  ·  " + minutes + " min read" : ""));
        }

        private void refreshStatus() {
            statusLabel.setIcon(dot(dirty ? DeepNoteTheme.WARNING : DeepNoteTheme.SUCCESS, 8));
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
            if (title == null) return;
            try {
                Note old = note;
                note = noteService.renameNote(note, title);
                openTabs.remove(old.path().toString());
                openTabs.put(note.path().toString(), this);
                assistantPanel.setNote(note.title());
                refreshHeader();
                reloadList();
            } catch (NoteStorageException exception) { showError(exception.getMessage()); }
        }

        void delete() {
            if (JOptionPane.showConfirmDialog(this, "Delete “" + note.title() + "”? This cannot be undone.",
                    "Delete note", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try {
                noteService.deleteNote(note);
                removeTab(this);
                reloadList();
            } catch (NoteStorageException exception) { showError(exception.getMessage()); }
        }
    }
}
