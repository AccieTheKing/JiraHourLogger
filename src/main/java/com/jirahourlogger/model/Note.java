package com.jirahourlogger.model;

import java.time.LocalDateTime;

/**
 * Note is a plain data class (a "model") that represents a single note.
 *
 * It holds everything we need to know about a note:
 *   - id        → a short unique identifier so we can tell notes apart
 *   - title     → the original filename of the dropped file
 *   - filePath  → where the file was copied to on disk
 *   - content   → the text content (only filled in for text-based files)
 *   - createdAt → when the note was added
 *
 * This class has no logic — it just holds data. Classes like this are often
 * called POJOs (Plain Old Java Objects) or DTOs (Data Transfer Objects).
 */
public class Note {

    private final String id;
    private final String title;
    private final String filePath;
    private final String content;
    private final LocalDateTime createdAt;

    /**
     * Constructor — called when creating a new Note object.
     * All fields are set here and never change (hence `final`).
     */
    public Note(String id, String title, String filePath, String content, LocalDateTime createdAt) {
        this.id        = id;
        this.title     = title;
        this.filePath  = filePath;
        this.content   = content;
        this.createdAt = createdAt;
    }

    // --- Getters ---
    // These allow other classes to read the values without being able to change them.

    public String getId()               { return id; }
    public String getTitle()            { return title; }
    public String getFilePath()         { return filePath; }
    public String getContent()          { return content; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
