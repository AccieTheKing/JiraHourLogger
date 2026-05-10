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

public class NotesManager {

    private static final Path NOTES_DIR =
        Path.of(System.getProperty("user.home"), ".jirahourlogger", "notes");

    public NotesManager() {
        try {
            Files.createDirectories(NOTES_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Could not create notes directory: " + NOTES_DIR, e);
        }
    }

    public Note addNote(Path sourceFile) throws IOException {
        String id = UUID.randomUUID().toString().substring(0, 8);
        String fileName = sourceFile.getFileName().toString();
        Path dest = NOTES_DIR.resolve(id + "_" + fileName);
        Files.copy(sourceFile, dest, StandardCopyOption.REPLACE_EXISTING);
        String content = isTextFile(fileName) ? Files.readString(dest) : "";
        return new Note(id, fileName, dest.toString(), content, LocalDateTime.now());
    }

    public List<Note> loadNotes() {
        List<Note> notes = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(NOTES_DIR)) {
            for (Path file : stream) {
                String raw = file.getFileName().toString();
                String title = raw.contains("_") ? raw.substring(raw.indexOf('_') + 1) : raw;
                String id = raw.contains("_") ? raw.substring(0, raw.indexOf('_')) : raw;
                String content = isTextFile(title) ? Files.readString(file) : "";
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

    public void deleteNote(Note note) throws IOException {
        Files.deleteIfExists(Path.of(note.getFilePath()));
    }

    private boolean isTextFile(String name) {
        return name.endsWith(".txt") || name.endsWith(".md") || name.endsWith(".java")
            || name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".xml");
    }
}
