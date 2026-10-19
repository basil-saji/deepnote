package com.deepnote.ai;

import com.deepnote.exception.ContextLimitException;
import com.deepnote.model.ChatMessage;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ContextManagerTest {
    @Test void keepsOnlyRecentConversationTurnsAndIncludesCurrentNote() {
        ContextManager manager = new ContextManager(1, 2000);
        String oldContent = "old ".repeat(5000);
        List<ChatMessage> messages = manager.prepare("Algorithms", "A graph has vertices.", List.of(
                new ChatMessage(ChatMessage.Role.USER, oldContent), new ChatMessage(ChatMessage.Role.ASSISTANT, oldContent),
                new ChatMessage(ChatMessage.Role.USER, "recent"), new ChatMessage(ChatMessage.Role.ASSISTANT, "recent answer")), "Explain it");
        assertEquals(3, messages.size());
        assertEquals("recent", messages.get(0).content());
        assertEquals("recent answer", messages.get(1).content());
        assertFalse(messages.get(0).content().contains("old"));
        assertFalse(messages.get(1).content().contains("old"));
        assertTrue(messages.get(2).content().contains("A graph has vertices."));
    }

    @Test void refusesACompleteNoteThatExceedsBudget() {
        ContextManager manager = new ContextManager(2, 100);
        assertThrows(ContextLimitException.class, () -> manager.prepare("Long", "This content cannot fit", List.of(), "Explain"));
    }

    @Test void retainsGroundingInstructionsNoteTitleAndQuestionWhenSmallNoteFits() {
        ContextManager manager = new ContextManager(2, 2000);
        List<ChatMessage> messages = manager.prepare("Algorithms", "A graph has vertices.", List.of(), "Explain the graph.");

        assertTrue(manager.systemInstruction().contains("primary source"));
        assertTrue(messages.get(0).content().contains("Algorithms"));
        assertTrue(messages.get(0).content().contains("A graph has vertices."));
        assertTrue(messages.get(0).content().contains("Explain the graph."));
    }
}
