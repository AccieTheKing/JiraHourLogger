package com.jirahourlogger.model;

import java.time.LocalDateTime;

public class Note {
    private final String id;
    private final String title;
    private final String filePath;
    private final String content;
    private final LocalDateTime createdAt;

    public Note(String id, String title, String filePath, String content, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.filePath = filePath;
        this.content = content;
        this.createdAt = createdAt;
    }

    public String getId()              { return id; }
    public String getTitle()           { return title; }
    public String getFilePath()        { return filePath; }
    public String getContent()         { return content; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
