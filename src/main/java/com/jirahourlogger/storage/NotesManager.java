package com.jirahourlogger.storage;

import com.jirahourlogger.model.Note;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * NotesManager handles all file system operations for notes.
 *
 * Responsibility: reading and writing notes to disk.
 * The rest of the app never touches the file system directly — it
 * always goes through this class. This separation makes it easy to
 * swap out storage later (e.g. a database) without touching the UI.
 *
 * Notes are stored in: ~/.jirahourlogger/notes/
 * The ~ means the current user's home folder (e.g. /Users/accie).
 *
 * Each note is saved as a copy of the original file, prefixed with a
 * short unique ID so two files with the same name don't overwrite each other.
 * Example filename on disk:  a3f9b2c1_MyDocument.pdf
 */
public class NotesManager {

    /**
     * Path.of() builds a file path from parts.
     * System.getProperty("user.home") returns the current user's home directory.
     * So NOTES_DIR = /Users/accie/.jirahourlogger/notes/
     */
    private static final Path NOTES_DIR =
        Path.of(System.getProperty("user.home"), ".jirahourlogger", "notes");

    /**
     * Constructor — runs once when NotesManager is created.
     * Creates the notes directory if it doesn't already exist.
     * Files.createDirectories() is safe to call even if the folder exists.
     */
    public NotesManager() {
        try {
            Files.createDirectories(NOTES_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create notes directory: " + NOTES_DIR, e);
        }
    }

    /**
     * Copies a file into the notes folder and returns a Note object for it.
     *
     * @param sourceFile  the original file the user dragged in
     * @return            a Note object representing the saved file
     */
    public Note addNote(Path sourceFile) throws IOException {
        // Generate a short random ID (first 8 chars of a UUID, e.g. "a3f9b2c1")
        String id = UUID.randomUUID().toString().substring(0, 8);

        // Get just the filename part of the path (e.g. "MyDocument.pdf")
        String fileName = sourceFile.getFileName().toString();

        // Build the destination path: notes/a3f9b2c1_MyDocument.pdf
        Path dest = NOTES_DIR.resolve(id + "_" + fileName);

        // Copy the file. REPLACE_EXISTING means overwrite if somehow the same path exists.
        Files.copy(sourceFile, dest, StandardCopyOption.REPLACE_EXISTING);

        // Only read file content for text-based files (PDFs etc. would be unreadable binary)
        String content = isTextFile(fileName) ? Files.readString(dest) : "";

        return new Note(id, fileName, dest.toString(), content, LocalDateTime.now());
    }

    /**
     * Reads all files in the notes folder and returns them as a list of Note objects.
     * Called every time the NotesView needs to refresh its list.
     */
    public List<Note> loadNotes() {
        List<Note> notes = new ArrayList<>();

        // DirectoryStream lets us loop over all files in the folder
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(NOTES_DIR)) {
            for (Path file : stream) {
                String raw = file.getFileName().toString(); // e.g. "a3f9b2c1_MyDocument.pdf"

                // Split on the first underscore to recover the id and original filename
                String id    = raw.contains("_") ? raw.substring(0, raw.indexOf('_'))  : raw;
                String title = raw.contains("_") ? raw.substring(raw.indexOf('_') + 1) : raw;

                String content = isTextFile(title) ? Files.readString(file) : "";

                // Read the file's actual creation time from the OS file metadata
                BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
                LocalDateTime created = LocalDateTime.ofInstant(
                    attrs.creationTime().toInstant(), ZoneId.systemDefault());

                notes.add(new Note(id, title, file.toString(), content, created));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return notes;
    }

    /**
     * Deletes the file on disk for the given note.
     * deleteIfExists() won't throw an error if the file is already gone.
     */
    public void deleteNote(Note note) throws IOException {
        Files.deleteIfExists(Path.of(note.getFilePath()));
    }

    /**
     * Returns true if we can safely read this file as plain text.
     * We avoid trying to read binary files (images, PDFs, etc.) as text.
     */
    private boolean isTextFile(String name) {
        return name.endsWith(".txt")  || name.endsWith(".md")   ||
               name.endsWith(".java") || name.endsWith(".json") ||
               name.endsWith(".csv")  || name.endsWith(".xml");
    }
}
