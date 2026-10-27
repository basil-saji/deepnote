package com.deepnote.fx;

import com.deepnote.model.Note;
import com.deepnote.service.LocalSearchService;
import com.deepnote.service.VectorSearchService;
import com.deepnote.model.VectorSearchResult;
import javafx.application.Platform;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Consumer;

/** Home tab: greeting + responsive note grid (browse state) and a dedicated keyboard-driven search view. */
public final class HomeViewController {
    public enum SearchMode { VECTOR, KEYWORD }
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
    private SearchMode searchMode = SearchMode.VECTOR;
    private VectorSearchService vectorSearchService;
    private List<VectorSearchResult> vectorResults = List.of();
    private int selected;
    private long searchGeneration;
    private final PauseTransition vectorDebounce = new PauseTransition(Duration.millis(180));
    private double searchMillis;
    private Consumer<Note> openNote = note -> { };
    private Consumer<VectorSearchResult> openVectorResult = result -> openNote.accept(new Note(result.title(), result.path(), ""));
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
        searchField.textProperty().addListener((o, old, value) -> { selected = 0; searchGeneration++; render(); });
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
    public void setOpenVectorResult(Consumer<VectorSearchResult> callback) { openVectorResult = callback; }
    public void setCreateNote(Runnable callback) { createNote = callback; }
    public void setVectorSearch(VectorSearchService service) { vectorSearchService = service; searchGeneration++; if (isSearching()) render(); }
    public SearchMode searchMode() { return searchMode; }
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
        if (searchMode == SearchMode.VECTOR) { searchVector(query); return; }
        List<Note> all = items.stream().map(NoteItem::note).toList();
        long started = System.nanoTime();
        List<Note> matched = searchService.search(all, query);
        searchMillis = (System.nanoTime() - started) / 1_000_000.0;

        results = matched.stream().map(note -> byPath.get(note.path())).filter(Objects::nonNull).toList();
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
        int countValue = searchMode == SearchMode.VECTOR ? vectorResults.size() : results.size();
        Label count = new Label(countValue + (countValue == 1 ? " match" : " matches")); count.getStyleClass().add("count-badge");
        Label filterLabel = new Label("FILTER:"); filterLabel.getStyleClass().add("mono-label");
        ToggleGroup group = new ToggleGroup();
        ToggleButton vector = new ToggleButton("Vector"), keyword = new ToggleButton("Keyword");
        vector.setToggleGroup(group); keyword.setToggleGroup(group); (searchMode == SearchMode.VECTOR ? vector : keyword).setSelected(true);
        keepOneSelected(group);
        vector.setOnAction(e -> { searchMode = SearchMode.VECTOR; selected = 0; searchGeneration++; render(); });
        keyword.setOnAction(e -> { searchMode = SearchMode.KEYWORD; selected = 0; searchGeneration++; render(); });
        HBox pills = new HBox(vector, keyword); pills.getStyleClass().add("segmented"); pills.setAlignment(Pos.CENTER);
        HBox row = new HBox(10, heading, count, spacer(), filterLabel, pills); row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void searchVector(String query) {
        vectorResults = List.of();
        collections.getChildren().add(searchHeader());
        if (vectorSearchService == null) { collections.getChildren().add(empty("Vector search is unavailable until PostgreSQL indexing is ready. Choose Keyword or fix the database connection.")); return; }
        collections.getChildren().add(empty("Searching local embeddings…"));
        long generation = searchGeneration;
        vectorDebounce.stop();
        vectorDebounce.setOnFinished(e -> executeVectorSearch(query, generation));
        vectorDebounce.play();
    }

    private void executeVectorSearch(String query, long generation) {
        if (generation != searchGeneration || searchMode != SearchMode.VECTOR) return;
        FxUtil.async(() -> vectorSearchService.search(query, 20), value -> {
            if (generation != searchGeneration || searchMode != SearchMode.VECTOR || !query.equals(searchField.getText().trim())) return;
            vectorResults = value; searchMillis = 0; collections.getChildren().clear(); collections.getChildren().add(searchHeader());
            if (value.isEmpty()) { collections.getChildren().add(empty("No indexed notes match that query.")); return; }
            VBox list = new VBox(14); resultCards.clear();
            for (VectorSearchResult result : value) { Region card = vectorCard(result); resultCards.add(card); list.getChildren().add(card); }
            collections.getChildren().add(list); applySelection();
        }, error -> { if (generation != searchGeneration || searchMode != SearchMode.VECTOR) return; collections.getChildren().clear(); collections.getChildren().add(searchHeader()); collections.getChildren().add(empty("Vector search unavailable: " + error.getMessage())); });
    }

    private Region vectorCard(VectorSearchResult result) {
        Label title = new Label(result.title()); title.getStyleClass().add("card-title");
        Label score = new Label(String.format(Locale.ROOT, "Relevance %.0f%%", result.score() * 100)); score.getStyleClass().add("read-badge");
        Label snippet = new Label(result.chunkText()); snippet.setWrapText(true); snippet.getStyleClass().add("card-preview");
        VBox card = new VBox(8, new HBox(8, title, spacer(), score), snippet); card.getStyleClass().add("result-card"); card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(e -> openVectorResult.accept(result)); return card;
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
                if (event.getTarget() == searchField) { openSelected(); event.consume(); }
            }
            default -> { }
        }
    }

    private void move(int delta) {
        int size = searchMode == SearchMode.VECTOR ? vectorResults.size() : results.size();
        if (size == 0) return;
        selected = Math.max(0, Math.min(size - 1, selected + delta));
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

    private void openSelected() {
        if (searchMode == SearchMode.VECTOR && selected < vectorResults.size()) openVectorResult.accept(vectorResults.get(selected));
        else if (searchMode == SearchMode.KEYWORD && selected < results.size()) openNote.accept(results.get(selected).note());
    }

    private void clearSearch() { searchField.clear(); searchField.requestFocus(); }

    // ---------------------------------------------------------------- small builders

    private static Region spacer() { Region r = new Region(); HBox.setHgrow(r, Priority.ALWAYS); return r; }
    private static Region divider() { Region r = new Region(); r.getStyleClass().add("card-divider"); return r; }
    private static Label empty(String text) { Label l = new Label(text); l.getStyleClass().add("empty-state"); return l; }
}
