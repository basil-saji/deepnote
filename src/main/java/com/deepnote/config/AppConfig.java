package com.deepnote.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/** Central runtime configuration for the desktop application. */
public record AppConfig(Path notesDirectory, String ollamaBaseUrl, String chatModelName,
                        int maxRecentChatTurns, int contextCharacterBudget, int requestTimeoutSeconds,
                        String dbHost, int dbPort, String dbName, String dbUser, String dbPassword,
                        String embeddingModel, int embeddingDimensions, double similarityThreshold) {
    public static AppConfig load() {
        Map<String, String> dotEnv = readDotEnv();
        String notesPath = System.getProperty("deepnote.notesDir",
                Paths.get(System.getProperty("user.home"), "Documents", "DeepNote", "notes").toString());
        return new AppConfig(
                Paths.get(notesPath),
                System.getProperty("deepnote.ollamaUrl", "http://localhost:11434"),
                System.getProperty("deepnote.chatModel", "qwen2.5:0.5b"),
                Integer.getInteger("deepnote.maxRecentTurns", 6),
                Integer.getInteger("deepnote.contextChars", 24000),
                Integer.getInteger("deepnote.timeoutSeconds", 90),
                setting("DB_HOST", "POSTGRES_HOST", "localhost", dotEnv),
                Integer.parseInt(setting("DB_PORT", "POSTGRES_PORT", "5432", dotEnv)),
                setting("DB_NAME", "POSTGRES_DB", "deepnote", dotEnv),
                setting("DB_USER", "POSTGRES_USER", "deepnote_app", dotEnv),
                setting("DB_PASSWORD", "POSTGRES_PASSWORD", "", dotEnv),
                System.getProperty("deepnote.embeddingModel", "embeddinggemma:300m"),
                Integer.getInteger("deepnote.embeddingDimensions", 768),
                Double.parseDouble(System.getProperty("deepnote.similarityThreshold", "0.80")));
    }

    private static String setting(String primary, String legacy, String fallback, Map<String, String> dotEnv) {
        String value = System.getenv(primary);
        if (value == null || value.isBlank()) value = System.getenv(legacy);
        if (value == null || value.isBlank()) value = dotEnv.get(primary);
        if (value == null || value.isBlank()) value = dotEnv.get(legacy);
        return value == null || value.isBlank() ? fallback : value;
    }

    /** Loads a local, ignored .env for desktop launches; process environment wins. */
    private static Map<String, String> readDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path file = Paths.get(".env");
        if (!Files.isRegularFile(file)) return values;
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                int separator = trimmed.indexOf('=');
                if (separator <= 0) continue;
                String key = trimmed.substring(0, separator).trim();
                String value = trimmed.substring(separator + 1).trim();
                if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) value = value.substring(1, value.length() - 1);
                values.put(key, value);
            }
        } catch (Exception ignored) { }
        return values;
    }
}
