package com.deepnote.fx;

import com.deepnote.model.Note;
import com.deepnote.service.LocalSearchService;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.beans.binding.Bindings;

import java.util.List;
import java.util.function.Consumer;

/** FXML-backed Home tab: local search and live TXT note collections. */
public final class HomeViewController {
    @FXML private VBox root, collections;
    @FXML private TextField searchField;
    @FXML private ToggleButton gridButton, listButton;
    @FXML private Button newNoteButton;
    private final LocalSearchService searchService = new LocalSearchService();
    private List<Note> notes = List.of();
    private Consumer<Note> openNote = note -> { };
    private Runnable createNote = () -> { };

    @FXML private void initialize() {
        ToggleGroup group = new ToggleGroup(); gridButton.setToggleGroup(group); listButton.setToggleGroup(group); gridButton.setSelected(true);
        searchField.textProperty().addListener((o, old, value) -> render());
        gridButton.setOnAction(e -> render()); listButton.setOnAction(e -> render()); newNoteButton.setOnAction(e -> createNote.run());
    }
    public void setNotes(List<Note> value) { notes = List.copyOf(value); render(); }
    public void setOpenNote(Consumer<Note> callback) { openNote = callback; }
    public void setCreateNote(Runnable callback) { createNote = callback; }
    public TextField searchField() { return searchField; }

    private void render() {
        List<Note> filtered = searchService.search(notes, searchField.getText()); collections.getChildren().clear();
        if (filtered.isEmpty()) { Label empty = new Label(searchField.getText().isBlank() ? "No notes yet. Create your first note to get started." : "No notes match that search. Try another title or keyword."); empty.getStyleClass().add("empty-state"); collections.getChildren().add(empty); return; }
        boolean listMode = listButton.isSelected();
        if (filtered.size() > 3 && searchField.getText().isBlank()) collections.getChildren().add(section("RECENTS", filtered.subList(0, 3), listMode));
        collections.getChildren().add(section(searchField.getText().isBlank() ? "NOTES  ·  " + filtered.size() : "SEARCH RESULTS  ·  " + filtered.size(), filtered, listMode));
    }
    private VBox section(String label, List<Note> items, boolean listMode) {
        Label heading = new Label(label); heading.getStyleClass().add("section-label"); FlowPane cards = new FlowPane(14, 14); cards.getStyleClass().add(listMode ? "note-list" : "note-grid");
        items.forEach(note -> cards.getChildren().add(card(note, listMode))); VBox box = new VBox(12, heading, cards); VBox.setMargin(box, new Insets(0, 0, 4, 0)); return box;
    }
    private Node card(Note note, boolean listMode) {
        VBox card = new VBox(9); card.getStyleClass().add("note-card"); if (listMode) card.getStyleClass().add("list-card");
        Label tag = new Label("TXT  ·  NOTE"); tag.getStyleClass().add("card-tag"); Label title = new Label(note.title()); title.getStyleClass().add("card-title");
        String preview = note.content().replaceAll("\\s+", " ").trim(); Label body = new Label(preview.isBlank() ? "Empty note" : preview); body.setWrapText(true); body.setMaxHeight(48); body.getStyleClass().add("card-preview");
        Label meta = new Label(wordCount(note.content()) + " words"); meta.getStyleClass().add("mono-label"); card.getChildren().addAll(tag, title, body, meta);
        if (listMode) card.prefWidthProperty().bind(root.widthProperty().subtract(72));
        else card.prefWidthProperty().bind(Bindings.max(220, Bindings.min(380, root.widthProperty().subtract(100).divide(3))));
        card.setOnMouseClicked(e -> openNote.accept(note)); return card;
    }
    private long wordCount(String value) { return java.util.Arrays.stream(value.trim().split("\\s+")).filter(s -> !s.isBlank()).count(); }
}
