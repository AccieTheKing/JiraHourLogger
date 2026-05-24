package com.jirahourlogger.ui;

import com.jirahourlogger.helper.StageHelper;
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
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * NotesView is the window that opens when the user clicks the "Notes" bubble.
 *
 * It has three sections stacked vertically (VBox):
 *   1. Title bar   — "Notes" heading + a close button, also used to drag the window
 *   2. Drop zone   — a dashed rectangle where users drag & drop files
 *   3. Notes list  — a scrollable list of all saved notes, each with a delete button
 *
 * KEY JAVAFX LAYOUT CLASSES USED:
 *
 *   VBox  — arranges children vertically (top to bottom), with a set spacing between them
 *   HBox  — arranges children horizontally (left to right)
 *   ScrollPane — wraps a node and adds a scrollbar when the content overflows
 *   Label — displays text (or an icon character)
 *   Insets — padding values (top, right, bottom, left)
 *   Pos   — alignment constants (e.g. Pos.CENTER_LEFT = vertically centred, left-aligned)
 */
public class NotesView {

    // NotesManager handles all file reading/writing — NotesView just calls it
    private final NotesManager notesManager = new NotesManager();

    // We keep a reference to the notes list so we can refresh it after adding/deleting
    private VBox notesList;

    private final StageHelper stageHelper = new StageHelper();

    public void show() {
        stageHelper
                .initStyle(StageStyle.UNDECORATED)
                .setAlwaysOnTop(true);

        // VBox is a vertical stack. 16 = 16px gap between each child.
        VBox root = new VBox(16);
        root.setPadding(new Insets(20)); // 20px padding on all four sides
        root.setStyle("-fx-background-color: #1E1E2E;"); // dark navy background

        // Build the three sections
        HBox titleBar = buildTitleBar();
        VBox dropZone  = buildDropZone();
        notesList = new VBox(8); // 8px gap between note cards
        refreshNotesList();      // populate with any existing notes

        // ScrollPane wraps the notes list so it scrolls if there are many notes
        ScrollPane scroll = new ScrollPane(notesList);
        scroll.setFitToWidth(true); // stretch content to fill the scroll pane's width
        scroll.setPrefHeight(300);
        // Remove the default white background of the scroll pane
        scroll.setStyle("-fx-background: #1E1E2E; -fx-background-color: #1E1E2E;");

        root.getChildren().addAll(titleBar, dropZone, scroll);

        // --- Make the window draggable by the title bar ---
        // We store the offset between the mouse position and the window's top-left
        // corner when the drag starts, then use it to reposition the window.
        final double[] dragDelta = {0, 0};
        titleBar.setOnMousePressed(e -> {
            dragDelta[0] = stageHelper.getX() - e.getScreenX();
            dragDelta[1] = stageHelper.getY() - e.getScreenY();
        });
        titleBar.setOnMouseDragged(e -> {
            stageHelper.setX(e.getScreenX() + dragDelta[0]);
            stageHelper.setY(e.getScreenY() + dragDelta[1]);
        });

        Scene scene = new Scene(root, 380, 520);
        scene.setFill(Color.web("#1E1E2E"));
        stageHelper.setScene(scene).show();
    }

    /**
     * Builds the title bar: "Notes" label on the left, "✕" close button on the right.
     *
     * HBox.setHgrow(header, Priority.ALWAYS) makes the header label expand to fill
     * all available horizontal space, which pushes the close button to the far right.
     */
    private HBox buildTitleBar() {
        Label header = new Label("Notes");
        header.setFont(Font.font("System", FontWeight.BOLD, 18));
        header.setTextFill(Color.WHITE);

        Label close = new Label("✕");
        close.setTextFill(Color.web("#888888"));
        close.setFont(Font.font(14));
        close.setStyle("-fx-cursor: hand;");
        close.setOnMouseClicked(e -> stageHelper.close());

        HBox bar = new HBox(header);
        HBox.setHgrow(header, Priority.ALWAYS); // header takes all available space
        bar.getChildren().add(close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-cursor: move;"); // show a move cursor so user knows it's draggable
        return bar;
    }

    /**
     * Builds the drag-and-drop zone — the dashed rectangle that accepts file drops.
     *
     * JavaFX drag-and-drop requires three event handlers:
     *
     *   setOnDragOver    — fires continuously as the user drags something over this node.
     *                      We MUST call e.acceptTransferModes() here to signal that we'll
     *                      accept the drop. Without this, the drop will be rejected.
     *
     *   setOnDragExited  — fires when the drag leaves the node. Used to reset the visual.
     *
     *   setOnDragDropped — fires when the user releases the mouse to drop the files.
     *                      e.getDragboard().getFiles() gives us the list of dropped files.
     *                      We MUST call e.setDropCompleted(true) at the end.
     */
    private VBox buildDropZone() {
        VBox zone = new VBox(8);
        zone.setAlignment(Pos.CENTER);
        zone.setPadding(new Insets(20));
        zone.setPrefHeight(110);
        zone.setStyle(dropZoneStyle(false)); // start with the inactive (no highlight) style

        // Unicode arrow character as a visual icon — no image files needed
        Label icon = new Label("⬆");
        icon.setFont(Font.font(24));
        icon.setTextFill(Color.web("#6C63FF"));

        Label hint = new Label("Drag & drop files here");
        hint.setTextFill(Color.web("#888888"));
        hint.setFont(Font.font("System", 13));

        zone.getChildren().addAll(icon, hint);

        // While dragging over the zone: accept the drag and highlight the zone
        zone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                // TransferMode.COPY means we're copying the file (not moving it)
                e.acceptTransferModes(TransferMode.COPY);
                zone.setStyle(dropZoneStyle(true)); // highlight the border
            }
            e.consume(); // mark event as handled so it doesn't bubble up
        });

        // When the drag leaves: remove the highlight
        zone.setOnDragExited(e -> zone.setStyle(dropZoneStyle(false)));

        // When the files are dropped: save each one and refresh the list
        zone.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles();
            for (File file : files) {
                try {
                    notesManager.addNote(Path.of(file.getAbsolutePath()));
                } catch (IOException ex) {
                    ex.printStackTrace(); // in a real app you'd show an error dialog here
                }
            }
            zone.setStyle(dropZoneStyle(false)); // reset highlight
            refreshNotesList();      // reload the list to show the new notes
            e.setDropCompleted(true); // required: tells the drag source the drop succeeded
            e.consume();
        });

        return zone;
    }

    /**
     * Clears and rebuilds the notes list from disk.
     * Called on first load and after any add/delete operation.
     */
    private void refreshNotesList() {
        notesList.getChildren().clear(); // remove all existing cards
        List<Note> notes = notesManager.loadNotes();

        if (notes.isEmpty()) {
            Label empty = new Label("No notes yet — drop files above to add.");
            empty.setTextFill(Color.web("#666666"));
            empty.setFont(Font.font(12));
            notesList.getChildren().add(empty);
            return;
        }

        // Build one card per note and add it to the list
        for (Note note : notes) {
            notesList.getChildren().add(buildNoteCard(note));
        }
    }

    /**
     * Builds a single note card: filename and date on the left, delete button on the right.
     *
     * HBox.setHgrow(info, Priority.ALWAYS) makes the info section expand to fill
     * the card, which pushes the delete button to the far right.
     */
    private HBox buildNoteCard(Note note) {
        HBox card = new HBox(12); // 12px gap between children
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #2A2A3E; -fx-background-radius: 8;");

        // Left side: note title (filename) and creation date stacked vertically
        VBox info = new VBox(3); // 3px gap between title and date

        Label title = new Label(note.getTitle());
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));

        // toLocalDate() strips the time part, leaving just e.g. "2026-05-11"
        Label date = new Label(note.getCreatedAt().toLocalDate().toString());
        date.setTextFill(Color.web("#888888"));
        date.setFont(Font.font(11));

        info.getChildren().addAll(title, date);
        HBox.setHgrow(info, Priority.ALWAYS); // info takes all available width

        // Right side: red delete button
        Label delete = new Label("✕");
        delete.setTextFill(Color.web("#FF6B6B"));
        delete.setStyle("-fx-cursor: hand;");
        delete.setOnMouseClicked(e -> {
            try {
                notesManager.deleteNote(note); // remove from disk
                refreshNotesList();            // update the UI
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });

        card.getChildren().addAll(info, delete);
        return card;
    }

    /**
     * Returns the CSS style string for the drop zone.
     * `active=true` = user is hovering with a file (highlight the border).
     * `active=false` = normal state (subtle border).
     *
     * String.format() works like a template: %s is replaced by the string arguments.
     */
    private String dropZoneStyle(boolean active) {
        return String.format(
            "-fx-border-color: %s; -fx-border-width: 2; -fx-border-style: dashed; " +
            "-fx-background-color: %s; -fx-border-radius: 10; -fx-background-radius: 10;",
            active ? "#6C63FF" : "#444444", // purple border when active, grey when not
            active ? "#2A2A4E" : "#2A2A3E"  // slightly lighter background when active
        );
    }
}
