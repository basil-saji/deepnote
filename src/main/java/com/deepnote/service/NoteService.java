package com.deepnote.service;

import com.deepnote.exception.NoteStorageException;
import com.deepnote.model.Note;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** File-backed CRUD service. TXT files remain the source of truth. */
public class NoteService {
    private static final Pattern INVALID_FILENAME = Pattern.compile("[\\\\/:*?\"<>|]");
    private final Path notesDirectory;

    public NoteService(Path notesDirectory) {
        this.notesDirectory = notesDirectory;
        try { Files.createDirectories(notesDirectory); }
        catch (IOException exception) { throw new NoteStorageException("Cannot create notes directory.", exception); }
    }

    public List<Note> listNotes() {
        try (var paths = Files.list(notesDirectory)) {
            return paths.filter(path -> path.toString().toLowerCase().endsWith(".txt"))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .map(this::readNote).toList();
        } catch (IOException exception) { throw new NoteStorageException("Cannot list notes.", exception); }
    }

    public Note createNote(String title) {
        String safeTitle = validateTitle(title);
        Path path = notesDirectory.resolve(safeTitle + ".txt");
        if (Files.exists(path)) throw new NoteStorageException("A note with that title already exists.");
        write(path, "");
        return new Note(safeTitle, path, "");
    }

    public Note readNote(Path path) {
        try { return new Note(titleFrom(path), path, Files.readString(path, StandardCharsets.UTF_8)); }
        catch (IOException exception) { throw new NoteStorageException("Cannot read note: " + path.getFileName(), exception); }
    }

    public Note saveNote(Note note, String content) {
        write(note.path(), content);
        return note.withContent(content);
    }

    public Note renameNote(Note note, String newTitle) {
        String safeTitle = validateTitle(newTitle);
        Path target = notesDirectory.resolve(safeTitle + ".txt");
        if (!target.equals(note.path()) && Files.exists(target)) throw new NoteStorageException("A note with that title already exists.");
        try { Files.move(note.path(), target); return note.withTitle(safeTitle, target); }
        catch (IOException exception) { throw new NoteStorageException("Cannot rename note.", exception); }
    }

    public void deleteNote(Note note) {
        try { Files.deleteIfExists(note.path()); }
        catch (IOException exception) { throw new NoteStorageException("Cannot delete note.", exception); }
    }

    private void write(Path path, String content) {
        try { Files.writeString(path, content, StandardCharsets.UTF_8); }
        catch (IOException exception) { throw new NoteStorageException("Cannot save note.", exception); }
    }

    private String validateTitle(String title) {
        String clean = title == null ? "" : title.trim();
        if (clean.isBlank() || INVALID_FILENAME.matcher(clean).find() || clean.equals(".") || clean.equals("..")) {
            throw new NoteStorageException("Use a non-empty title without filesystem separator characters.");
        }
        return clean;
    }

    private String titleFrom(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.length() - 4);
    }
}
