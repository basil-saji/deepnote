package com.deepnote.service;

import com.deepnote.exception.NoteStorageException;
import com.deepnote.model.Note;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class NoteServiceTest {
    @TempDir Path directory;

    @Test void createsReadsUpdatesAndDeletesTxtNotes() {
        NoteService service = new NoteService(directory);
        Note note = service.createNote("Java Notes");
        assertEquals("", service.readNote(note.path()).content());
        Note saved = service.saveNote(note, "Classes and objects");
        assertEquals("Classes and objects", service.readNote(saved.path()).content());
        service.deleteNote(saved);
        assertFalse(note.path().toFile().exists());
    }

    @Test void rejectsUnsafeOrDuplicateTitlesAndRenameCollisions() {
        NoteService service = new NoteService(directory);
        Note first = service.createNote("First");
        service.createNote("Second");
        assertThrows(NoteStorageException.class, () -> service.createNote("First"));
        assertThrows(NoteStorageException.class, () -> service.createNote("../escape"));
        assertThrows(NoteStorageException.class, () -> service.renameNote(first, "Second"));
    }
}
