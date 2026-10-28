package com.deepnote.fx;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.config.AppConfig;
import com.deepnote.model.ChatMessage;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Note-scoped local AI assistant docked beside the editor. */
public final class AssistantPanelController {
    private record Reply(String text, long millis) { }

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label modelPill, statusLabel, contextPill;
    @FXML private VBox welcome, messageBox, suggestions, composerBox;
    @FXML private ScrollPane scroll;
    @FXML private TextArea composerInput;
    @FXML private Button sendButton, clearButton, closeButton;

    private AIAssistantService service;
    private Supplier<String> title = () -> "", content = () -> "";
    private Consumer<String> insertHandler = text -> { };
    private Runnable closeHandler = () -> { };
    private List<ChatMessage> history = new ArrayList<>();
    private boolean running;

    @FXML private void initialize() {
        for (String prompt : List.of("Summarize this note", "Explain it simply", "List key takeaways", "What details are missing?")) {
            Button button = new Button(prompt); button.getStyleClass().add("suggestion-button"); button.setMaxWidth(Double.MAX_VALUE);
            button.setOnAction(e -> { composerInput.setText(prompt); send(); });
            suggestions.getChildren().add(button);
        }
        sendButton.setOnAction(e -> send());
        clearButton.setOnAction(e -> clearChat());
        closeButton.setOnAction(e -> closeHandler.run());
        clearButton.setTooltip(new Tooltip("Clear chat")); closeButton.setTooltip(new Tooltip("Close panel"));
        composerInput.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER && !e.isShiftDown()) { e.consume(); send(); } });
        composerInput.focusedProperty().addListener((o, old, focused) -> FxUtil.toggleClass(composerBox, "focus-within", focused));
        messageBox.heightProperty().addListener((o, old, height) -> scroll.setVvalue(1.0));
    }

    public void configure(AppConfig config, AIAssistantService assistant, Supplier<String> noteTitle, Supplier<String> noteContent,
                          List<ChatMessage> session, Consumer<String> insert, Runnable close) {
        service = assistant; title = noteTitle; content = noteContent; history = session; insertHandler = insert; closeHandler = close;
        modelPill.setText(config.chatModelName());
        contextPill.setText("@" + noteTitle.get());
        for (ChatMessage message : history) {
            messageBox.getChildren().add(message.role() == ChatMessage.Role.USER ? userBubble(message.content(), null) : assistantCard(message.content(), -1));
        }
        FxUtil.show(welcome, history.isEmpty());
        checkConnection(config);
        composerInput.requestFocus();
    }

    // ---------------------------------------------------------------- sending

    private void send() {
        String prompt = composerInput.getText().trim();
        if (prompt.isBlank() || running) return;
        running = true; sendButton.setDisable(true); composerInput.clear(); FxUtil.show(welcome, false);
        messageBox.getChildren().add(userBubble(prompt, LocalTime.now()));
        Node thinking = thinkingCard(); messageBox.getChildren().add(thinking);
        String noteTitle = title.get(), noteContent = content.get();
        List<ChatMessage> snapshot = new ArrayList<>(history);
        contextPill.setText("@" + noteTitle);
        FxUtil.async(() -> {
            long started = System.nanoTime();
            String answer = service.answer(noteTitle, noteContent, snapshot, prompt);
            return new Reply(answer, (System.nanoTime() - started) / 1_000_000);
        }, reply -> {
            messageBox.getChildren().remove(thinking);
            history.add(new ChatMessage(ChatMessage.Role.USER, prompt));
            history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, reply.text()));
            messageBox.getChildren().add(assistantCard(reply.text(), reply.millis()));
            setOnline(true); finish();
        }, error -> {
            messageBox.getChildren().remove(thinking);
            messageBox.getChildren().add(errorCard(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage()));
            composerInput.setText(prompt);
            finish();
        });
    }

    private void finish() { running = false; sendButton.setDisable(false); composerInput.requestFocus(); }

    private void clearChat() {
        history.clear();
        messageBox.getChildren().removeIf(node -> node != welcome);
        FxUtil.show(welcome, true);
    }

    // ---------------------------------------------------------------- message nodes

    private Node userBubble(String text, LocalTime time) {
        Label meta = new Label(time == null ? "You" : "You • " + time.format(CLOCK)); meta.getStyleClass().add("chat-meta");
        Label bubble = new Label(text); bubble.getStyleClass().add("user-bubble"); bubble.setWrapText(true); bubble.setMaxWidth(260);
        VBox box = new VBox(4, meta, bubble); box.setAlignment(Pos.CENTER_RIGHT); box.setFillWidth(false);
        return box;
    }

    private Node assistantCard(String text, long millis) {
        Label who = new Label("✦ DeepNote AI"); who.getStyleClass().add("chat-meta");
        Region gap = new Region(); HBox.setHgrow(gap, Priority.ALWAYS);
        HBox meta = new HBox(8, who, gap); meta.setAlignment(Pos.CENTER_LEFT);
        if (millis >= 0) { Label took = new Label("Thought in " + millis + "ms"); took.getStyleClass().add("chat-meta"); meta.getChildren().add(took); }

        VBox body = MarkdownView.render(text);
        Button insert = new Button("+ Insert to note"); insert.getStyleClass().add("copy-button");
        insert.setOnAction(e -> { insertHandler.accept(text); flash(insert, "✓ Inserted", "+ Insert to note"); });
        Button copy = new Button("⧉ Copy"); copy.getStyleClass().add("copy-button");
        copy.setOnAction(e -> {
            ClipboardContent clip = new ClipboardContent(); clip.putString(text); Clipboard.getSystemClipboard().setContent(clip);
            flash(copy, "✓ Copied", "⧉ Copy");
        });
        HBox actions = new HBox(8, insert, copy);
        VBox card = new VBox(10, body, actions); card.getStyleClass().add("ai-card");
        return new VBox(4, meta, card);
    }

    private Node thinkingCard() {
        Label who = new Label("✦ DeepNote AI"); who.getStyleClass().add("chat-meta");
        Label body = new Label("Thinking…"); body.getStyleClass().add("ai-thinking");
        VBox card = new VBox(body); card.getStyleClass().add("ai-card");
        return new VBox(4, who, card);
    }

    private Node errorCard(String message) {
        Label who = new Label("LOCAL MODEL ERROR"); who.getStyleClass().add("chat-meta");
        Label body = new Label(message); body.getStyleClass().add("ai-text"); body.setWrapText(true); body.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(body); card.getStyleClass().addAll("ai-card", "error");
        return new VBox(4, who, card);
    }

    private void flash(Button button, String temporary, String original) {
        button.setText(temporary);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.2));
        pause.setOnFinished(e -> button.setText(original)); pause.play();
    }

    // ---------------------------------------------------------------- Ollama status

    private void checkConnection(AppConfig config) {
        statusLabel.setText("● Checking…"); setStatusStyle(false);
        String base = config.ollamaBaseUrl().replaceAll("/+$", "");
        FxUtil.async(() -> {
            HttpClient client = HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(2)).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/api/tags")).timeout(java.time.Duration.ofSeconds(2)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode() == 200;
        }, this::setOnline, error -> setOnline(false));
    }

    private void setOnline(boolean online) { statusLabel.setText(online ? "● Connected" : "● Offline"); setStatusStyle(online); }

    private void setStatusStyle(boolean online) {
        statusLabel.getStyleClass().removeAll("status-online", "status-offline");
        statusLabel.getStyleClass().add(online ? "status-online" : "status-offline");
    }
}
