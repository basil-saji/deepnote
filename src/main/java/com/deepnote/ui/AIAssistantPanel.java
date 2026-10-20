package com.deepnote.ui;

import com.deepnote.ai.AIAssistantService;
import com.deepnote.model.ChatMessage;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Integrated AI Assistant panel:
 * A calm, high-legibility workspace with contextual grounding, structured bubbles,
 * interactive prompt suggestions, and a refined multiline composer.
 */
public class AIAssistantPanel extends JPanel {
    private static final String[][] SUGGESTIONS = {
            {"Summarize this note", "Summarize this note in a few concise sentences."},
            {"Explain it simply", "Explain the concepts in this note in simple, clear terms."},
            {"List key takeaways", "List the key takeaways of this note as bullet points."},
            {"What details are missing?", "What important details or questions seem to be missing from this note?"}
    };

    private final JLabel noteBadge = new JLabel();
    private final ScrollableColumn messageList = new ScrollableColumn(null, false);
    private final JScrollPane messageScroll;
    private final JTextArea questionField;
    private final ModernButton sendButton;
    private final JLabel statusLabel = new JLabel("Ready");
    private final RoundedPanel composer = new RoundedPanel(new BorderLayout(8, 0), 12,
            DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER);
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

        sendButton = ModernButton.iconOnly(DeepNoteIcon.Kind.SEND, "Send (Enter)", ModernButton.Variant.PRIMARY, 15);

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
        setMinimumSize(new Dimension(320, 0));
        setPreferredSize(new Dimension(400, 0));

        add(buildHeader(), BorderLayout.NORTH);

        messageList.setLayout(new BoxLayout(messageList, BoxLayout.Y_AXIS));
        messageList.setBorder(DeepNoteTheme.padding(16, 18, 12, 18));

        messageScroll = new JScrollPane(messageList);
        messageScroll.setBorder(null);
        messageScroll.setOpaque(false);
        messageScroll.getViewport().setOpaque(false);
        messageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        messageScroll.getVerticalScrollBar().setUnitIncrement(16);
        add(messageScroll, BorderLayout.CENTER);

        add(buildComposer(), BorderLayout.SOUTH);

        thinkingTimer = new Timer(350, event -> animateThinking());
        sendButton.addActionListener(event -> sendQuestion());
        bindComposerKeys();
        renderHistory();
    }

    public void setNote(String title) {
        this.noteTitle = title;
        noteBadge.setText(truncate(title, 26));
        noteBadge.setToolTipText("Active note: " + title);
    }

    public void focusInput() {
        questionField.requestFocusInWindow();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(DeepNoteTheme.SIDEBAR);
        header.setBorder(BorderFactory.createCompoundBorder(
                DeepNoteTheme.divider(0, 0, 1, 0),
                DeepNoteTheme.padding(12, 16, 12, 12)));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        IconBadge avatar = new IconBadge(DeepNoteIcon.Kind.SPARK, 26, true);
        JLabel title = new JLabel("AI Assistant");
        title.setFont(DeepNoteTheme.TITLE_SM);
        title.setForeground(DeepNoteTheme.TEXT);

        noteBadge.setFont(DeepNoteTheme.SMALL_MEDIUM);
        noteBadge.setForeground(DeepNoteTheme.ACCENT_TEXT);
        noteBadge.setBorder(BorderFactory.createCompoundBorder(
                new RoundedPanel(8, DeepNoteTheme.ACCENT_SOFT, DeepNoteTheme.ACCENT_BORDER_SOFT).getBorder(),
                DeepNoteTheme.padding(2, 8, 2, 8)));

        left.add(avatar);
        left.add(title);
        left.add(noteBadge);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);

        ModernButton newChat = ModernButton.iconOnly(DeepNoteIcon.Kind.REFRESH, "New conversation",
                ModernButton.Variant.GHOST, 15);
        newChat.addActionListener(event -> clearConversation());

        ModernButton collapse = ModernButton.iconOnly(DeepNoteIcon.Kind.SIDEBAR_RIGHT, "Close AI assistant (Ctrl+J)",
                ModernButton.Variant.GHOST, 16);
        collapse.addActionListener(event -> collapseAction.run());

        actions.add(newChat);
        actions.add(collapse);

        header.add(left, BorderLayout.WEST);
        header.add(actions, BorderLayout.EAST);
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
        questionField.setBackground(DeepNoteTheme.SURFACE);
        questionField.setOpaque(false);
        questionField.setBorder(null);
        questionField.setRows(2);
        questionField.setToolTipText("Enter to send, Shift+Enter for a new line");

        questionField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) {
                composer.setStroke(DeepNoteTheme.ACCENT);
                composer.setFill(DeepNoteTheme.SURFACE_HOVER);
            }
            @Override public void focusLost(FocusEvent event) {
                composer.setStroke(DeepNoteTheme.BORDER);
                composer.setFill(DeepNoteTheme.SURFACE);
            }
        });

        JPanel sendHolder = new JPanel(new BorderLayout());
        sendHolder.setOpaque(false);
        sendHolder.add(sendButton, BorderLayout.SOUTH);

        composer.setBorder(DeepNoteTheme.padding(8, 12, 8, 8));
        composer.add(questionField, BorderLayout.CENTER);
        composer.add(sendHolder, BorderLayout.EAST);
        wrapper.add(composer, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(DeepNoteTheme.padding(2, 4, 0, 4));

        statusLabel.setFont(DeepNoteTheme.SMALL);
        statusLabel.setForeground(DeepNoteTheme.MUTED);

        JLabel hint = new JLabel("Enter to send  ·  Shift+Enter for newline");
        hint.setFont(DeepNoteTheme.SMALL);
        hint.setForeground(DeepNoteTheme.FAINT);

        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(hint, BorderLayout.EAST);
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
        thinkingLabel.setText("Analyzing note and crafting answer" + ".".repeat(thinkingStep));
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private void clearConversation() {
        if (requestRunning) return;
        history.clear();
        lastError = null;
        setStatus("Ready", DeepNoteTheme.MUTED);
        renderHistory();
    }

    private void renderHistory() {
        messageList.removeAll();
        thinkingLabel = null;

        if (history.isEmpty() && pendingQuestion == null && lastError == null) {
            messageList.add(emptyState());
        } else {
            for (ChatMessage message : history) {
                addRow(message.role() == ChatMessage.Role.USER
                        ? userRow(message.content()) : assistantRow(message.content()));
            }
            if (pendingQuestion != null) {
                addRow(userRow(pendingQuestion));
                addRow(thinkingRow());
            }
            if (lastError != null) {
                addRow(errorRow(lastError));
            }
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
        messageList.add(Box.createVerticalStrut(14));
    }

    private JComponent emptyState() {
        JPanel holder = new JPanel(new GridBagLayout());
        holder.setOpaque(false);
        holder.setAlignmentX(Component.LEFT_ALIGNMENT);
        holder.setBorder(DeepNoteTheme.padding(24, 0, 16, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        IconBadge badge = new IconBadge(DeepNoteIcon.Kind.SPARK, 44, true);
        badge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Chat with this note");
        title.setFont(DeepNoteTheme.TITLE);
        title.setForeground(DeepNoteTheme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel copy = new JLabel("<html><div style='text-align:center;width:270px;color:#8E92A4;line-height:1.4'>"
                + "Ask questions, generate summaries, or explore ideas strictly grounded in your active note.</div></html>");
        copy.setFont(DeepNoteTheme.SMALL);
        copy.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(badge);
        content.add(Box.createVerticalStrut(14));
        content.add(title);
        content.add(Box.createVerticalStrut(6));
        content.add(copy);
        content.add(Box.createVerticalStrut(24));

        JLabel suggestionsLabel = new JLabel("SUGGESTED PROMPTS");
        suggestionsLabel.setFont(DeepNoteTheme.LABEL);
        suggestionsLabel.setForeground(DeepNoteTheme.FAINT);
        suggestionsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(suggestionsLabel);
        content.add(Box.createVerticalStrut(10));

        for (String[] suggestion : SUGGESTIONS) {
            ModernButton chip = new ModernButton(suggestion[0], DeepNoteIcon.Kind.SPARK, ModernButton.Variant.CHIP, 14);
            chip.setHorizontalAlignment(SwingConstants.LEFT);
            chip.setAlignmentX(Component.LEFT_ALIGNMENT);
            chip.setMaximumSize(new Dimension(320, 36));
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
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        row.setOpaque(false);

        RoundedPanel bubble = new RoundedPanel(new BorderLayout(), 14,
                DeepNoteTheme.ACCENT_SOFT, DeepNoteTheme.ACCENT_BORDER_SOFT);
        bubble.setBorder(DeepNoteTheme.padding(9, 14, 9, 14));

        JTextArea area = textBlock(text, DeepNoteTheme.TEXT);
        bubble.add(area, BorderLayout.CENTER);

        int maxW = messageScroll != null && messageScroll.getViewport().getWidth() > 100
                ? messageScroll.getViewport().getWidth() - 60 : 320;
        Dimension pref = bubble.getPreferredSize();
        if (pref.width > maxW) {
            area.setSize(maxW - 28, Short.MAX_VALUE);
            bubble.setPreferredSize(new Dimension(maxW, area.getPreferredSize().height + 18));
        }

        row.add(bubble);
        return row;
    }

    private JComponent assistantRow(String text) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);
        row.add(assistantHeader(), BorderLayout.NORTH);

        RoundedPanel container = new RoundedPanel(new BorderLayout(), 12,
                DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER_SUBTLE);
        container.setBorder(DeepNoteTheme.padding(10, 14, 10, 14));
        container.add(textBlock(text, DeepNoteTheme.TEXT_SECONDARY), BorderLayout.CENTER);

        row.add(container, BorderLayout.CENTER);
        return row;
    }

    private JComponent thinkingRow() {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setOpaque(false);
        row.add(assistantHeader(), BorderLayout.NORTH);

        RoundedPanel container = new RoundedPanel(new BorderLayout(), 12,
                DeepNoteTheme.SURFACE, DeepNoteTheme.BORDER_SUBTLE);
        container.setBorder(DeepNoteTheme.padding(10, 14, 10, 14));

        thinkingLabel = new JLabel("Analyzing note and crafting answer…");
        thinkingLabel.setFont(DeepNoteTheme.SMALL_MEDIUM);
        thinkingLabel.setForeground(DeepNoteTheme.ACCENT_TEXT);
        container.add(thinkingLabel, BorderLayout.CENTER);

        row.add(container, BorderLayout.CENTER);
        return row;
    }

    private JComponent errorRow(String message) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 6), 12, DeepNoteTheme.DANGER_SOFT,
                DeepNoteTheme.DANGER_BORDER);
        card.setBorder(DeepNoteTheme.padding(10, 14, 10, 14));

        JLabel title = new JLabel("Assistant query failed");
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

        IconBadge avatar = new IconBadge(DeepNoteIcon.Kind.SPARK, 18, false);
        JLabel name = new JLabel("DeepNote AI");
        name.setFont(DeepNoteTheme.SMALL_MEDIUM);
        name.setForeground(DeepNoteTheme.ACCENT_TEXT);

        header.add(avatar);
        header.add(Box.createHorizontalStrut(6));
        header.add(name);
        header.add(Box.createHorizontalGlue());
        return header;
    }

    private JTextArea textBlock(String text, Color color) {
        JTextArea area = new JTextArea(text) {
            @Override
            public Dimension getPreferredSize() {
                int width = getWidth();
                if (width <= 0 && messageScroll != null) {
                    width = messageScroll.getViewport().getWidth() - 72;
                }
                if (width > 0) {
                    setSize(width, Short.MAX_VALUE);
                }
                return super.getPreferredSize();
            }
        };
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(DeepNoteTheme.CHAT);
        area.setForeground(color);
        return area;
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private String rootMessage(Exception exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
