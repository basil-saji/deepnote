package com.deepnote.ui;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.model.ChatMessage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Right-side, note-scoped assistant view. */
public class AIAssistantPanel extends JPanel {
    private final JLabel noteLabel = new JLabel();
    private final JPanel messageList = new JPanel();
    private final JScrollPane messageScroll;
    private final JTextArea questionField = new JTextArea(3, 1);
    private final JButton sendButton;
    private final JLabel statusLabel = new JLabel("Ready");
    private final List<ChatMessage> history = new ArrayList<>();
    private final AIAssistantService assistantService;
    private final Supplier<String> contentSupplier;
    private final Runnable collapseAction;
    private String noteTitle;
    private boolean requestRunning;

    public AIAssistantPanel(AIAssistantService assistantService, Supplier<String> contentSupplier, Runnable collapseAction) {
        this.assistantService = assistantService;
        this.contentSupplier = contentSupplier;
        this.collapseAction = collapseAction;
        setBackground(DeepNoteTheme.SURFACE);
        setBorder(DeepNoteTheme.padding(16, 16, 16, 16));
        setLayout(new BorderLayout(0, 14));
        add(buildHeader(), BorderLayout.NORTH);

        messageList.setOpaque(false);
        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageScroll = new JScrollPane(messageList);
        messageScroll.setBorder(null);
        messageScroll.setOpaque(false);
        messageScroll.getViewport().setOpaque(false);
        add(messageScroll, BorderLayout.CENTER);

        sendButton = new JButton("Send", new DeepNoteIcon(DeepNoteIcon.Kind.SEND, Color.WHITE));
        sendButton.setFont(DeepNoteTheme.BODY);
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(DeepNoteTheme.ACCENT);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        sendButton.putClientProperty("JButton.buttonType", "roundRect");
        sendButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel composer = buildComposer();
        add(composer, BorderLayout.SOUTH);
        sendButton.addActionListener(event -> sendQuestion());
        questionField.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "sendQuestion");
        questionField.getActionMap().put("sendQuestion", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { sendQuestion(); }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JPanel titleStack = new JPanel();
        titleStack.setOpaque(false);
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Ask about this note");
        heading.setFont(new Font("Segoe UI", Font.BOLD, 16));
        heading.setForeground(DeepNoteTheme.TEXT);
        noteLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        noteLabel.setForeground(DeepNoteTheme.MUTED);
        titleStack.add(heading);
        titleStack.add(Box.createVerticalStrut(4));
        titleStack.add(noteLabel);
        header.add(titleStack, BorderLayout.CENTER);
        JButton closeButton = new JButton(new DeepNoteIcon(DeepNoteIcon.Kind.CLOSE, DeepNoteTheme.MUTED));
        closeButton.setToolTipText("Collapse assistant");
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        closeButton.addActionListener(event -> collapseAction.run());
        header.add(closeButton, BorderLayout.EAST);
        return header;
    }

    private JPanel buildComposer() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        JPanel inputSurface = new JPanel(new BorderLayout(8, 0));
        inputSurface.setBackground(DeepNoteTheme.ELEVATED);
        inputSurface.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(DeepNoteTheme.BORDER), DeepNoteTheme.padding(8, 10, 8, 8)));
        questionField.setLineWrap(true);
        questionField.setWrapStyleWord(true);
        questionField.setFont(DeepNoteTheme.BODY);
        questionField.setForeground(DeepNoteTheme.TEXT);
        questionField.setBackground(DeepNoteTheme.ELEVATED);
        questionField.setCaretColor(DeepNoteTheme.ACCENT_HOVER);
        questionField.setBorder(null);
        questionField.setToolTipText("Ask a question about this note (Ctrl+Enter to send)");
        inputSurface.add(questionField, BorderLayout.CENTER);
        inputSurface.add(sendButton, BorderLayout.EAST);
        wrapper.add(inputSurface, BorderLayout.NORTH);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(DeepNoteTheme.MUTED);
        footer.add(statusLabel, BorderLayout.WEST);
        JButton newConversation = new JButton("New conversation");
        newConversation.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        newConversation.setForeground(DeepNoteTheme.ACCENT_HOVER);
        newConversation.setContentAreaFilled(false);
        newConversation.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        newConversation.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        newConversation.addActionListener(event -> clearConversation());
        footer.add(newConversation, BorderLayout.EAST);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    public void setNote(String title) {
        noteTitle = title;
        noteLabel.setText(title);
        renderHistory();
    }

    private void sendQuestion() {
        if (requestRunning) return;
        String question = questionField.getText().trim();
        if (question.isBlank()) return;
        List<ChatMessage> snapshot = List.copyOf(history);
        String titleAtRequest = noteTitle;
        String contentAtRequest = contentSupplier.get();
        requestRunning = true;
        sendButton.setEnabled(false);
        questionField.setEnabled(false);
        statusLabel.setForeground(DeepNoteTheme.ACCENT_HOVER);
        statusLabel.setText("Thinking…");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() { return assistantService.answer(titleAtRequest, contentAtRequest, snapshot, question); }

            @Override protected void done() {
                try {
                    if (!titleAtRequest.equals(noteTitle)) return;
                    String answer = get();
                    history.add(new ChatMessage(ChatMessage.Role.USER, question));
                    history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, answer));
                    questionField.setText("");
                    setStatus("Ready", DeepNoteTheme.SUCCESS);
                    renderHistory();
                } catch (Exception exception) {
                    setStatus("Error: " + rootMessage(exception), DeepNoteTheme.DANGER);
                } finally {
                    requestRunning = false;
                    sendButton.setEnabled(true);
                    questionField.setEnabled(true);
                    questionField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void setStatus(String text, Color color) { statusLabel.setText(text); statusLabel.setForeground(color); }
    private void clearConversation() { history.clear(); setStatus("New conversation", DeepNoteTheme.MUTED); renderHistory(); }

    private void renderHistory() {
        messageList.removeAll();
        if (history.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            JLabel icon = new JLabel(new DeepNoteIcon(DeepNoteIcon.Kind.SPARK, DeepNoteTheme.ACCENT_HOVER));
            JLabel title = new JLabel("Your note, in conversation");
            title.setFont(new Font("Segoe UI", Font.BOLD, 14));
            title.setForeground(DeepNoteTheme.TEXT);
            JLabel copy = new JLabel("Ask for a summary, an explanation, or a simpler version.");
            copy.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            copy.setForeground(DeepNoteTheme.MUTED);
            empty.add(icon);
            empty.add(Box.createVerticalStrut(10));
            empty.add(title);
            empty.add(Box.createVerticalStrut(5));
            empty.add(copy);
            messageList.add(empty);
        } else {
            for (ChatMessage message : history) {
                messageList.add(messageBubble(message));
                messageList.add(Box.createVerticalStrut(10));
            }
        }
        messageList.revalidate();
        messageList.repaint();
        SwingUtilities.invokeLater(() -> messageScroll.getVerticalScrollBar().setValue(messageScroll.getVerticalScrollBar().getMaximum()));
    }

    private JPanel messageBubble(ChatMessage message) {
        JPanel bubble = new JPanel(new BorderLayout(0, 5));
        bubble.setOpaque(true);
        boolean user = message.role() == ChatMessage.Role.USER;
        bubble.setBackground(user ? Color.decode("#292849") : DeepNoteTheme.ELEVATED);
        bubble.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(user ? Color.decode("#45427A") : DeepNoteTheme.BORDER), DeepNoteTheme.padding(9, 11, 9, 11)));
        JLabel role = new JLabel(user ? "YOU" : "DEEPNOTE");
        role.setFont(DeepNoteTheme.LABEL);
        role.setForeground(user ? Color.decode("#C5C3FF") : DeepNoteTheme.ACCENT_HOVER);
        JTextArea text = new JTextArea(message.content());
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(DeepNoteTheme.BODY);
        text.setForeground(DeepNoteTheme.TEXT);
        text.setBackground(bubble.getBackground());
        text.setBorder(null);
        bubble.add(role, BorderLayout.NORTH);
        bubble.add(text, BorderLayout.CENTER);
        return bubble;
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
