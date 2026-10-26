package com.deepnote.fx;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.ai.ContextManager;
import com.deepnote.ai.OllamaChatModelClient;
import com.deepnote.config.AppConfig;
import com.deepnote.model.ChatMessage;
import com.deepnote.model.Note;
import com.deepnote.service.NoteService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Workspace shell: single-row tab strip, active view host and live bottom status bar. */
public final class FoundationController {
    @FXML private BorderPane root;
    @FXML private HBox tabStrip;
    @FXML private Button newHomeButton;
    @FXML private Label statusPill, statusLeft, statusRight;
    @FXML private StackPane workspace;

    private final AppConfig config = AppConfig.load();
    private final NoteService noteService = new NoteService(config.notesDirectory());
    private final AIAssistantService assistantService = new AIAssistantService(
            new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()),
            new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(), Duration.ofSeconds(config.requestTimeoutSeconds())));
    private final Map<Path, List<ChatMessage>> conversations = new HashMap<>();
    private final List<TabEntry> entries = new ArrayList<>();
    private final Circle pillDot = new Circle(4);
    private TabEntry current;
    private List<NoteItem> items = List.of();

    /** One workspace tab: a pill in the strip plus either a Home view or a note editor. */
    private final class TabEntry {
        final HBox pill = new HBox(8);
        final Circle dot = new Circle(4);
        final Label titleLabel = new Label();
        final Tooltip tooltip = new Tooltip();
        Node content;
        HomeViewController home;
        EditorViewController editor;

        TabEntry() {
            titleLabel.getStyleClass().add("tab-title"); titleLabel.setTextOverrun(OverrunStyle.ELLIPSIS); titleLabel.setMinWidth(24);
            HBox.setHgrow(titleLabel, Priority.ALWAYS);
            Button close = new Button("×"); close.getStyleClass().add("tab-close"); close.setFocusTraversable(false);
            close.setOnAction(e -> { e.consume(); closeEntry(this); });
            pill.getStyleClass().add("tab-pill"); pill.setAlignment(javafx.geometry.Pos.CENTER_LEFT); pill.setMaxWidth(220);
            pill.getChildren().addAll(dot, titleLabel, close);
            pill.setOnMouseClicked(e -> select(this));
            Tooltip.install(pill, tooltip);
            updateDot();
        }
        void setTitle(String text) { titleLabel.setText(text); tooltip.setText(text); }
        void updateDot() { setDot(dot, editor == null ? "dot-blue" : editor.isDirty() ? "dot-amber" : "dot-green"); }
        void updateActive() { FxUtil.toggleClass(pill, "active", this == current); }
    }

    @FXML private void initialize() {
        newHomeButton.setTooltip(new Tooltip("Open a new Home tab"));
        newHomeButton.setOnAction(e -> addHomeTab());
        statusPill.setGraphic(pillDot);
        root.sceneProperty().addListener((o, old, scene) -> { if (scene != null) installShortcuts(scene); });
        addHomeTab();
        refreshNotes();
    }

    private void installShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN), this::newNoteShortcut);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.K, KeyCombination.SHORTCUT_DOWN), this::searchShortcut);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> { if (current != null && current.editor != null) current.editor.save(); });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.J, KeyCombination.SHORTCUT_DOWN), () -> { if (current != null && current.editor != null) openAssistant(current); });
    }

    private void newNoteShortcut() { promptCreate(current != null && current.home != null ? current : null); }
    private void searchShortcut() {
        if (current == null || current.home == null) { addHomeTab(); return; } // a fresh Home tab focuses its own search field
        current.home.focusSearch();
    }

    // ---------------------------------------------------------------- notes (disk I/O off the FX thread)

    private void refreshNotes() {
        FxUtil.async(() -> noteService.listNotes().stream().map(NoteItem::of).toList(), loaded -> {
            items = loaded;
            entries.stream().filter(e -> e.home != null).forEach(e -> e.home.setNotes(items));
            refreshStatus();
        }, ex -> { statusLeft.setText("Could not load notes · " + ex.getMessage()); items = List.of(); });
    }

    private void promptCreate(TabEntry source) {
        TextInputDialog dialog = new TextInputDialog("Untitled note");
        dialog.setTitle("New note"); dialog.setHeaderText("Create a TXT note");
        dialog.showAndWait().ifPresent(title -> FxUtil.async(() -> noteService.createNote(title), note -> {
            refreshNotes(); openNote(source, note);
        }, ex -> showError("Could not create note", ex.getMessage())));
    }

    // ---------------------------------------------------------------- tabs

    private void addHomeTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/home-view.fxml"));
            Node view = loader.load(); HomeViewController controller = loader.getController();
            TabEntry entry = new TabEntry(); entry.home = controller; entry.content = view; entry.setTitle("Home");
            controller.setNotes(items);
            controller.setOpenNote(note -> openNote(entry, note));
            controller.setCreateNote(() -> promptCreate(entry));
            controller.setStateListener(() -> { if (entry == current) refreshStatus(); });
            addEntry(entry);
        } catch (IOException ex) { showError("Could not open Home", ex.getMessage()); }
    }

    private void addEntry(TabEntry entry) {
        entries.add(entry);
        tabStrip.getChildren().add(tabStrip.getChildren().size() - 1, entry.pill); // keep the inline "+" last
        select(entry);
    }

    private void select(TabEntry entry) {
        current = entry;
        workspace.getChildren().setAll(entry.content);
        entries.forEach(TabEntry::updateActive);
        refreshStatus();
        if (entry.home != null) entry.home.focusSearch();
        if (entry.editor != null) entry.editor.focusEditor();
    }

    private void closeEntry(TabEntry entry) {
        if (entry.editor != null) entry.editor.requestClose(() -> removeEntry(entry)); else removeEntry(entry);
    }

    private void removeEntry(TabEntry entry) {
        int index = entries.indexOf(entry);
        if (index < 0) return;
        entries.remove(index);
        tabStrip.getChildren().remove(entry.pill);
        if (entries.isEmpty()) { current = null; addHomeTab(); }
        else if (entry == current) select(entries.get(Math.min(index, entries.size() - 1)));
    }

    private void openNote(TabEntry source, Note note) {
        for (TabEntry existing : entries) {
            if (existing.editor != null && existing.editor.note().path().equals(note.path())) { select(existing); return; }
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/editor-view.fxml"));
            Node view = loader.load(); EditorViewController editor = loader.getController();
            TabEntry entry = source != null && entries.contains(source) ? source : new TabEntry();
            Instant modified = items.stream().filter(i -> i.note().path().equals(note.path())).map(NoteItem::modified).findFirst().orElse(null);
            editor.configure(noteService, note, modified, config.chatModelName());
            editor.setAskAction(() -> openAssistant(entry));
            editor.setDeletedAction(() -> { removeEntry(entry); refreshNotes(); });
            editor.setSavedAction(this::refreshNotes);
            editor.setRenamedAction(title -> { entry.setTitle(title); refreshNotes(); });
            editor.setStateListener(() -> { entry.updateDot(); if (entry == current) refreshStatus(); });
            entry.home = null; entry.editor = editor; entry.content = view; entry.setTitle(note.title()); entry.updateDot();
            if (entries.contains(entry)) select(entry); else addEntry(entry);
        } catch (IOException ex) { showError("Could not open note", ex.getMessage()); }
    }

    // ---------------------------------------------------------------- AI panel

    private void openAssistant(TabEntry entry) {
        EditorViewController editor = entry.editor;
        if (editor == null || editor.isAssistantOpen()) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/assistant-panel.fxml"));
            Node panel = loader.load(); AssistantPanelController assistant = loader.getController();
            List<ChatMessage> history = conversations.computeIfAbsent(editor.note().path(), ignored -> new ArrayList<>());
            assistant.configure(config, assistantService, () -> editor.note().title(), editor::currentContent, history,
                    editor::insertText, () -> editor.setAssistantPanel(null));
            editor.setAssistantPanel(panel);
        } catch (IOException ex) { showError("Could not open assistant", ex.getMessage()); }
    }

    // ---------------------------------------------------------------- status

    private void refreshStatus() {
        if (current == null) return;
        String[] text;
        if (current.home != null) {
            text = current.home.status();
            statusPill.setText("Local Workspace"); setDot(pillDot, "dot-blue");
        } else {
            text = current.editor.status();
            boolean dirty = current.editor.isDirty();
            statusPill.setText(dirty ? "Unsaved changes" : "Saved"); setDot(pillDot, dirty ? "dot-amber" : "dot-green");
        }
        statusLeft.setText(text[0]); statusRight.setText(text[1]);
    }

    private static void setDot(Circle dot, String styleClass) {
        dot.getStyleClass().removeAll("dot-blue", "dot-green", "dot-amber");
        dot.getStyleClass().add(styleClass);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? title : message, ButtonType.OK);
        alert.setTitle(title); alert.showAndWait();
    }
}
