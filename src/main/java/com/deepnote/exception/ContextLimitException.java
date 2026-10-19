package com.deepnote.exception;

/** Indicates that a complete active note cannot fit in the configured MVP context budget. */
public class ContextLimitException extends RuntimeException {
    public ContextLimitException(String message) { super(message); }
}
