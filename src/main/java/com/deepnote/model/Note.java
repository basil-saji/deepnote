package com.deepnote.model;

import java.nio.file.Path;

/** A portable plain-text note. */
public record Note(String title, Path path, String content) {
    public Note withContent(String newContent) { return new Note(title, path, newContent); }
    public Note withTitle(String newTitle, Path newPath) { return new Note(newTitle, newPath, content); }
}
