package com.jirahourlogger.ui;

import com.jirahourlogger.model.Note;
import com.jirahourlogger.storage.NotesManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class NotesView {

    private final NotesManager notesManager = new NotesManager();
    private VBox notesList;
    private Stage stage;

    public void show() {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1E1E2E;");

        HBox titleBar = buildTitleBar();
        VBox dropZone = buildDropZone();
        notesList = new VBox(8);
        refreshNotesList();

        ScrollPane scroll = new ScrollPane(notesList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(300);
        scroll.setStyle("-fx-background: #1E1E2E; -fx-background-color: #1E1E2E;");

        root.getChildren().addAll(titleBar, dropZone, scroll);

        // Draggable by title bar
        final double[] dragDelta = {0, 0};
        titleBar.setOnMousePressed(e -> {
            dragDelta[0] = stage.getX() - e.getScreenX();
            dragDelta[1] = stage.getY() - e.getScreenY();
        });
        titleBar.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() + dragDelta[0]);
            stage.setY(e.getScreenY() + dragDelta[1]);
        });

        Scene scene = new Scene(root, 380, 520);
        scene.setFill(Color.web("#1E1E2E"));
        stage.setScene(scene);
        stage.show();
    }

    private HBox buildTitleBar() {
        Label header = new Label("Notes");
        header.setFont(Font.font("System", FontWeight.BOLD, 18));
        header.setTextFill(Color.WHITE);

        Label close = new Label("✕");
        close.setTextFill(Color.web("#888888"));
        close.setFont(Font.font(14));
        close.setStyle("-fx-cursor: hand;");
        close.setOnMouseClicked(e -> stage.close());

        HBox bar = new HBox(header);
        HBox.setHgrow(header, Priority.ALWAYS);
        bar.getChildren().add(close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-cursor: move;");
        return bar;
    }

    private VBox buildDropZone() {
        VBox zone = new VBox(8);
        zone.setAlignment(Pos.CENTER);
        zone.setPadding(new Insets(20));
        zone.setPrefHeight(110);
        zone.setStyle(dropZoneStyle(false));

        Label icon = new Label("⬆");
        icon.setFont(Font.font(24));
        icon.setTextFill(Color.web("#6C63FF"));

        Label hint = new Label("Drag & drop files here");
        hint.setTextFill(Color.web("#888888"));
        hint.setFont(Font.font("System", 13));

        zone.getChildren().addAll(icon, hint);

        zone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                zone.setStyle(dropZoneStyle(true));
            }
            e.consume();
        });

        zone.setOnDragExited(e -> zone.setStyle(dropZoneStyle(false)));

        zone.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles();
            for (File file : files) {
                try {
                    notesManager.addNote(Path.of(file.getAbsolutePath()));
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            zone.setStyle(dropZoneStyle(false));
            refreshNotesList();
            e.setDropCompleted(true);
            e.consume();
        });

        return zone;
    }

    private void refreshNotesList() {
        notesList.getChildren().clear();
        List<Note> notes = notesManager.loadNotes();

        if (notes.isEmpty()) {
            Label empty = new Label("No notes yet — drop files above to add.");
            empty.setTextFill(Color.web("#666666"));
            empty.setFont(Font.font(12));
            notesList.getChildren().add(empty);
            return;
        }

        for (Note note : notes) {
            notesList.getChildren().add(buildNoteCard(note));
        }
    }

    private HBox buildNoteCard(Note note) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #2A2A3E; -fx-background-radius: 8;");

        VBox info = new VBox(3);
        Label title = new Label(note.getTitle());
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));

        Label date = new Label(note.getCreatedAt().toLocalDate().toString());
        date.setTextFill(Color.web("#888888"));
        date.setFont(Font.font(11));

        info.getChildren().addAll(title, date);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label delete = new Label("✕");
        delete.setTextFill(Color.web("#FF6B6B"));
        delete.setStyle("-fx-cursor: hand;");
        delete.setOnMouseClicked(e -> {
            try {
                notesManager.deleteNote(note);
                refreshNotesList();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(info, delete);
        return card;
    }

    private String dropZoneStyle(boolean active) {
        return String.format(
            "-fx-border-color: %s; -fx-border-width: 2; -fx-border-style: dashed; " +
            "-fx-background-color: %s; -fx-border-radius: 10; -fx-background-radius: 10;",
            active ? "#6C63FF" : "#444444",
            active ? "#2A2A4E" : "#2A2A3E"
        );
    }
}
