package com.deepnote.fx;

import com.deepnote.model.Note;
import com.deepnote.service.LocalSearchService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

/** Home tab: greeting + responsive note grid (browse state) and a dedicated keyboard-driven search view. */
public final class HomeViewController {
    private static final double GAP = 16;

    @FXML private VBox root, body, collections;
    @FXML private HBox headerRow, searchBar, controlsRow;
    @FXML private Label greetingLabel, countLabel, shortcutBadge;
    @FXML private StackPane searchIcon;
    @FXML private TextField searchField;
    @FXML private Button newNoteButton, clearButton;
    @FXML private ScrollPane scroll;
    @FXML private ComboBox<String> sortBox;
    @FXML private ToggleButton gridButton, listButton;

    private final LocalSearchService searchService = new LocalSearchService();
    private final List<FlowPane> grids = new ArrayList<>();
    private final List<Region> resultCards = new ArrayList<>();
    private List<NoteItem> items = List.of();
    private Map<Path, NoteItem> byPath = Map.of();
    private List<NoteItem> results = List.of();
    private boolean recentOnly;
    private int selected;
    private double searchMillis;
    private Consumer<Note> openNote = note -> { };
    private Runnable createNote = () -> { };
    private Runnable stateListener = () -> { };

    @FXML private void initialize() {
        greetingLabel.setText(greeting());
        searchIcon.getChildren().add(FxUtil.icon(FxUtil.SEARCH));
        sortBox.getItems().addAll("Last Modified", "Title", "Words");
        sortBox.getSelectionModel().selectFirst();
        sortBox.setOnAction(e -> render());

        ToggleGroup layout = new ToggleGroup();
        gridButton.setToggleGroup(layout); listButton.setToggleGroup(layout); gridButton.setSelected(true);
        keepOneSelected(layout);
        gridButton.setOnAction(e -> render()); listButton.setOnAction(e -> render());

        newNoteButton.setOnAction(e -> createNote.run());
        clearButton.setOnAction(e -> clearSearch());
        searchField.textProperty().addListener((o, old, value) -> { selected = 0; render(); });
        searchField.focusedProperty().addListener((o, old, focused) -> FxUtil.toggleClass(searchBar, "focused", focused));
        scroll.viewportBoundsProperty().addListener((o, old, bounds) -> applyLayout());
        root.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);
        render();
    }

    public void setNotes(List<NoteItem> value) {
        items = List.copyOf(value);
        Map<Path, NoteItem> index = new HashMap<>();
        items.forEach(item -> index.put(item.note().path(), item));
        byPath = index;
        render();
    }
    public void setOpenNote(Consumer<Note> callback) { openNote = callback; }
    public void setCreateNote(Runnable callback) { createNote = callback; }
    public void setStateListener(Runnable callback) { stateListener = callback; }
    public void focusSearch() { Platform.runLater(() -> { searchField.requestFocus(); searchField.selectAll(); }); }

    /** {left, right} text for the global bottom status bar. */
    public String[] status() {
        if (isSearching()) return new String[] {
                "↑ ↓ Navigate  •  ↵ Open result  •  ESC Reset search",
                "Indexed: " + items.size() + " notes  •  Search time: " + String.format(Locale.ROOT, "%.1f", searchMillis) + "ms" };
        return new String[] { "● Local Workspace  |  " + items.size() + " notes", "Shortcuts: Ctrl+N New • Ctrl+K Search" };
    }

    private boolean isSearching() { return !searchField.getText().isBlank(); }
    private boolean listMode() { return listButton.isSelected(); }

    private static String greeting() {
        int hour = LocalTime.now().getHour();
        return (hour < 12 ? "Good Morning" : hour < 18 ? "Good Afternoon" : "Good Evening") + " 👋";
    }
    private static void keepOneSelected(ToggleGroup group) {
        group.selectedToggleProperty().addListener((o, old, now) -> { if (now == null && old != null) old.setSelected(true); });
    }

    // ---------------------------------------------------------------- rendering

    private void render() {
        boolean searching = isSearching();
        FxUtil.show(headerRow, !searching);
        FxUtil.show(controlsRow, !searching);
        FxUtil.show(clearButton, searching);
        FxUtil.toggleClass(searchBar, "active", searching);
        shortcutBadge.setText(searching ? "ESC" : "Ctrl+K");
        collections.getChildren().clear(); grids.clear(); resultCards.clear();
        if (searching) renderSearch(); else renderBrowse();
        applyLayout();
        stateListener.run();
    }

    private void renderBrowse() {
        countLabel.setText(items.size() + (items.size() == 1 ? " note" : " notes"));
        if (items.isEmpty()) { collections.getChildren().add(empty("No notes yet. Create your first note to get started.")); return; }
        if (items.size() > 3) {
            List<NoteItem> recent = items.stream().sorted(Comparator.comparing(NoteItem::modified).reversed()).limit(3).toList();
            collections.getChildren().add(section("RECENTS", recent));
        }
        collections.getChildren().add(section("NOTES (" + items.size() + " total)", sorted(items)));
    }

    private List<NoteItem> sorted(List<NoteItem> source) {
        Comparator<NoteItem> order = switch (String.valueOf(sortBox.getValue())) {
            case "Title" -> Comparator.comparing(item -> item.note().title().toLowerCase(Locale.ROOT));
            case "Words" -> Comparator.comparingLong(NoteItem::words).reversed();
            default -> Comparator.comparing(NoteItem::modified).reversed();
        };
        return source.stream().sorted(order).toList();
    }

    private VBox section(String label, List<NoteItem> list) {
        Label heading = new Label(label); heading.getStyleClass().add("section-label");
        FlowPane grid = new FlowPane(GAP, GAP); grids.add(grid);
        list.forEach(item -> grid.getChildren().add(card(item)));
        return new VBox(12, heading, grid);
    }

    private Region card(NoteItem item) {
        Label badge = new Label(".TXT"); badge.getStyleClass().add("txt-badge");
        Label time = new Label(FxUtil.relativeTime(item.modified())); time.getStyleClass().add("mono-label");
        HBox top = new HBox(8, badge, spacer(), time); top.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label(item.note().title()); title.getStyleClass().add("card-title"); title.setMaxWidth(Double.MAX_VALUE);
        String flat = item.note().content().replaceAll("\\s+", " ").trim();
        Label preview = new Label(flat.isBlank() ? "Empty note" : flat);
        preview.getStyleClass().add("card-preview"); preview.setWrapText(true); preview.setMaxWidth(Double.MAX_VALUE);
        preview.setMinHeight(54); preview.setPrefHeight(54); preview.setMaxHeight(54); preview.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(preview, Priority.ALWAYS);

        Label words = new Label(item.words() + " words"); words.getStyleClass().add("mono-label");
        Label read = new Label(item.minutesToRead() + " min read"); read.getStyleClass().add("read-badge");
        HBox footer = new HBox(8, words, spacer(), read); footer.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(10, top, title, preview, divider(), footer);
        card.getStyleClass().add("note-card"); card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> openNote.accept(item.note()));
        return card;
    }

    private void renderSearch() {
        String query = searchField.getText().trim();
        List<Note> all = items.stream().map(NoteItem::note).toList();
        long started = System.nanoTime();
        List<Note> matched = searchService.search(all, query);
        searchMillis = (System.nanoTime() - started) / 1_000_000.0;

        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        results = matched.stream().map(note -> byPath.get(note.path())).filter(Objects::nonNull)
                .filter(item -> !recentOnly || item.modified().isAfter(cutoff)).toList();
        if (selected >= results.size()) selected = Math.max(0, results.size() - 1);

        collections.getChildren().add(searchHeader());
        if (results.isEmpty()) { collections.getChildren().add(empty("No notes match that search. Try another title or keyword.")); return; }
        VBox list = new VBox(14);
        for (NoteItem item : results) { Region card = resultCard(item, query); resultCards.add(card); list.getChildren().add(card); }
        collections.getChildren().add(list);
        applySelection();
    }

    private Node searchHeader() {
        Label heading = new Label("Search results:"); heading.getStyleClass().add("search-heading");
        Label count = new Label(results.size() + (results.size() == 1 ? " match" : " matches")); count.getStyleClass().add("count-badge");
        Label filterLabel = new Label("FILTER:"); filterLabel.getStyleClass().add("mono-label");
        ToggleGroup group = new ToggleGroup();
        ToggleButton all = new ToggleButton("All Notes"), recent = new ToggleButton("Recent Only");
        all.setToggleGroup(group); recent.setToggleGroup(group); (recentOnly ? recent : all).setSelected(true);
        keepOneSelected(group);
        all.setOnAction(e -> { recentOnly = false; selected = 0; render(); });
        recent.setOnAction(e -> { recentOnly = true; selected = 0; render(); });
        HBox pills = new HBox(all, recent); pills.getStyleClass().add("segmented"); pills.setAlignment(Pos.CENTER);
        HBox row = new HBox(10, heading, count, spacer(), filterLabel, pills); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Region resultCard(NoteItem item, String query) {
        StackPane badge = new StackPane(FxUtil.icon(FxUtil.DOC)); badge.getStyleClass().add("icon-badge");
        badge.setMinSize(26, 26); badge.setPrefSize(26, 26); badge.setMaxSize(26, 26);
        TextFlow title = FxUtil.highlight(item.note().title(), query, "title-text");
        Label time = new Label("Updated " + FxUtil.relativeTime(item.modified())); time.getStyleClass().add("mono-label");
        HBox top = new HBox(10, badge, title, time); top.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(title, Priority.ALWAYS);

        TextFlow snippet = FxUtil.highlight(FxUtil.snippet(item.note().content(), query), query, "snippet-text");
        Label meta = new Label("📖 " + item.words() + " words   •   " + item.minutesToRead() + " min read   •   " + item.characters() + " characters");
        meta.getStyleClass().add("meta-text");

        VBox card = new VBox(12, top, snippet, divider(), meta);
        card.getStyleClass().add("result-card"); card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> openNote.accept(item.note()));
        return card;
    }

    // ---------------------------------------------------------------- responsive grid

    /** 3 columns at >=1020px, 2 at 660-1019px, 1 below (or in list mode); card width fills the row exactly. */
    private void applyLayout() {
        double available = Math.floor(scroll.getViewportBounds().getWidth());
        if (available <= 0) return;
        int columns = listMode() ? 1 : available >= 1020 ? 3 : available >= 660 ? 2 : 1;
        double width = Math.floor((available - GAP * (columns - 1)) / columns);
        for (FlowPane grid : grids) {
            grid.setPrefWrapLength(available);
            for (Node child : grid.getChildren()) {
                Region card = (Region) child;
                card.setMinWidth(width); card.setPrefWidth(width); card.setMaxWidth(width);
            }
        }
    }

    // ---------------------------------------------------------------- keyboard & selection

    private void onKey(KeyEvent event) {
        if (!isSearching()) return;
        switch (event.getCode()) {
            case DOWN -> { move(1); event.consume(); }
            case UP -> { move(-1); event.consume(); }
            case ESCAPE -> { clearSearch(); event.consume(); }
            case ENTER -> {
                if (event.getTarget() == searchField && !results.isEmpty()) { openNote.accept(results.get(selected).note()); event.consume(); }
            }
            default -> { }
        }
    }

    private void move(int delta) {
        if (results.isEmpty()) return;
        selected = Math.max(0, Math.min(results.size() - 1, selected + delta));
        applySelection();
        ensureVisible(resultCards.get(selected));
    }

    private void applySelection() {
        for (int i = 0; i < resultCards.size(); i++) FxUtil.toggleClass(resultCards.get(i), "selected", i == selected);
    }

    private void ensureVisible(Node node) {
        if (node.getScene() == null) return;
        Bounds target = body.sceneToLocal(node.localToScene(node.getBoundsInLocal()));
        double view = scroll.getViewportBounds().getHeight(), range = body.getHeight() - view;
        if (target == null || range <= 0) return;
        double top = scroll.getVvalue() * range;
        if (target.getMinY() < top) scroll.setVvalue(Math.max(0, (target.getMinY() - 8) / range));
        else if (target.getMaxY() > top + view) scroll.setVvalue(Math.min(1, (target.getMaxY() + 8 - view) / range));
    }

    private void clearSearch() { recentOnly = false; searchField.clear(); searchField.requestFocus(); }

    // ---------------------------------------------------------------- small builders

    private static Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
    private static Region divider() { Region r = new Region(); r.getStyleClass().add("card-divider"); return r; }
    private static Label empty(String text) { Label l = new Label(text); l.getStyleClass().add("empty-state"); return l; }
}
