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
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Main DeepNote workspace and desktop shell. */
public class MainWindow extends JFrame {
    private final NoteService noteService;
    private final LocalSearchService searchService = new LocalSearchService();
    private final AIAssistantService assistantService;
    private final DefaultListModel<Note> noteListModel = new DefaultListModel<>();
    private final JList<Note> noteList = new JList<>(noteListModel);
    private final JTabbedPane tabs = new JTabbedPane();
    private final Map<String, EditorTab> openTabs = new HashMap<>();
    private final JTextField searchField = new JTextField();
    private final JPanel workspaceArea = new JPanel(new CardLayout());
    private final JPanel homePanel = new JPanel(new BorderLayout());
    private final JLabel noteCountLabel = new JLabel();

    public MainWindow(AppConfig config) {
        super("DeepNote");
        noteService = new NoteService(config.notesDirectory());
        assistantService = new AIAssistantService(
                new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()),
                new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(), Duration.ofSeconds(config.requestTimeoutSeconds())));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(980, 640));
        setSize(1280, 800);
        setLocationByPlatform(true);
        buildUi();
        refreshNotes();
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent event) { closeApplication(); }
        });
    }

    private void buildUi() {
        getContentPane().setBackground(DeepNoteTheme.BACKGROUND);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        JPanel navigation = buildNavigation();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.setBorder(null);
        tabs.setOpaque(true);
        tabs.setBackground(DeepNoteTheme.SURFACE);
        workspaceArea.add(buildHomePanel(), "home");
        workspaceArea.add(tabs, "tabs");
        showHome();

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, navigation, workspaceArea);
        mainSplit.setDividerLocation(270);
        mainSplit.setResizeWeight(0);
        mainSplit.setBorder(null);
        mainSplit.setContinuousLayout(true);
        add(mainSplit, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        noteList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && noteList.getSelectedValue() != null) openNote(noteList.getSelectedValue());
        });
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent event) { filterNotes(); }
            @Override public void removeUpdate(DocumentEvent event) { filterNotes(); }
            @Override public void changedUpdate(DocumentEvent event) { filterNotes(); }
        });
        tabs.addChangeListener(event -> updateSelectionFromTab());
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(DeepNoteTheme.SURFACE);
        header.setBorder(BorderFactory.createCompoundBorder(DeepNoteTheme.divider(0, 0, 1, 0), DeepNoteTheme.padding(12, 18, 12, 18)));
        JPanel identity = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        identity.setOpaque(false);
        JLabel mark = new JLabel("D", SwingConstants.CENTER);
        mark.setOpaque(true);
        mark.setBackground(DeepNoteTheme.ACCENT);
        mark.setForeground(Color.WHITE);
        mark.setFont(new Font("Segoe UI", Font.BOLD, 15));
        mark.setPreferredSize(new Dimension(30, 30));
        JLabel title = new JLabel("DeepNote");
        title.setFont(DeepNoteTheme.TITLE);
        JLabel subtitle = new JLabel("Private notes, local intelligence");
        subtitle.setForeground(DeepNoteTheme.MUTED);
        subtitle.setFont(DeepNoteTheme.BODY);
        JPanel copy = new JPanel();
        copy.setOpaque(false);
        copy.setLayout(new BoxLayout(copy, BoxLayout.Y_AXIS));
        copy.add(title);
        copy.add(subtitle);
        identity.add(mark);
        identity.add(copy);
        header.add(identity, BorderLayout.WEST);
        JButton newNote = actionButton("New note", new DeepNoteIcon(DeepNoteIcon.Kind.PLUS, Color.WHITE), ButtonStyle.PRIMARY);
        newNote.setToolTipText("Create a new TXT note");
        newNote.addActionListener(event -> createNote());
        header.add(newNote, BorderLayout.EAST);
        return header;
    }

    private JPanel buildNavigation() {
        JPanel navigation = new JPanel(new BorderLayout(0, 14));
        navigation.setBackground(DeepNoteTheme.SURFACE);
        navigation.setBorder(BorderFactory.createCompoundBorder(DeepNoteTheme.divider(0, 0, 0, 1), DeepNoteTheme.padding(18, 16, 16, 16)));
        JPanel searchBlock = new JPanel(new BorderLayout(0, 10));
        searchBlock.setOpaque(false);
        searchField.putClientProperty("JTextField.placeholderText", "Search notes");
        searchField.putClientProperty("JTextField.leadingIcon", new DeepNoteIcon(DeepNoteIcon.Kind.SEARCH, DeepNoteTheme.MUTED));
        searchField.setToolTipText("Search note titles and content");
        searchBlock.add(searchField, BorderLayout.NORTH);
        JPanel libraryHeader = new JPanel(new BorderLayout());
        libraryHeader.setOpaque(false);
        JLabel libraryLabel = new JLabel("YOUR NOTES");
        libraryLabel.setFont(DeepNoteTheme.LABEL);
        libraryLabel.setForeground(DeepNoteTheme.MUTED);
        noteCountLabel.setFont(DeepNoteTheme.LABEL);
        noteCountLabel.setForeground(DeepNoteTheme.MUTED);
        libraryHeader.add(libraryLabel, BorderLayout.WEST);
        libraryHeader.add(noteCountLabel, BorderLayout.EAST);
        searchBlock.add(libraryHeader, BorderLayout.SOUTH);
        navigation.add(searchBlock, BorderLayout.NORTH);
        noteList.setBackground(DeepNoteTheme.SURFACE);
        noteList.setForeground(DeepNoteTheme.TEXT);
        noteList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        noteList.setFixedCellHeight(58);
        noteList.setBorder(null);
        noteList.setCellRenderer(new NoteCellRenderer());
        JScrollPane notesScroll = new JScrollPane(noteList);
        notesScroll.setBorder(null);
        notesScroll.getViewport().setBackground(DeepNoteTheme.SURFACE);
        navigation.add(notesScroll, BorderLayout.CENTER);
        JPanel navigationFooter = new JPanel(new BorderLayout(0, 8));
        navigationFooter.setOpaque(false);
        JLabel hint = new JLabel("Keyword search · TXT source of truth");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(DeepNoteTheme.MUTED);
        navigationFooter.add(hint, BorderLayout.NORTH);
        JButton footerNew = actionButton("New note", new DeepNoteIcon(DeepNoteIcon.Kind.PLUS, DeepNoteTheme.ACCENT_HOVER), ButtonStyle.GHOST);
        footerNew.setHorizontalAlignment(SwingConstants.LEFT);
        footerNew.addActionListener(event -> createNote());
        navigationFooter.add(footerNew, BorderLayout.SOUTH);
        navigation.add(navigationFooter, BorderLayout.SOUTH);
        navigation.setPreferredSize(new Dimension(270, 0));
        return navigation;
    }

    private JPanel buildHomePanel() {
        homePanel.setBackground(DeepNoteTheme.BACKGROUND);
        homePanel.setBorder(DeepNoteTheme.padding(50, 64, 50, 64));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        JLabel eyebrow = new JLabel("YOUR PRIVATE WORKSPACE");
        eyebrow.setForeground(DeepNoteTheme.ACCENT_HOVER);
        eyebrow.setFont(DeepNoteTheme.LABEL);
        JLabel heading = new JLabel("Make space for better thinking.");
        heading.setForeground(DeepNoteTheme.TEXT);
        heading.setFont(DeepNoteTheme.DISPLAY);
        JLabel description = new JLabel("Create a plain-text note, keep it portable, and ask your local assistant when you need a second perspective.");
        description.setForeground(DeepNoteTheme.MUTED);
        description.setFont(DeepNoteTheme.BODY_LARGE);
        description.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(eyebrow);
        content.add(Box.createVerticalStrut(10));
        content.add(heading);
        content.add(Box.createVerticalStrut(12));
        content.add(description);
        content.add(Box.createVerticalStrut(28));
        JButton create = actionButton("Create your first note", new DeepNoteIcon(DeepNoteIcon.Kind.PLUS, Color.WHITE), ButtonStyle.PRIMARY);
        create.setAlignmentX(Component.LEFT_ALIGNMENT);
        create.addActionListener(event -> createNote());
        content.add(create);
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.NORTH);
        homePanel.add(wrapper, BorderLayout.NORTH);
        return homePanel;
    }

    private JPanel buildStatusBar() {
        JPanel status = new JPanel(new BorderLayout());
        status.setBackground(DeepNoteTheme.BACKGROUND);
        status.setBorder(DeepNoteTheme.padding(8, 18, 8, 18));
        JLabel left = new JLabel("LOCAL WORKSPACE");
        left.setFont(DeepNoteTheme.LABEL);
        left.setForeground(DeepNoteTheme.MUTED);
        JLabel right = new JLabel("Ollama assistant · offline by default");
        right.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        right.setForeground(DeepNoteTheme.MUTED);
        status.add(left, BorderLayout.WEST);
        status.add(right, BorderLayout.EAST);
        return status;
    }

    private void refreshNotes() {
        List<Note> notes = noteService.listNotes();
        noteListModel.clear();
        notes.forEach(noteListModel::addElement);
        noteCountLabel.setText(notes.size() + (notes.size() == 1 ? " note" : " notes"));
        if (tabs.getTabCount() == 0) showHome();
    }

    private void filterNotes() {
        List<Note> results = searchService.search(noteService.listNotes(), searchField.getText());
        noteListModel.clear();
        results.forEach(noteListModel::addElement);
        noteCountLabel.setText(results.size() + (results.size() == 1 ? " result" : " results"));
    }

    private void showHome() { ((CardLayout) workspaceArea.getLayout()).show(workspaceArea, "home"); }
    private void showTabs() { ((CardLayout) workspaceArea.getLayout()).show(workspaceArea, "tabs"); }

    private void createNote() {
        String title = JOptionPane.showInputDialog(this, "Give your note a title:", "New note", JOptionPane.PLAIN_MESSAGE);
        if (title == null) return;
        try {
            Note note = noteService.createNote(title);
            refreshNotes();
            openNote(note);
        } catch (NoteStorageException exception) { showError(exception.getMessage()); }
    }

    private void openNote(Note note) {
        EditorTab existing = openTabs.get(note.path().toString());
        if (existing != null) { tabs.setSelectedComponent(existing); showTabs(); return; }
        EditorTab editorTab = new EditorTab(note);
        openTabs.put(note.path().toString(), editorTab);
        tabs.addTab(note.title(), editorTab);
        tabs.setTabComponentAt(tabs.indexOfComponent(editorTab), tabHeader(editorTab));
        tabs.setSelectedComponent(editorTab);
        showTabs();
    }

    private JPanel tabHeader(EditorTab editorTab) {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        header.setOpaque(false);
        editorTab.tabTitle = new JLabel(editorTab.note.title());
        editorTab.tabTitle.setForeground(DeepNoteTheme.TEXT);
        editorTab.tabTitle.setFont(DeepNoteTheme.BODY);
        JLabel document = new JLabel(new DeepNoteIcon(DeepNoteIcon.Kind.DOCUMENT, DeepNoteTheme.MUTED));
        JButton close = iconButton(new DeepNoteIcon(DeepNoteIcon.Kind.CLOSE, DeepNoteTheme.MUTED), "Close note");
        close.addActionListener(event -> closeTab(editorTab));
        header.add(document);
        header.add(editorTab.tabTitle);
        header.add(close);
        return header;
    }

    private void closeTab(EditorTab tab) {
        if (tab.isDirty()) {
            int choice = JOptionPane.showConfirmDialog(this, "Save changes to “" + tab.note.title() + "” before closing?", "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION) return;
            if (choice == JOptionPane.YES_OPTION && !tab.save()) return;
        }
        openTabs.remove(tab.note.path().toString());
        tabs.remove(tab);
        if (tabs.getTabCount() == 0) showHome();
    }

    private void updateSelectionFromTab() {
        Component component = tabs.getSelectedComponent();
        if (component instanceof EditorTab tab) noteList.setSelectedValue(noteService.readNote(tab.note.path()), true);
    }

    private void closeApplication() {
        for (EditorTab tab : List.copyOf(openTabs.values())) {
            if (!tab.isDirty()) continue;
            int choice = JOptionPane.showConfirmDialog(this, "Save changes to “" + tab.note.title() + "” before closing?", "Unsaved changes", JOptionPane.YES_NO_CANCEL_OPTION);
            if (choice == JOptionPane.CANCEL_OPTION) return;
            if (choice == JOptionPane.YES_OPTION && !tab.save()) return;
        }
        dispose();
    }

    private void showError(String message) { JOptionPane.showMessageDialog(this, message, "DeepNote", JOptionPane.ERROR_MESSAGE); }

    private JButton actionButton(String text, Icon icon, ButtonStyle style) {
        JButton button = new JButton(text, icon);
        button.setFont(DeepNoteTheme.BODY);
        button.setIconTextGap(8);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        button.setMargin(new Insets(0, 0, 0, 0));
        button.putClientProperty("JButton.buttonType", "roundRect");
        if (style == ButtonStyle.PRIMARY) {
            button.setBackground(DeepNoteTheme.ACCENT);
            button.setForeground(Color.WHITE);
        } else if (style == ButtonStyle.DANGER) {
            button.setBackground(DeepNoteTheme.SURFACE);
            button.setForeground(DeepNoteTheme.DANGER);
        } else {
            button.setBackground(DeepNoteTheme.ELEVATED);
            button.setForeground(DeepNoteTheme.TEXT);
        }
        return button;
    }

    private JButton iconButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        button.setContentAreaFilled(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private enum ButtonStyle { PRIMARY, GHOST, DANGER }

    private static final class NoteCellRenderer extends JPanel implements ListCellRenderer<Note> {
        private final JLabel title = new JLabel();
        private final JLabel metadata = new JLabel("TXT note");

        NoteCellRenderer() {
            setLayout(new BorderLayout(0, 3));
            setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            title.setFont(DeepNoteTheme.BODY);
            metadata.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            add(title, BorderLayout.CENTER);
            add(metadata, BorderLayout.SOUTH);
        }

        @Override public Component getListCellRendererComponent(JList<? extends Note> list, Note note, int index, boolean selected, boolean focused) {
            setOpaque(true);
            setBackground(selected ? Color.decode("#292849") : DeepNoteTheme.SURFACE);
            title.setText(note.title());
            title.setForeground(selected ? Color.WHITE : DeepNoteTheme.TEXT);
            metadata.setForeground(selected ? Color.decode("#C5C3FF") : DeepNoteTheme.MUTED);
            return this;
        }
    }

    private final class EditorTab extends JPanel {
        private Note note;
        private final JTextArea editor = new JTextArea();
        private final JPanel assistantHost = new JPanel(new BorderLayout());
        private final AIAssistantPanel assistantPanel;
        private JLabel tabTitle;
        private boolean dirty;

        EditorTab(Note note) {
            this.note = note;
            setLayout(new BorderLayout());
            setBackground(DeepNoteTheme.BACKGROUND);
            editor.setText(note.content());
            editor.setLineWrap(true);
            editor.setWrapStyleWord(true);
            editor.setFont(DeepNoteTheme.BODY_LARGE);
            editor.setForeground(DeepNoteTheme.TEXT);
            editor.setBackground(DeepNoteTheme.BACKGROUND);
            editor.setCaretColor(DeepNoteTheme.ACCENT_HOVER);
            editor.setSelectionColor(Color.decode("#3A3B70"));
            editor.setSelectedTextColor(DeepNoteTheme.TEXT);
            editor.setMargin(new Insets(26, 34, 40, 34));
            assistantPanel = new AIAssistantPanel(assistantService, editor::getText, () -> toggleAssistant(false));
            assistantPanel.setNote(note.title());
            assistantHost.add(assistantPanel, BorderLayout.CENTER);
            assistantHost.setPreferredSize(new Dimension(360, 0));
            assistantHost.setVisible(false);
            assistantHost.setBorder(DeepNoteTheme.divider(0, 1, 0, 0));

            JPanel toolbar = new JPanel(new BorderLayout(12, 0));
            toolbar.setBackground(DeepNoteTheme.SURFACE);
            toolbar.setBorder(BorderFactory.createCompoundBorder(DeepNoteTheme.divider(0, 0, 1, 0), DeepNoteTheme.padding(10, 18, 10, 18)));
            JPanel descriptor = new JPanel();
            descriptor.setOpaque(false);
            descriptor.setLayout(new BoxLayout(descriptor, BoxLayout.Y_AXIS));
            JLabel noteType = new JLabel("PLAIN TEXT NOTE");
            noteType.setFont(DeepNoteTheme.LABEL);
            noteType.setForeground(DeepNoteTheme.MUTED);
            JLabel notePath = new JLabel(note.title());
            notePath.setFont(DeepNoteTheme.BODY);
            notePath.setForeground(DeepNoteTheme.TEXT);
            descriptor.add(noteType);
            descriptor.add(notePath);
            toolbar.add(descriptor, BorderLayout.WEST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            JButton save = actionButton("Save", new DeepNoteIcon(DeepNoteIcon.Kind.SAVE, DeepNoteTheme.TEXT), ButtonStyle.GHOST);
            JButton rename = actionButton("Rename", new DeepNoteIcon(DeepNoteIcon.Kind.EDIT, DeepNoteTheme.TEXT), ButtonStyle.GHOST);
            JButton delete = actionButton("Delete", new DeepNoteIcon(DeepNoteIcon.Kind.TRASH, DeepNoteTheme.DANGER), ButtonStyle.DANGER);
            JButton ask = actionButton("Ask AI", new DeepNoteIcon(DeepNoteIcon.Kind.SPARK, Color.WHITE), ButtonStyle.PRIMARY);
            save.addActionListener(event -> save());
            rename.addActionListener(event -> rename());
            delete.addActionListener(event -> delete());
            ask.addActionListener(event -> toggleAssistant(!assistantHost.isVisible()));
            actions.add(save); actions.add(rename); actions.add(delete); actions.add(ask);
            toolbar.add(actions, BorderLayout.EAST);
            JScrollPane editorScroll = new JScrollPane(editor);
            editorScroll.setBorder(null);
            editorScroll.getViewport().setBackground(DeepNoteTheme.BACKGROUND);
            add(toolbar, BorderLayout.NORTH);
            add(editorScroll, BorderLayout.CENTER);
            add(assistantHost, BorderLayout.EAST);
            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent event) { markDirty(); }
                @Override public void removeUpdate(DocumentEvent event) { markDirty(); }
                @Override public void changedUpdate(DocumentEvent event) { markDirty(); }
            });
        }

        private void toggleAssistant(boolean show) {
            assistantHost.setVisible(show);
            revalidate();
            repaint();
        }

        boolean isDirty() { return dirty; }
        private void markDirty() { dirty = true; updateTabTitle(); }
        private void updateTabTitle() { if (tabTitle != null) tabTitle.setText(note.title() + (dirty ? " *" : "")); }
        boolean save() {
            try { note = noteService.saveNote(note, editor.getText()); dirty = false; updateTabTitle(); refreshNotes(); return true; }
            catch (NoteStorageException exception) { showError(exception.getMessage()); return false; }
        }
        void rename() {
            String title = JOptionPane.showInputDialog(this, "New title:", note.title());
            if (title == null) return;
            try {
                Note old = note;
                note = noteService.renameNote(note, title);
                openTabs.remove(old.path().toString());
                openTabs.put(note.path().toString(), this);
                updateTabTitle();
                assistantPanel.setNote(note.title());
                refreshNotes();
            } catch (NoteStorageException exception) { showError(exception.getMessage()); }
        }
        void delete() {
            if (JOptionPane.showConfirmDialog(this, "Delete “" + note.title() + "”?", "Delete note", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try { noteService.deleteNote(note); openTabs.remove(note.path().toString()); tabs.remove(this); refreshNotes(); }
            catch (NoteStorageException exception) { showError(exception.getMessage()); }
        }
    }
}
