package com.deepnote.fx;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.model.ChatMessage;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** FXML-backed, note-scoped local AI assistant. */
public final class AssistantPanelController {
    @FXML private Label statusLabel, thinkingLabel;
    @FXML private VBox welcome;
    @FXML private FlowPane suggestions;
    @FXML private ListView<String> messages;
    @FXML private TextArea composer;
    @FXML private Button sendButton, clearButton, closeButton;
    private AIAssistantService service;
    private String title;
    private Supplier<String> content = () -> "";
    private List<ChatMessage> history;
    private boolean running;

    @FXML private void initialize() {
        for (String suggestion : List.of("Summarize this note", "Explain it simply", "List key takeaways", "What details are missing?")) {
            Button button = new Button(suggestion); button.getStyleClass().add("suggestion-button");
            button.setOnAction(e -> { composer.setText(suggestion); send(); }); suggestions.getChildren().add(button);
        }
        sendButton.setOnAction(e -> send()); clearButton.setOnAction(e -> { history.clear(); messages.getItems().clear(); welcome.setVisible(true); welcome.setManaged(true); });
        composer.setOnKeyPressed(e -> { if (e.getCode() == javafx.scene.input.KeyCode.ENTER && !e.isShiftDown()) { e.consume(); send(); } });
        closeButton.setOnAction(e -> { if (closeHandler != null) closeHandler.run(); });
    }
    private Runnable closeHandler;
    public void configure(AIAssistantService assistant, String noteTitle, Supplier<String> noteContent, List<ChatMessage> session, Runnable close) {
        service = assistant; title = noteTitle; content = noteContent; history = session; closeHandler = close; statusLabel.setText("Local model · request on demand"); refresh();
    }
    private void refresh() { messages.getItems().clear(); history.forEach(m -> messages.getItems().add(label(m))); welcome.setVisible(history.isEmpty()); welcome.setManaged(history.isEmpty()); }
    private String label(ChatMessage message) { return (message.role() == ChatMessage.Role.USER ? "YOU\n" : "DEEPNOTE AI\n") + message.content(); }
    private void send() {
        String prompt = composer.getText().trim(); if (prompt.isBlank() || running) return; running = true; sendButton.setDisable(true); composer.clear(); messages.getItems().add("YOU\n" + prompt); thinkingLabel.setText("Thinking…"); welcome.setVisible(false); welcome.setManaged(false);
        String capturedContent = content.get();
        Task<String> task = new Task<>() { @Override protected String call() { return service.answer(title, capturedContent, history, prompt); } };
        task.setOnSucceeded(e -> { String answer = task.getValue(); history.add(new ChatMessage(ChatMessage.Role.USER, prompt)); history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, answer)); messages.getItems().add("DEEPNOTE AI\n" + answer); finish(""); });
        task.setOnFailed(e -> { messages.getItems().add("LOCAL MODEL ERROR\n" + task.getException().getMessage()); composer.setText(prompt); finish("Request failed · retry available"); });
        Thread worker = new Thread(task, "deepnote-ollama"); worker.setDaemon(true); worker.start();
    }
    private void finish(String state) { running = false; sendButton.setDisable(false); thinkingLabel.setText(state); }
}
