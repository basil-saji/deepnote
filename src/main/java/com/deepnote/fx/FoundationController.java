package com.deepnote.fx;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.ai.ContextManager;
import com.deepnote.ai.OllamaChatModelClient;
import com.deepnote.config.AppConfig;
import com.deepnote.model.ChatMessage;
import com.deepnote.model.Note;
import com.deepnote.service.NoteService;
import com.deepnote.service.NoteIndexService;
import com.deepnote.service.TextChunker;
import com.deepnote.db.Database;
import com.deepnote.ai.OllamaEmbeddingClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/** Workspace shell: single-row tab strip, active view host and live bottom status bar. */
public final class FoundationController {
    @FXML private BorderPane root;
    @FXML private HBox topBar, tabStrip;
    @FXML private Button newHomeButton, backButton, minButton, maxButton, closeButton;
    @FXML private Label statusLeft, statusRight;
    @FXML private StackPane workspace;

    private final AppConfig config = AppConfig.load();
    private final NoteService noteService = new NoteService(config.notesDirectory());
    private final Database database = new Database(config);
    private final NoteIndexService indexService = new NoteIndexService(database, new OllamaEmbeddingClient(config.ollamaBaseUrl(), config.embeddingModel(), Duration.ofSeconds(config.requestTimeoutSeconds()), config.embeddingDimensions()), new TextChunker(1200));
    private final AIAssistantService assistantService = new AIAssistantService(
            new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()),
            new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(), Duration.ofSeconds(config.requestTimeoutSeconds())));
    private final Map<Path, List<ChatMessage>> conversations = new HashMap<>();
    private final List<TabEntry> entries = new ArrayList<>();
    private static final double EDGE = 6;
    private Stage stage;
    private boolean maximized, dragging;
    private double dragOffsetX, dragOffsetY, startX, startY, startW, startH, startScreenX, startScreenY;
    private double[] restoreBounds;
    private int resizeMask;
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
        setupWindowChrome();
        root.sceneProperty().addListener((o, old, scene) -> {
            if (scene == null) return;
            installShortcuts(scene);
            scene.windowProperty().addListener((w, a, window) -> { if (window instanceof Stage s) attachStage(s, scene); });
            if (scene.getWindow() instanceof Stage s) attachStage(s, scene);
        });
        addHomeTab();
        refreshNotes();
        FxUtil.async(() -> { database.migrate(); return null; }, ignored -> { }, ignored -> { });
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
        Dialogs.prompt(window(), "New note", "Give your TXT note a title.", "Untitled note", "Create",
                title -> FxUtil.async(() -> noteService.createNote(title), note -> { refreshNotes(); openNote(source, note); },
                        ex -> showError("Could not create note", ex.getMessage())));
    }

    // ---------------------------------------------------------------- tabs

    private void addHomeTab() {
        TabEntry entry = new TabEntry();
        if (bindHome(entry)) addEntry(entry);
    }

    /** Turns {@code entry} into a Home tab (new tab, or an editor tab going Back). */
    private boolean bindHome(TabEntry entry) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/home-view.fxml"));
            Node view = loader.load(); HomeViewController controller = loader.getController();
            entry.home = controller; entry.editor = null; entry.content = view; entry.setTitle("Home"); entry.updateDot();
            controller.setNotes(items);
            controller.setOpenNote(note -> openNote(entry, note));
            controller.setOpenVectorResult(result -> openNote(null, new Note(result.title(), result.path(), ""), result.chunkText()));
            controller.setCreateNote(() -> promptCreate(entry));
            controller.setVectorSearch(new com.deepnote.service.VectorSearchService(database, new OllamaEmbeddingClient(config.ollamaBaseUrl(), config.embeddingModel(), Duration.ofSeconds(config.requestTimeoutSeconds()), config.embeddingDimensions()), config.similarityThreshold()));
            controller.setStateListener(() -> { if (entry == current) refreshStatus(); });
            return true;
        } catch (IOException ex) { showError("Could not open Home", ex.getMessage()); return false; }
    }

    /** Back button: leaves the note (asking Save/Discard/Cancel if dirty) and returns this tab to Home. */
    private void goBack() {
        TabEntry entry = current;
        if (entry == null || entry.editor == null) return;
        entry.editor.requestClose(() -> {
            if (bindHome(entry)) { select(entry); refreshNotes(); }
        });
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
        FxUtil.show(backButton, entry.editor != null);
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
        openNote(source, note, null);
    }

    private void openNote(TabEntry source, Note note, String passage) {
        for (TabEntry existing : entries) {
            if (existing.editor != null && existing.editor.note().path().equals(note.path())) { select(existing); if (passage != null) existing.editor.highlightPassage(passage); return; }
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/editor-view.fxml"));
            Node view = loader.load(); EditorViewController editor = loader.getController();
            TabEntry entry = source != null && entries.contains(source) ? source : new TabEntry();
            Instant modified = items.stream().filter(i -> i.note().path().equals(note.path())).map(NoteItem::modified).findFirst().orElse(null);
            Note latest = noteService.readNote(note.path());
            editor.configure(noteService, latest, modified, config.chatModelName(), indexService);
            if (passage != null) editor.highlightPassage(passage);
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
        String[] text = current.home != null ? current.home.status() : current.editor.status();
        statusLeft.setText(text[0]); statusRight.setText(text[1]);
    }

    private static void setDot(Circle dot, String styleClass) {
        dot.getStyleClass().removeAll("dot-blue", "dot-green", "dot-amber");
        dot.getStyleClass().add(styleClass);
    }

    private void showError(String title, String message) { Dialogs.message(window(), title, message == null ? title : message); }

    private javafx.stage.Window window() { return root.getScene() == null ? null : root.getScene().getWindow(); }

    // ---------------------------------------------------------------- window chrome (undecorated stage)

    private void setupWindowChrome() {
        backButton.setGraphic(iconBox(FxUtil.BACK)); backButton.setTooltip(new Tooltip("Back to Home"));
        backButton.setOnAction(e -> goBack()); FxUtil.show(backButton, false);
        minButton.setGraphic(iconBox(FxUtil.MINIMIZE)); minButton.setTooltip(new Tooltip("Minimize"));
        maxButton.setGraphic(iconBox(FxUtil.MAXIMIZE)); maxButton.setTooltip(new Tooltip("Maximize"));
        closeButton.setGraphic(iconBox(FxUtil.CLOSE)); closeButton.setTooltip(new Tooltip("Close"));
        minButton.setOnAction(e -> { if (stage != null) stage.setIconified(true); });
        maxButton.setOnAction(e -> toggleMaximize());
        closeButton.setOnAction(e -> closeWindow());
        topBar.setOnMousePressed(this::barPressed);
        topBar.setOnMouseDragged(this::barDragged);
        topBar.setOnMouseReleased(e -> dragging = false);
    }

    private static Node iconBox(String path) { StackPane box = new StackPane(FxUtil.icon(path)); box.setMinSize(16, 16); return box; }

    private void attachStage(Stage target, Scene scene) {
        if (stage == target) return;
        stage = target;
        stage.setOnCloseRequest(e -> { e.consume(); closeWindow(); });
        scene.addEventFilter(MouseEvent.MOUSE_MOVED, this::edgeMoved);
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, this::edgePressed);
        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::edgeDragged);
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> resizeMask = 0);
    }

    /** Checks every dirty editor (Save / Discard / Cancel) before the app exits. */
    private void closeWindow() { confirmClosing(new ArrayList<>(entries).iterator()); }

    private void confirmClosing(Iterator<TabEntry> remaining) {
        while (remaining.hasNext()) {
            TabEntry entry = remaining.next();
            if (entry.editor != null && entry.editor.isDirty()) {
                select(entry);
                entry.editor.requestClose(() -> confirmClosing(remaining));
                return;
            }
        }
        Platform.exit();
    }

    private void toggleMaximize() {
        if (stage == null) return;
        if (maximized) { restoreWindow(); return; }
        restoreBounds = new double[] { stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight() };
        List<Screen> screens = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
        Rectangle2D area = (screens.isEmpty() ? Screen.getPrimary() : screens.get(0)).getVisualBounds(); // never covers the taskbar
        stage.setX(area.getMinX()); stage.setY(area.getMinY()); stage.setWidth(area.getWidth()); stage.setHeight(area.getHeight());
        maximized = true; updateMaxIcon();
    }

    private void restoreWindow() {
        if (restoreBounds != null) { stage.setX(restoreBounds[0]); stage.setY(restoreBounds[1]); stage.setWidth(restoreBounds[2]); stage.setHeight(restoreBounds[3]); }
        maximized = false; updateMaxIcon();
    }

    private void updateMaxIcon() {
        maxButton.setGraphic(iconBox(maximized ? FxUtil.RESTORE : FxUtil.MAXIMIZE));
        maxButton.getTooltip().setText(maximized ? "Restore" : "Maximize");
    }

    private boolean interactive(Object target) {
        Node node = target instanceof Node n ? n : null;
        while (node != null && node != topBar) {
            if (node instanceof ButtonBase || node.getStyleClass().contains("tab-pill")) return true;
            node = node.getParent();
        }
        return false;
    }

    private void barPressed(MouseEvent e) {
        if (stage == null) return;
        dragging = !interactive(e.getTarget());
        if (!dragging) return;
        if (e.getClickCount() == 2) { toggleMaximize(); dragging = false; return; }
        dragOffsetX = e.getScreenX() - stage.getX(); dragOffsetY = e.getScreenY() - stage.getY();
    }

    private void barDragged(MouseEvent e) {
        if (stage == null || !dragging) return;
        if (maximized) { // un-maximize proportionally so the cursor stays at the same relative spot on the bar
            double ratio = dragOffsetX / Math.max(1, stage.getWidth());
            restoreWindow(); dragOffsetX = ratio * stage.getWidth();
        }
        stage.setX(e.getScreenX() - dragOffsetX); stage.setY(e.getScreenY() - dragOffsetY);
    }

    private int edgeMask(MouseEvent e, Scene scene) {
        int mask = 0;
        if (e.getSceneX() < EDGE) mask |= 1;
        if (e.getSceneX() > scene.getWidth() - EDGE) mask |= 2;
        if (e.getSceneY() < EDGE) mask |= 4;
        if (e.getSceneY() > scene.getHeight() - EDGE) mask |= 8;
        return mask;
    }

    private void edgeMoved(MouseEvent e) {
        Scene scene = root.getScene();
        int mask = maximized ? 0 : edgeMask(e, scene);
        scene.setCursor(switch (mask) {
            case 1, 2 -> Cursor.H_RESIZE; case 4, 8 -> Cursor.V_RESIZE;
            case 5, 10 -> Cursor.NW_RESIZE; case 6, 9 -> Cursor.NE_RESIZE;
            default -> Cursor.DEFAULT;
        });
    }

    private void edgePressed(MouseEvent e) {
        if (maximized || stage == null) return;
        resizeMask = edgeMask(e, root.getScene());
        if (resizeMask == 0) return;
        startX = stage.getX(); startY = stage.getY(); startW = stage.getWidth(); startH = stage.getHeight();
        startScreenX = e.getScreenX(); startScreenY = e.getScreenY();
        e.consume();
    }

    private void edgeDragged(MouseEvent e) {
        if (resizeMask == 0 || stage == null) return;
        double dx = e.getScreenX() - startScreenX, dy = e.getScreenY() - startScreenY;
        double minW = stage.getMinWidth(), minH = stage.getMinHeight();
        double x = startX, y = startY, w = startW, h = startH;
        if ((resizeMask & 2) != 0) w = Math.max(minW, startW + dx);
        if ((resizeMask & 1) != 0) { w = Math.max(minW, startW - dx); x = startX + (startW - w); }
        if ((resizeMask & 8) != 0) h = Math.max(minH, startH + dy);
        if ((resizeMask & 4) != 0) { h = Math.max(minH, startH - dy); y = startY + (startH - h); }
        stage.setX(x); stage.setY(y); stage.setWidth(w); stage.setHeight(h);
        e.consume();
    }
}
