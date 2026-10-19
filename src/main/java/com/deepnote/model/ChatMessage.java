package com.deepnote.model;

/** One in-memory message in a note-scoped conversation. */
public record ChatMessage(Role role, String content) {
    public enum Role { USER, ASSISTANT }
}
