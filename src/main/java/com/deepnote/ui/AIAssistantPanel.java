package com.deepnote.ui;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.model.ChatMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Right-side, note-scoped assistant: a calm chat view with suggestions, bubbles and a rounded composer. */
public class AIAssistantPanel extends JPanel {
    private static final String[][] SUGGESTIONS = {
            {"Summarize this note", "Summarize this note in a few sentences."},
            {"Explain it simply", "Explain this note in simple terms."},
            {"List the key points", "List the key points of this note as short items."},
            {"What details are missing?", "What important details seem to be missing from this note?"}
    };

    private final JLabel noteLabel = new JLabel();
    private final ScrollableColumn messageList = new ScrollableColumn(null, false);
    private final JScrollPane messageScroll;
    private final JTextArea questionField;
    private final ModernButton sendButton = ModernButton.iconOnly(
            DeepNoteIcon.Kind.SEND, "Send (Enter)", ModernButton.Variant.PRIMARY, 16);
    private final JLabel statusLabel = new JLabel("Ready");
    private final RoundedPanel composer = new RoundedPanel(new BorderLayout(8, 0), 18,
            DeepNoteTheme.RAISED, DeepNoteTheme.BORDER);
    private final List<ChatMessage> history = new ArrayList<>();
    private final AIAssistantService assistantService;
    private final Supplier<String> contentSupplier;
    private final Runnable collapseAction;
    private final Timer thinkingTimer;
    private JLabel thinkingLabel;
    private int thinkingStep;
    private String noteTitle = "";
    private String pendingQuestion;
    private String lastError;
    private boolean requestRunning;

    public AIAssistantPanel(AIAssistantService assistantService, Supplier<String> contentSupplier,
                            Runnable collapseAction) {
        this.assistantService = assistantService;
        this.contentSupplier = contentSupplier;
        this.collapseAction = collapseAction;
        questionField = new JTextArea(2, 1) {
            @Override protected void paintComponent(Graphics graphics) {
                super.paintComponent(graphics);
                if (getDocument().getLength() > 0) return;
                Graphics2D g = (Graphics2D) graphics.create();
                DeepNoteTheme.smooth(g);
                g.setColor(DeepNoteTheme.FAINT);
                g.setFont(getFont());
                g.drawString("Ask anything about this note…", getInsets().left,
                        getInsets().top + g.getFontMetrics().getAscent());
                g.dispose();
            }
        };
        setBackground(DeepNoteTheme.SIDEBAR);
        setLayout(new BorderLayout());
        add(buildHeader(), BorderLayout.NORTH);

        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageList.setBorder(DeepNoteTheme.padding(18, 18, 8, 18));
        messageScroll = new JScrollPane(messageList);
        messageScroll.setBorder(null);
        messageScroll.setOpaque(false);
        messageScroll.getViewport().setOpaque(false);
        messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(messageScroll, BorderLayout.CENTER);
        add(buildComposer(), BorderLayout.SOUTH);

        thinkingTimer = new Timer(380, event -> animateThinking());
        sendButton.addActionListener(event -> sendQuestion());
        bindComposerKeys();
        renderHistory();
    }

    public void setNote(String title) {
        noteTitle = title;
        noteLabel.setText(title);
    }

    public void focusInput() { questionField.requestFocusInWindow(); }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createCompoundBorder(
                DeepNoteTheme.divider(0, 0, 1, 0), DeepNoteTheme.padding(14, 18, 14, 12)));
        IconBadge avatar = new IconBadge(DeepNoteIcon.Kind.SPARK, 32, false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel heading = new JLabel("Ask AI");
        heading.setFont(DeepNoteTheme.TITLE_SM);
        heading.setForeground(DeepNoteTheme.TEXT);
        noteLabel.setFont(DeepNoteTheme.SMALL);
        noteLabel.setForeground(DeepNoteTheme.MUTED);
        titles.add(heading);
        titles.add(noteLabel);
        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);
        left.add(avatar, BorderLayout.WEST);
        left.add(titles, BorderLayout.CENTER);
        ModernButton close = ModernButton.iconOnly(DeepNoteIcon.Kind.CLOSE, "Collapse assistant",
                ModernButton.Variant.GHOST, 16);
        close.addActionListener(event -> collapseAction.run());
        header.add(left, BorderLayout.CENTER);
        header.add(close, BorderLayout.EAST);
        return header;
    }

    private JPanel buildComposer() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.setOpaque(false);
        wrapper.setBorder(DeepNoteTheme.padding(6, 14, 12, 14));

        questionField.setLineWrap(true);
        questionField.setWrapStyleWord(true);
        questionField.setFont(DeepNoteTheme.CHAT);
        questionField.setForeground(DeepNoteTheme.TEXT);
        questionField.setBackground(DeepNoteTheme.RAISED);
        questionField.setOpaque(false);
        questionField.setBorder(null);
        questionField.setToolTipText("Enter to send, Shift+Enter for a new line");
        questionField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { composer.setStroke(DeepNoteTheme.ACCENT); }
            @Override public void focusLost(FocusEvent event) { composer.setStroke(DeepNoteTheme.BORDER); }
        });
        JPanel sendHolder = new JPanel(new BorderLayout());
        sendHolder.setOpaque(false);
        sendHolder.add(sendButton, BorderLayout.SOUTH);
        composer.setBorder(DeepNoteTheme.padding(10, 14, 8, 8));
        composer.add(questionField, BorderLayout.CENTER);
        composer.add(sendHolder, BorderLayout.EAST);
        wrapper.add(composer, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(DeepNoteTheme.padding(0, 4, 0, 0));
        statusLabel.setFont(DeepNoteTheme.SMALL);
        statusLabel.setForeground(DeepNoteTheme.MUTED);
        ModernButton newChat = new ModernButton("New chat", null, ModernButton.Variant.GHOST);
        newChat.setFont(DeepNoteTheme.SMALL_MEDIUM);
        newChat.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
        newChat.addActionListener(event -> clearConversation());
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(newChat, BorderLayout.EAST);
        wrapper.add(footer, BorderLayout.SOUTH);
        return wrapper;
    }

    private void bindComposerKeys() {
        InputMap input = questionField.getInputMap();
        ActionMap actions = questionField.getActionMap();
        Action send = new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent event) { sendQuestion(); }
        };
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "sendQuestion");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "sendQuestion");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK), "insert-break");
        actions.put("sendQuestion", send);
    }

    private void sendQuestion() {
        if (requestRunning) return;
        String question = questionField.getText().trim();
        if (question.isBlank()) return;
        List<ChatMessage> snapshot = List.copyOf(history);
        String titleAtRequest = noteTitle;
        String contentAtRequest = contentSupplier.get();
        requestRunning = true;
        pendingQuestion = question;
        lastError = null;
        questionField.setText("");
        questionField.setEditable(false);
        sendButton.setEnabled(false);
        setStatus("Thinking…", DeepNoteTheme.ACCENT_TEXT);
        renderHistory();
        thinkingTimer.start();
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return assistantService.answer(titleAtRequest, contentAtRequest, snapshot, question);
            }

            @Override protected void done() {
                thinkingTimer.stop();
                requestRunning = false;
                pendingQuestion = null;
                try {
                    String answer = get();
                    history.add(new ChatMessage(ChatMessage.Role.USER, question));
                    history.add(new ChatMessage(ChatMessage.Role.ASSISTANT, answer));
                    setStatus("Ready", DeepNoteTheme.MUTED);
                } catch (Exception exception) {
                    lastError = rootMessage(exception);
                    questionField.setText(question);
                    setStatus("Request failed", DeepNoteTheme.DANGER);
                } finally {
                    questionField.setEditable(true);
                    sendButton.setEnabled(true);
                    renderHistory();
                    questionField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    private void animateThinking() {
        if (thinkingLabel == null) return;
        thinkingStep = (thinkingStep + 1) % 4;
        thinkingLabel.setText("Thinking" + ".".repeat(thinkingStep));
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void clearConversation() {
        if (requestRunning) return;
        history.clear();
        lastError = null;
        setStatus("New chat", DeepNoteTheme.MUTED);
        renderHistory();
    }

    private void renderHistory() {
        messageList.removeAll();
        thinkingLabel = null;
        if (history.isEmpty() && pendingQuestion == null && lastError == null) {
            messageList.add(emptyState());
        } else {
            for (ChatMessage message : history) addRow(message.role() == ChatMessage.Role.USER
                    ? userRow(message.content()) : assistantRow(message.content()));
            if (pendingQuestion != null) {
                addRow(userRow(pendingQuestion));
                addRow(thinkingRow());
            }
            if (lastError != null) addRow(errorRow(lastError));
        }
        messageList.revalidate();
        messageList.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = messageScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void addRow(JComponent row) {
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        messageList.add(row);
        messageList.add(Box.createVerticalStrut(16));
    }

    private JComponent emptyState() {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.setBorder(DeepNoteTheme.padding(28, 0, 0, 0));
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        IconBadge badge = new IconBadge(DeepNoteIcon.Kind.SPARK, 48, true);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Chat with this note");
        title.setFont(DeepNoteTheme.TITLE_SM);
        title.setForeground(DeepNoteTheme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel copy = new JLabel("<html><div style='text-align:center;width:250px'>Get summaries and explanations "
                + "grounded in this note. Everything runs on your device.</div></html>");
        copy.setFont(DeepNoteTheme.SMALL);
        copy.setForeground(DeepNoteTheme.MUTED);
        copy.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(badge);
        content.add(Box.createVerticalStrut(14));
        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(copy);
        content.add(Box.createVerticalStrut(22));
        for (String[] suggestion : SUGGESTIONS) {
            ModernButton chip = new ModernButton(suggestion[0], DeepNoteIcon.Kind.SPARK, ModernButton.Variant.CHIP);
            chip.setHorizontalAlignment(SwingConstants.LEFT);
            chip.setAlignmentX(Component.CENTER_ALIGNMENT);
            chip.setMaximumSize(new Dimension(Integer.MAX_VALUE, chip.getPreferredSize().height));
            chip.addActionListener(event -> {
                questionField.setText(suggestion[1]);
                sendQuestion();
            });
            content.add(chip);
            content.add(Box.createVerticalStrut(8));
        }
        holder.add(content);
        return holder;
    }

    private JComponent userRow(String text) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(DeepNoteTheme.padding(0, 36, 0, 0));
        RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 18, DeepNoteTheme.ACCENT_SOFT,
                DeepNoteTheme.alpha(DeepNoteTheme.ACCENT, 90));
        bubble.setBorder(DeepNoteTheme.padding(10, 14, 10, 14));
        bubble.add(textBlock(text, DeepNoteTheme.TEXT), BorderLayout.CENTER);
        row.add(bubble, BorderLayout.CENTER);
        return row;
    }

    private JComponent assistantRow(String text) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);
        row.add(assistantHeader(), BorderLayout.NORTH);
        row.add(textBlock(text, DeepNoteTheme.TEXT_SECONDARY), BorderLayout.CENTER);
        return row;
    }

    private JComponent thinkingRow() {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);
        row.add(assistantHeader(), BorderLayout.NORTH);
        thinkingLabel = new JLabel("Thinking");
        thinkingLabel.setFont(DeepNoteTheme.CHAT);
        thinkingLabel.setForeground(DeepNoteTheme.MUTED);
        row.add(thinkingLabel, BorderLayout.CENTER);
        return row;
    }

    private JComponent errorRow(String message) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 4), 14, DeepNoteTheme.DANGER_SOFT,
                DeepNoteTheme.alpha(DeepNoteTheme.DANGER, 90));
        card.setBorder(DeepNoteTheme.padding(10, 14, 10, 14));
        JLabel title = new JLabel("Couldn't get an answer");
        title.setFont(DeepNoteTheme.SMALL_MEDIUM);
        title.setForeground(DeepNoteTheme.DANGER);
        card.add(title, BorderLayout.NORTH);
        card.add(textBlock(message, DeepNoteTheme.TEXT_SECONDARY), BorderLayout.CENTER);
        return card;
    }

    private JComponent assistantHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        JLabel name = new JLabel("DeepNote");
        name.setFont(DeepNoteTheme.SMALL_MEDIUM);
        name.setForeground(DeepNoteTheme.ACCENT_TEXT);
        header.add(new IconBadge(DeepNoteIcon.Kind.SPARK, 20, false));
        header.add(Box.createHorizontalStrut(8));
        header.add(name);
        header.add(Box.createHorizontalGlue());
        return header;
    }

    private JTextArea textBlock(String text, Color color) {
        JTextArea area = new JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(DeepNoteTheme.CHAT);
        area.setForeground(color);
        return area;
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
