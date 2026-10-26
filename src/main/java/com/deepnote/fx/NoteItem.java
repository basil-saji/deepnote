package com.deepnote.fx;

import com.deepnote.model.Note;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;

/** UI-side view of a {@link Note} with metrics derived from real file data. Build off the FX thread. */
record NoteItem(Note note, Instant modified, long words, int characters, int minutesToRead) {
    static NoteItem of(Note note) {
        Instant modified;
        try { modified = Files.getLastModifiedTime(note.path()).toInstant(); }
        catch (IOException | RuntimeException ex) { modified = Instant.EPOCH; }
        long words = FxUtil.wordCount(note.content());
        return new NoteItem(note, modified, words, note.content().length(), FxUtil.readMinutes(words));
    }
}
