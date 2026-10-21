package com.deepnote.fx;

import com.deepnote.ai.*;
import com.deepnote.config.AppConfig;
import com.deepnote.exception.NoteStorageException;
import com.deepnote.model.ChatMessage;
import com.deepnote.model.Note;
import com.deepnote.service.NoteService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;

/** Small workspace coordinator; substantial screens are FXML-backed focused controllers. */
public final class FoundationController {
    @FXML private TabPane tabs;
    @FXML private Button newHomeButton;
    @FXML private Label modelLabel, statusLabel;
    @FXML private StackPane workspace;
    private final AppConfig config = AppConfig.load();
    private final NoteService noteService = new NoteService(config.notesDirectory());
    private final AIAssistantService assistantService = new AIAssistantService(new ContextManager(config.maxRecentChatTurns(), config.contextCharacterBudget()), new OllamaChatModelClient(config.ollamaBaseUrl(), config.chatModelName(), Duration.ofSeconds(config.requestTimeoutSeconds())));
    private final Map<Path, List<ChatMessage>> conversations = new HashMap<>();
    private final Map<Tab, EditorViewController> editors = new HashMap<>();
    private List<Note> notes = List.of();

    @FXML private void initialize() {
        modelLabel.setText("Local workspace · " + config.chatModelName());
        newHomeButton.setTooltip(new Tooltip("Open a new Home tab")); newHomeButton.setOnAction(e -> addHomeTab());
        tabs.getSelectionModel().selectedItemProperty().addListener((o, old, selected) -> updateStatus(selected));
        refreshNotes(); addHomeTab();
    }

    private void refreshNotes() { try { notes = noteService.listNotes(); } catch (NoteStorageException ex) { statusLabel.setText("Could not load notes · " + ex.getMessage()); notes = List.of(); } }
    private void addHomeTab() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/home-view.fxml")); Node view = loader.load(); HomeViewController controller = loader.getController();
            Tab tab = new Tab("Home", view); tab.setClosable(true); controller.setNotes(notes); controller.setOpenNote(note -> openNote(tab, note)); controller.setCreateNote(() -> createNote(tab));
            tab.setOnClosed(e -> { if (tabs.getTabs().isEmpty()) addHomeTab(); }); tabs.getTabs().add(tab); tabs.getSelectionModel().select(tab);
        } catch (IOException ex) { showError("Could not open Home", ex.getMessage()); }
    }

    private void openNote(Tab source, Note note) {
        for (Tab tab : tabs.getTabs()) { EditorViewController existing = editors.get(tab); if (existing != null && existing.note().path().equals(note.path())) { tabs.getSelectionModel().select(tab); return; } }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/editor-view.fxml")); Node view = loader.load(); EditorViewController editor = loader.getController();
            editor.configure(noteService, note, this::openAssistant, () -> { editors.remove(source); tabs.getTabs().remove(source); refreshNotes(); if (tabs.getTabs().isEmpty()) addHomeTab(); });
            editor.setRenamedAction(source::setText);
            source.setText(note.title()); source.setContent(view); source.setOnCloseRequest(e -> { if (!editor.confirmClose()) e.consume(); }); editors.put(source, editor); tabs.getSelectionModel().select(source);
        } catch (IOException ex) { showError("Could not open note", ex.getMessage()); }
    }

    private void createNote(Tab home) { TextInputDialog dialog = new TextInputDialog("Untitled note"); dialog.setTitle("New note"); dialog.setHeaderText("Create a TXT note"); dialog.showAndWait().ifPresent(title -> { try { Note note = noteService.createNote(title); refreshNotes(); openNote(home, note); } catch (NoteStorageException ex) { showError("Could not create note", ex.getMessage()); } }); }
    private void openAssistant(EditorViewController editor) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/deepnote/fx/assistant-panel.fxml")); Node panel = loader.load(); AssistantPanelController assistant = loader.getController();
            List<ChatMessage> history = conversations.computeIfAbsent(editor.note().path(), ignored -> new ArrayList<>());
            assistant.configure(assistantService, editor.note().title(), editor::currentContent, history, () -> ((javafx.scene.layout.BorderPane) editorRoot(editor)).setRight(null));
            ((javafx.scene.layout.BorderPane) editorRoot(editor)).setRight(panel);
        } catch (IOException ex) { showError("Could not open assistant", ex.getMessage()); }
    }
    private Node editorRoot(EditorViewController editor) { for (Tab tab : editors.keySet()) if (editors.get(tab) == editor && tab.getContent() instanceof Node node) return node; throw new IllegalStateException("Editor tab is not open"); }
    private void updateStatus(Tab selected) { EditorViewController editor = editors.get(selected); statusLabel.setText(editor == null ? "Local workspace · " + notes.size() + " notes" : "UTF-8 · TXT · " + (editor.isDirty() ? "Unsaved changes" : "Saved")); }
    private void showError(String title, String message) { new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait(); }
}
