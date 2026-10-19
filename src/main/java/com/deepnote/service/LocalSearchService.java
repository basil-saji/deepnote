package com.deepnote.service;

import com.deepnote.model.Note;
import java.util.List;
import java.util.Locale;

/** Simple local title/content keyword search, independent of AI and PostgreSQL. */
public class LocalSearchService {
    public List<Note> search(List<Note> notes, String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) return notes;
        return notes.stream().filter(note -> (note.title() + "\n" + note.content()).toLowerCase(Locale.ROOT).contains(normalized)).toList();
    }
}
