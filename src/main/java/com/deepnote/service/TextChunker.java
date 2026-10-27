package com.deepnote.service;

import java.util.*;

/** Deterministic paragraph-aware chunking; avoids another dependency for this small boundary. */
public final class TextChunker {
    private final int maxCharacters;
    public TextChunker(int maxCharacters) { this.maxCharacters = maxCharacters; }
    public List<String> split(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\R\\s*\\R")) {
            String remaining = paragraph.trim();
            while (!remaining.isEmpty()) {
                int end = Math.min(maxCharacters, remaining.length());
                if (end < remaining.length()) { int space = remaining.lastIndexOf(' ', end); if (space > maxCharacters / 2) end = space; }
                result.add(remaining.substring(0, end).trim());
                remaining = remaining.substring(end).trim();
            }
        }
        return result;
    }
}
