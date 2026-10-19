package com.deepnote.exception;

/** A failure communicating with the configured local chat model. */
public class ChatModelException extends RuntimeException {
    public ChatModelException(String message) { super(message); }
    public ChatModelException(String message, Throwable cause) { super(message, cause); }
}
