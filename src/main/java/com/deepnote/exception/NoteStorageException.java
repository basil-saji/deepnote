package com.deepnote.exception;

/** A user-visible failure while reading or writing a TXT note. */
public class NoteStorageException extends RuntimeException {
    public NoteStorageException(String message) { super(message); }
    public NoteStorageException(String message, Throwable cause) { super(message, cause); }
}
