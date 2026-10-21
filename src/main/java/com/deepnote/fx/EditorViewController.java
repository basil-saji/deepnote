package com.deepnote.fx;

import com.deepnote.exception.NoteStorageException;
import com.deepnote.model.Note;
import com.deepnote.service.NoteService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.Optional;
import java.util.function.Consumer;

/** FXML-backed plain-text editor and note toolbar. */
public final class EditorViewController {
    @FXML private Label titleLabel, metadataLabel;
    @FXML private TextArea editor;
    @FXML private HBox actions;
    private NoteService noteService;
    private Note note;
    private boolean dirty;
    private Consumer<EditorViewController> askAction = ignored -> { };
    private Consumer<String> renamedAction = ignored -> { };
    private Runnable deletedAction = () -> { };

    @FXML private void initialize() { editor.textProperty().addListener((o, old, value) -> { dirty = true; updateMetadata(value); }); }
    public void configure(NoteService service, Note initial, Consumer<EditorViewController> ask, Runnable deleted) {
        noteService = service; note = initial; askAction = ask; deletedAction = deleted; editor.setText(initial.content()); dirty = false; titleLabel.setText(initial.title()); updateMetadata(initial.content());
        Button rename = button("Rename", "secondary-button", "Rename note"); Button save = button("Save", "secondary-button", "Save note (Ctrl+S)"); Button delete = button("Delete", "danger-button", "Delete note"); Button askButton = button("Ask about this note", "primary-button", "Ask DeepNote about the active note");
        rename.setOnAction(e -> rename()); save.setOnAction(e -> save()); delete.setOnAction(e -> delete()); askButton.setOnAction(e -> askAction.accept(this)); actions.getChildren().setAll(rename, save, delete, askButton);
    }
    public void setRenamedAction(Consumer<String> callback) { renamedAction = callback; }
    public Note note() { return note; }
    public String currentContent() { return editor.getText(); }
    public boolean isDirty() { return dirty; }
    public void save() { try { note = noteService.saveNote(note, editor.getText()); dirty = false; titleLabel.setText(note.title()); updateMetadata(editor.getText()); } catch (NoteStorageException ex) { error(ex.getMessage()); } }
    public boolean confirmClose() {
        if (!dirty) return true;
        Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, "Save changes to “" + note.title() + "” before closing?", ButtonType.CANCEL, ButtonType.NO, ButtonType.YES);
        Optional<ButtonType> answer = dialog.showAndWait(); if (answer.isEmpty() || answer.get() == ButtonType.CANCEL) return false; if (answer.get() == ButtonType.YES) { save(); return !dirty; } return true;
    }
    private void rename() { TextInputDialog dialog = new TextInputDialog(note.title()); dialog.setTitle("Rename note"); dialog.setHeaderText("Choose a new note title"); dialog.showAndWait().ifPresent(value -> { try { note = noteService.renameNote(note, value); titleLabel.setText(note.title()); renamedAction.accept(note.title()); } catch (NoteStorageException ex) { error(ex.getMessage()); } }); }
    private void delete() { Alert dialog = new Alert(Alert.AlertType.CONFIRMATION, "Delete “" + note.title() + "”?", ButtonType.CANCEL, ButtonType.OK); dialog.showAndWait().filter(ButtonType.OK::equals).ifPresent(ok -> { try { noteService.deleteNote(note); deletedAction.run(); } catch (NoteStorageException ex) { error(ex.getMessage()); } }); }
    private Button button(String text, String style, String tooltip) { Button button = new Button(text); button.getStyleClass().add(style); button.setTooltip(new Tooltip(tooltip)); return button; }
    private void updateMetadata(String value) { long words = java.util.Arrays.stream(value.trim().split("\\s+")).filter(s -> !s.isBlank()).count(); metadataLabel.setText(words + " words  ·  " + value.length() + " characters  ·  UTF-8 TXT  ·  " + (dirty ? "Unsaved changes" : "Saved")); }
    private void error(String message) { new Alert(Alert.AlertType.ERROR, message, ButtonType.OK).showAndWait(); }
}
