package com.deepnote.ai;

import com.deepnote.model.ChatMessage;
import java.util.List;

/** Boundary for a local generative model. */
public interface ChatModelClient {
    String generate(String systemInstruction, List<ChatMessage> messages);
}
