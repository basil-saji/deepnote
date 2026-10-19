package com.deepnote.config;

import java.nio.file.Path;
import java.nio.file.Paths;

/** Central runtime configuration for the desktop application. */
public record AppConfig(Path notesDirectory, String ollamaBaseUrl, String chatModelName,
                        int maxRecentChatTurns, int contextCharacterBudget, int requestTimeoutSeconds) {
    public static AppConfig load() {
        String notesPath = System.getProperty("deepnote.notesDir",
                Paths.get(System.getProperty("user.home"), "DeepNote", "notes").toString());
        return new AppConfig(
                Paths.get(notesPath),
                System.getProperty("deepnote.ollamaUrl", "http://localhost:11434"),
                System.getProperty("deepnote.chatModel", "qwen2.5:0.5b"),
                Integer.getInteger("deepnote.maxRecentTurns", 6),
                Integer.getInteger("deepnote.contextChars", 24000),
                Integer.getInteger("deepnote.timeoutSeconds", 90));
    }
}
