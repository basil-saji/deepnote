package com.deepnote.fx;

import com.deepnote.model.Note;
import com.deepnote.service.NoteService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

import java.time.Instant;
import java.util.function.Consumer;

/** Plain-text note editor: centred reading column, icon toolbar and borderless text area. */
public final class EditorViewController {
    @FXML private BorderPane root;
    @FXML private Label titleLabel, metadataLabel;
    @FXML private TextArea contentArea;
    @FXML private Button renameButton, deleteButton, saveButton, askButton;

    private final BooleanProperty dirty = new SimpleBooleanProperty(false);
    private NoteService noteService;
    private Note note;
    private Instant lastEdited = Instant.now();
    private String modelName = "";
    private boolean loading, assistantOpen;
    private Runnable askAction = () -> { };
    private Runnable deletedAction = () -> { };
    private Runnable savedAction = () -> { };
    private Runnable stateListener = () -> { };
    private Consumer<String> renamedAction = ignored -> { };

    @FXML private void initialize() {
        setIcon(renameButton, FxUtil.PENCIL, "Rename note");
        setIcon(deleteButton, FxUtil.TRASH, "Delete note"); deleteButton.getStyleClass().add("danger");
        setIcon(saveButton, FxUtil.SAVE, "Save note (Ctrl+S)");
        askButton.setTooltip(new Tooltip("Ask DeepNote about this note (Ctrl+J)"));
        renameButton.setOnAction(e -> rename());
        deleteButton.setOnAction(e -> delete());
        saveButton.setOnAction(e -> save());
        askButton.setOnAction(e -> askAction.run());
        contentArea.textProperty().addListener((o, old, value) -> {
            if (!loading) dirty.set(true);
            updateMetadata(); stateListener.run();
        });
        contentArea.caretPositionProperty().addListener((o, old, pos) -> stateListener.run());
        dirty.addListener((o, old, value) -> { FxUtil.toggleClass(saveButton, "dirty", value); stateListener.run(); });
    }

    public void configure(NoteService service, Note initial, Instant modified, String model) {
        noteService = service; note = initial; lastEdited = modified == null ? Instant.now() : modified; modelName = model;
        loading = true; contentArea.setText(initial.content()); loading = false;
        dirty.set(false); titleLabel.setText(initial.title()); updateMetadata();
    }

    public void setAskAction(Runnable callback) { askAction = callback; }
    public void setDeletedAction(Runnable callback) { deletedAction = callback; }
    public void setSavedAction(Runnable callback) { savedAction = callback; }
    public void setRenamedAction(Consumer<String> callback) { renamedAction = callback; }
    public void setStateListener(Runnable callback) { stateListener = callback; }

    public Note note() { return note; }
    public String currentContent() { return contentArea.getText(); }
    public boolean isDirty() { return dirty.get(); }
    public BooleanProperty dirtyProperty() { return dirty; }
    public boolean isAssistantOpen() { return assistantOpen; }
    public void focusEditor() { javafx.application.Platform.runLater(contentArea::requestFocus); }

    /** Docks (or removes, when null) the AI panel; the toolbar's Ask button is hidden while the panel is open. */
    public void setAssistantPanel(Node panel) {
        root.setRight(panel);
        assistantOpen = panel != null;
        FxUtil.show(askButton, !assistantOpen);
        stateListener.run();
    }

    /** Inserts AI output at the caret (on its own line) and marks the note dirty. */
    public void insertText(String text) {
        int caret = contentArea.getCaretPosition();
        String before = contentArea.getText();
        String prefix = caret > 0 && before.charAt(caret - 1) != '\n' ? "\n" : "";
        contentArea.insertText(caret, prefix + text);
        contentArea.requestFocus();
    }

    /** {left, right} text for the global bottom status bar. */
    public String[] status() {
        int caret = contentArea.getCaretPosition(), line = 1, column = 1;
        String text = contentArea.getText();
        for (int i = 0; i < caret && i < text.length(); i++) { if (text.charAt(i) == '\n') { line++; column = 1; } else column++; }
        return new String[] { "● UTF-8 • TXT • Ln " + line + ", Col " + column,
                "AI: " + modelName + " • Ctrl+S Save • Ctrl+J Ask AI" };
    }

    public void save() { save(null); }

    private void save(Runnable afterSuccess) {
        String snapshot = contentArea.getText(); Note target = note;
        FxUtil.async(() -> noteService.saveNote(target, snapshot), saved -> {
            note = saved; lastEdited = Instant.now();
            if (snapshot.equals(contentArea.getText())) dirty.set(false);
            titleLabel.setText(note.title()); updateMetadata(); savedAction.run();
            if (afterSuccess != null) afterSuccess.run();
        }, ex -> error(ex.getMessage()));
    }

    /** Runs {@code proceed} once it is safe to close: immediately if clean, after Save/Discard if dirty, never on Cancel. */
    public void requestClose(Runnable proceed) {
        if (!dirty.get()) { proceed.run(); return; }
        Dialogs.unsaved(owner(), "Unsaved changes", "Save changes to “" + note.title() + "” before leaving?", () -> save(proceed), proceed);
    }

    private void rename() {
        Dialogs.prompt(owner(), "Rename note", "Choose a new title for this note.", note.title(), "Rename", value -> {
            Note target = note;
            FxUtil.async(() -> noteService.renameNote(target, value), renamed -> {
                note = renamed; titleLabel.setText(note.title()); renamedAction.accept(note.title());
            }, ex -> error(ex.getMessage()));
        });
    }

    private void delete() {
        Dialogs.confirm(owner(), "Delete note", "“" + note.title() + "” will be permanently deleted. This can’t be undone.", "Delete", true, () -> {
            Note target = note;
            FxUtil.async(() -> { noteService.deleteNote(target); return Boolean.TRUE; }, done -> deletedAction.run(), ex -> error(ex.getMessage()));
        });
    }

    private javafx.stage.Window owner() { return root.getScene() == null ? null : root.getScene().getWindow(); }

    private void setIcon(Button button, String path, String tooltip) {
        StackPane graphic = new StackPane(FxUtil.icon(path)); graphic.setMinSize(16, 16);
        button.setGraphic(graphic); button.setTooltip(new Tooltip(tooltip));
    }

    private void updateMetadata() {
        String value = contentArea.getText() == null ? "" : contentArea.getText();
        long words = FxUtil.wordCount(value);
        metadataLabel.setText(words + " words • " + value.length() + " characters • 🕒 " + FxUtil.readMinutes(words)
                + " min read • Last edited " + FxUtil.relativeTime(lastEdited));
    }

    private void error(String message) { Dialogs.message(owner(), "Something went wrong", message == null ? "Operation failed." : message); }
}
