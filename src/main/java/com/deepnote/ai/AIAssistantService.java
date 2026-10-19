package com.deepnote.ai;

import com.deepnote.model.ChatMessage;
import java.util.List;

/** Coordinates note-grounded prompt preparation and local generation. */
public class AIAssistantService {
    private final ContextManager contextManager;
    private final ChatModelClient modelClient;

    public AIAssistantService(ContextManager contextManager, ChatModelClient modelClient) {
        this.contextManager = contextManager;
        this.modelClient = modelClient;
    }

    public String answer(String noteTitle, String noteContent, List<ChatMessage> history, String question) {
        List<ChatMessage> messages = contextManager.prepare(noteTitle, noteContent, history, question);
        return modelClient.generate(contextManager.systemInstruction(), messages);
    }
}
