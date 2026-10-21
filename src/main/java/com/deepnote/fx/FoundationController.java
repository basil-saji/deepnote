package com.deepnote.fx;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.ai.ContextManager;
import com.deepnote.ai.OllamaChatModelClient;
import com.deepnote.config.AppConfig;
import com.deepnote.exception.NoteStorageException;
import com.deepnote.model.Note;
import com.deepnote.model.ChatMessage;
import com.deepnote.service.LocalSearchService;
import com.deepnote.service.NoteService;
import javafx.concurrent.Task;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/** Main JavaFX workspace. Existing file and AI services remain the business layer. */
public final class FoundationController {
    @FXML private BorderPane root;
    private final AppConfig config = AppConfig.load();
    private final NoteService noteService = new NoteService(config.notesDirectory());
    private final LocalSearchService searchService = new LocalSearchService();
    private final AIAssistantService assistantService = new AIAssistantService(
            new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()),
            new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(),
                    Duration.ofSeconds(config.requestTimeoutSeconds())));
    private final TabPane tabs = new TabPane();
    private final Map<Tab, EditorState> editors = new HashMap<>();
    private final Map<Path, List<ChatMessage>> conversations = new HashMap<>();
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final Label status = new Label("Ready · TXT workspace");

    @FXML private void initialize() { buildShell(); refreshNotes(); addHomeTab(); }

    private void buildShell() {
        root.getStyleClass().add("app-root");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.ALL_TABS);
        tabs.getStyleClass().add("workspace-tabs");
        Button plus = button("+", "icon-button");
        plus.setTooltip(new Tooltip("Open a new Home tab")); plus.setOnAction(e -> addHomeTab());
        HBox tabBar = new HBox(tabs, plus); tabBar.setAlignment(Pos.CENTER_LEFT); tabBar.getStyleClass().add("tab-bar");
        HBox.setHgrow(tabs, Priority.ALWAYS);
        Label brand = new Label("◆  DeepNote"); brand.getStyleClass().add("brand");
        Label model = new Label("LOCAL · " + config.chatModelName()); model.getStyleClass().add("mono-label");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox header = new HBox(brand, gap, model); header.setAlignment(Pos.CENTER_LEFT); header.getStyleClass().add("app-header");
        root.setTop(new VBox(header, tabBar)); root.setCenter(tabs);
        status.getStyleClass().add("status-line"); root.setBottom(status);
    }

    private void addHomeTab() {
        Tab tab = new Tab("Home"); tab.setContent(homeView(tab)); tab.setClosable(true);
        tab.setOnClosed(event -> editors.remove(tab));
        tabs.getTabs().add(tab); tabs.getSelectionModel().select(tab);
    }

    private Node homeView(Tab homeTab) {
        VBox page = new VBox(24); page.getStyleClass().add("home-page"); page.setPadding(new Insets(34, 48, 40, 48));
        HBox heading = new HBox(12); Label greeting = new Label("Good morning"); greeting.getStyleClass().add("page-title");
        Label mark = new Label("●"); mark.getStyleClass().add("accent-text"); Region push = new Region(); HBox.setHgrow(push, Priority.ALWAYS);
        Button create = button("+  New note", "primary-button"); create.setOnAction(e -> createNote(homeTab));
        Button settings = button("⚙", "icon-button"); settings.setTooltip(new Tooltip("Settings"));
        heading.getChildren().addAll(greeting, mark, push, create, settings);
        TextField search = new TextField(); search.setPromptText("Search notes, tags, summaries, AI insights…"); search.getStyleClass().add("search-field");
        VBox dynamic = new VBox(26); dynamic.setId("home-dynamic");
        HBox controls = new HBox(8); controls.setAlignment(Pos.CENTER_RIGHT);
        ToggleGroup group = new ToggleGroup(); ToggleButton grid = new ToggleButton("▦  Grid"); ToggleButton list = new ToggleButton("☰  List");
        grid.setToggleGroup(group); list.setToggleGroup(group); grid.setSelected(true); grid.getStyleClass().add("view-toggle"); list.getStyleClass().add("view-toggle");
        controls.getChildren().addAll(grid, list);
        search.textProperty().addListener((o, old, value) -> populateHome(dynamic, homeTab, value));
        grid.setOnAction(e -> { dynamic.setUserData(false); populateHome(dynamic, homeTab, search.getText()); });
        list.setOnAction(e -> { dynamic.setUserData(true); populateHome(dynamic, homeTab, search.getText()); });
        page.getChildren().addAll(heading, search, controls, dynamic); populateHome(dynamic, homeTab, "");
        ScrollPane scroll = new ScrollPane(page); scroll.setFitToWidth(true); return scroll;
    }

    private void populateHome(VBox dynamic, Tab homeTab, String query) {
        boolean listMode = Boolean.TRUE.equals(dynamic.getUserData());
        List<Note> filtered = searchService.search(notes, query); dynamic.getChildren().clear();
        if (filtered.isEmpty()) {
            Label empty = new Label(query == null || query.isBlank() ? "No notes yet. Create your first note to get started." : "No notes match that search.");
            empty.getStyleClass().add("empty-state"); dynamic.getChildren().add(empty); return;
        }
        dynamic.getChildren().add(section("RECENTS", filtered.stream().limit(3).toList(), homeTab, listMode));
        dynamic.getChildren().add(section("NOTES  ·  " + filtered.size(), filtered, homeTab, listMode));
    }

    private VBox section(String title, List<Note> items, Tab homeTab, boolean listMode) {
        VBox section = new VBox(12); Label heading = new Label(title); heading.getStyleClass().add("section-label");
        FlowPane cards = new FlowPane(14, 14); cards.getStyleClass().add(listMode ? "note-list" : "note-grid");
        items.forEach(note -> cards.getChildren().add(noteCard(note, homeTab))); section.getChildren().addAll(heading, cards); return section;
    }

    private Node noteCard(Note note, Tab homeTab) {
        VBox card = new VBox(10); card.getStyleClass().add("note-card");
        Label tag = new Label("TXT  ·  NOTE"); tag.getStyleClass().add("card-tag");
        Label title = new Label(note.title()); title.getStyleClass().add("card-title");
        String preview = note.content().replaceAll("\\s+", " ").trim(); Label body = new Label(preview.isBlank() ? "Empty note" : preview);
        body.setWrapText(true); body.setMaxHeight(48); body.getStyleClass().add("card-preview");
        Label meta = new Label(wordCount(note.content()) + " words"); meta.getStyleClass().add("mono-label"); card.getChildren().addAll(tag, title, body, meta);
        card.setOnMouseClicked(e -> openNote(homeTab, note)); return card;
    }

    private void openNote(Tab source, Note note) {
        for (Tab tab : tabs.getTabs()) { EditorState state = editors.get(tab); if (state != null && state.note.path().equals(note.path())) { tabs.getSelectionModel().select(tab); return; } }
        EditorState state = editorView(note); source.setText(note.title()); source.setContent(state.container); editors.put(source, state);
        source.setOnCloseRequest(event -> { if (!confirmClose(state)) event.consume(); });
        tabs.getSelectionModel().select(source);
    }

    private EditorState editorView(Note initial) {
        TextArea editor = new TextArea(initial.content()); editor.setWrapText(true); editor.getStyleClass().add("note-editor");
        Label title = new Label(initial.title()); title.getStyleClass().add("editor-title"); Label meta = new Label(metadata(initial.content())); meta.getStyleClass().add("editor-meta");
        Button home = button("Home", "text-button"); home.setOnAction(e -> addHomeTab()); Button save = button("Save", "secondary-button");
        Button rename = button("Rename", "secondary-button"); Button delete = button("Delete", "danger-button"); Button ask = button("✦  Ask about this note", "primary-button");
        HBox actions = new HBox(8, home, save, rename, delete, ask); actions.setAlignment(Pos.CENTER_RIGHT); HBox.setHgrow(actions, Priority.ALWAYS);
        HBox titleRow = new HBox(title, actions); titleRow.setAlignment(Pos.CENTER_LEFT); VBox column = new VBox(16, titleRow, meta, editor); column.getStyleClass().add("editor-column"); column.setPadding(new Insets(34, 48, 24, 48)); VBox.setVgrow(editor, Priority.ALWAYS);
        BorderPane container = new BorderPane(column); EditorState state = new EditorState(initial, editor, title, meta, container);
        editor.textProperty().addListener((o, old, value) -> { state.dirty = true; title.setText(initial.title() + "  •"); meta.setText(metadata(value)); });
        save.setOnAction(e -> save(state)); rename.setOnAction(e -> rename(state)); delete.setOnAction(e -> delete(state)); ask.setOnAction(e -> showAssistant(state)); return state;
    }

    private void showAssistant(EditorState state) {
        if (state.aiPanel != null) { state.container.setRight(null); state.aiPanel = null; return; }
        VBox panel = new VBox(12); panel.getStyleClass().add("ai-panel"); panel.setPadding(new Insets(20)); panel.setPrefWidth(330);
        HBox head = new HBox(8); Label heading = new Label("DeepNote AI"); heading.getStyleClass().add("panel-title"); Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS); Button close = button("×", "icon-button");
        close.setOnAction(e -> { state.container.setRight(null); state.aiPanel = null; }); head.getChildren().addAll(heading, gap, close);
        Label active = new Label(state.note.title()); active.getStyleClass().add("mono-label"); ListView<String> messages = new ListView<>(); messages.getStyleClass().add("message-list");
        for (ChatMessage m : conversations.getOrDefault(state.note.path(), List.of())) messages.getItems().add((m.role() == ChatMessage.Role.USER ? "You\n" : "AI\n") + m.content());
        TextArea question = new TextArea(); question.setPromptText("Ask about this note…"); question.setPrefRowCount(3); question.getStyleClass().add("composer"); Button send = button("Send  ↑", "primary-button");
        HBox composer = new HBox(question, send); HBox.setHgrow(question, Priority.ALWAYS); panel.getChildren().addAll(head, active, messages, composer); VBox.setVgrow(messages, Priority.ALWAYS);
        send.setOnAction(e -> askAI(state, question, messages, send)); state.aiPanel = panel; state.container.setRight(panel);
    }

    private void askAI(EditorState state, TextArea question, ListView<String> messages, Button send) {
        String prompt = question.getText().trim(); if (prompt.isBlank() || state.running) return; state.running = true; send.setDisable(true); messages.getItems().add("You\n" + prompt); question.clear();
        List<ChatMessage> history = conversations.computeIfAbsent(state.note.path(), ignored -> new ArrayList<>());
        Task<String> task = new Task<>() { @Override protected String call() { return assistantService.answer(state.note.title(), state.editor.getText(), history, prompt); } };
        task.setOnSucceeded(e -> { String answer = task.getValue(); history.add(new ChatMessage(ChatMessage.Role.USER, prompt)); history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, answer)); messages.getItems().add("AI\n" + answer); state.running = false; send.setDisable(false); });
        task.setOnFailed(e -> { messages.getItems().add("Error\n" + task.getException().getMessage()); state.running = false; send.setDisable(false); });
        Thread thread = new Thread(task, "deepnote-ollama"); thread.setDaemon(true); thread.start();
    }

    private void createNote(Tab home) { TextInputDialog d = new TextInputDialog("Untitled note"); d.setTitle("New note"); d.setHeaderText("Create a TXT note"); d.showAndWait().ifPresent(t -> { try { Note n = noteService.createNote(t); refreshNotes(); openNote(home, n); } catch (NoteStorageException ex) { error(ex.getMessage()); } }); }
    private void save(EditorState s) { try { s.note = noteService.saveNote(s.note, s.editor.getText()); s.dirty = false; s.title.setText(s.note.title()); status.setText("Saved · " + s.note.title()); } catch (NoteStorageException ex) { error(ex.getMessage()); } }
    private void rename(EditorState s) { TextInputDialog d = new TextInputDialog(s.note.title()); d.setTitle("Rename note"); d.showAndWait().ifPresent(t -> { try { s.note = noteService.renameNote(s.note, t); s.title.setText(s.note.title()); refreshNotes(); } catch (NoteStorageException ex) { error(ex.getMessage()); } }); }
    private void delete(EditorState s) { Alert d = new Alert(Alert.AlertType.CONFIRMATION, "Delete “" + s.note.title() + "”?", ButtonType.CANCEL, ButtonType.OK); d.showAndWait().filter(ButtonType.OK::equals).ifPresent(ok -> { try { noteService.deleteNote(s.note); tabs.getTabs().removeIf(tab -> editors.get(tab) == s); refreshNotes(); } catch (NoteStorageException ex) { error(ex.getMessage()); } }); }
    private void refreshNotes() { notes.setAll(noteService.listNotes()); }
    private boolean confirmClose(EditorState state) {
        if (!state.dirty) return true;
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, "Save changes to “" + state.note.title() + "” before closing?", ButtonType.CANCEL, ButtonType.NO, ButtonType.YES);
        Optional<ButtonType> choice = dialog.showAndWait();
        if (choice.isEmpty() || choice.get() == ButtonType.CANCEL) return false;
        if (choice.get() == ButtonType.YES) { save(state); return !state.dirty; }
        return true;
    }
    private void error(String message) { new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait(); }
    private Button button(String text, String style) { Button b = new Button(text); b.getStyleClass().add(style); return b; }
    private String metadata(String content) { return wordCount(content) + " words  ·  " + content.length() + " characters"; }
    private long wordCount(String content) { return Arrays.stream(content.trim().split("\\s+")).filter(s -> !s.isBlank()).count(); }
    private static final class EditorState { Note note; final TextArea editor; final Label title; final Label meta; final BorderPane container; VBox aiPanel; boolean dirty; boolean running; EditorState(Note n, TextArea e, Label t, Label m, BorderPane c) { note=n; editor=e; title=t; meta=m; container=c; } }
}
