package com.deepnote.ai;

import com.deepnote.model.ChatMessage;
import com.deepnote.exception.ChatModelException;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class AIAssistantServiceTest {
    @Test void passesActiveNoteToClientAndKeepsCallsIndependent() {
        ChatModelClient fake = (system, messages) -> messages.get(0).content();
        AIAssistantService service = new AIAssistantService(new ContextManager(4, 2000), fake);
        String first = service.answer("First", "alpha", List.of(), "What is here?");
        String second = service.answer("Second", "beta", List.of(), "What is here?");
        assertTrue(first.contains("alpha")); assertFalse(first.contains("beta"));
        assertTrue(second.contains("beta")); assertFalse(second.contains("alpha"));
    }

    @Test void surfacesLocalModelFailureWithoutChangingThePromptContract() {
        ChatModelClient unavailable = (system, messages) -> { throw new ChatModelException("Ollama unavailable"); };
        AIAssistantService service = new AIAssistantService(new ContextManager(2, 2000), unavailable);
        ChatModelException failure = assertThrows(ChatModelException.class, () -> service.answer("Note", "content", List.of(), "question"));
        assertEquals("Ollama unavailable", failure.getMessage());
    }

    @Test void doesNotInvokeModelWhenContextLimitFails() {
        AtomicBoolean invoked = new AtomicBoolean();
        ChatModelClient client = (system, messages) -> { invoked.set(true); return "unexpected"; };
        AIAssistantService service = new AIAssistantService(new ContextManager(2, 100), client);

        assertThrows(com.deepnote.exception.ContextLimitException.class,
                () -> service.answer("Note", "A note that is too large for this budget", List.of(), "question"));
        assertFalse(invoked.get());
    }
}
