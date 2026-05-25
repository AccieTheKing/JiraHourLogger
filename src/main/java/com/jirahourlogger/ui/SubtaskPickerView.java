package com.jirahourlogger.ui;

import com.jirahourlogger.api.JiraClient;
import com.jirahourlogger.model.JiraIssue;
import com.jirahourlogger.model.LogEntry;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.LocalDate;
import java.util.List;

/**
 * SubtaskPickerView shows a small window for a single LogEntry.
 *
 * It displays:
 *   - The time block (e.g. "09:00 – 11:00") and the parent ticket key
 *   - A list of subtasks the user can click to select
 *   - A "Log time" button that posts the worklog to the selected subtask
 *
 * When the worklog is posted (or skipped), `onComplete` is called so
 * NotesView can move on to the next entry.
 *
 * Modality.APPLICATION_MODAL blocks interaction with other windows while
 * this picker is open, so entries are handled one at a time.
 */
public class SubtaskPickerView {

    private final LogEntry entry;
    private final List<JiraIssue> subtasks;
    private final LocalDate date;
    private final JiraClient jiraClient;
    private final Runnable onComplete;  // called when this entry is done (logged or skipped)

    private JiraIssue selectedSubtask = null;

    public SubtaskPickerView(LogEntry entry, List<JiraIssue> subtasks,
                             LocalDate date, JiraClient jiraClient, Runnable onComplete) {
        this.entry       = entry;
        this.subtasks    = subtasks;
        this.date        = date;
        this.jiraClient  = jiraClient;
        this.onComplete  = onComplete;
    }

    public void show() {
        Stage stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.initModality(Modality.APPLICATION_MODAL); // blocks other windows while open
        stage.setAlwaysOnTop(true);

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #1E1E2E; -fx-background-radius: 12;");

        // --- Header: time range + parent ticket ---
        String parentKey = JiraClient.extractKey(entry.url());
        String timeLabel = entry.timeRange().start() + " – " + entry.timeRange().end();

        Label timeText = new Label(timeLabel);
        timeText.setFont(Font.font("System", FontWeight.BOLD, 16));
        timeText.setTextFill(Color.WHITE);

        Label parentLabel = new Label(parentKey);
        parentLabel.setFont(Font.font("System", 13));
        parentLabel.setTextFill(Color.web("#6C63FF"));

        root.getChildren().addAll(timeText, parentLabel);

        // --- Description (optional) ---
        entry.description().ifPresent(desc -> {
            Label descLabel = new Label(desc);
            descLabel.setTextFill(Color.web("#AAAAAA"));
            descLabel.setFont(Font.font("System", 12));
            descLabel.setWrapText(true);
            root.getChildren().add(descLabel);
        });

        // --- Subtask list or fallback message ---
        if (subtasks.isEmpty()) {
            Label none = new Label("No subtasks found — time will be logged to " + parentKey);
            none.setTextFill(Color.web("#888888"));
            none.setWrapText(true);
            root.getChildren().add(none);
        } else {
            Label pick = new Label("Select a subtask to log time to:");
            pick.setTextFill(Color.web("#888888"));
            pick.setFont(Font.font(12));
            root.getChildren().add(pick);

            for (JiraIssue subtask : subtasks) {
                root.getChildren().add(buildSubtaskCard(subtask));
            }
        }

        // --- Footer: status label + action buttons ---
        Label statusLabel = new Label("");
        statusLabel.setTextFill(Color.web("#FF6B6B"));
        statusLabel.setFont(Font.font(11));

        Button logBtn = new Button("Log time");
        logBtn.setStyle(
                "-fx-background-color: #6C63FF; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        logBtn.setOnAction(e -> handleLogTime(stage, statusLabel));

        Button skipBtn = new Button("Skip");
        skipBtn.setStyle(
                "-fx-background-color: #2A2A3E; -fx-text-fill: #888888; " +
                "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        skipBtn.setOnAction(e -> {
            stage.close();
            onComplete.run(); // move on to the next entry
        });

        HBox buttons = new HBox(8, logBtn, skipBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        root.getChildren().addAll(statusLabel, buttons);

        // Let JavaFX measure the content rather than guessing a fixed height
        Scene scene = new Scene(root);
        scene.setFill(Color.web("#1E1E2E"));
        stage.setScene(scene);
        stage.setWidth(560);
        stage.show();
    }

    /**
     * Builds a clickable card for a single subtask.
     * Clicking it selects the subtask (highlighted border) and deselects any previous selection.
     */
    private HBox buildSubtaskCard(JiraIssue subtask) {
        HBox card = new HBox(10);
        card.setPadding(new Insets(10, 14, 10, 14));
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle(cardStyle(false));
        card.setOnMouseClicked(e -> {
            // Visually deselect all cards, then highlight this one
            // We achieve this by storing the selected subtask and refreshing via the parent VBox
            selectedSubtask = subtask;
            // Re-style: walk the parent VBox children to reset all cards, highlight this one
            if (card.getParent() instanceof VBox parent) {
                parent.getChildren().stream()
                        .filter(node -> node instanceof HBox)
                        .forEach(node -> node.setStyle(cardStyle(false)));
            }
            card.setStyle(cardStyle(true));
        });

        Label key = new Label(subtask.key());
        key.setFont(Font.font("System", FontWeight.BOLD, 12));
        key.setTextFill(Color.web("#6C63FF"));
        key.setMinWidth(Region.USE_PREF_SIZE); // never truncate the ticket key

        Label summary = new Label(subtask.summary());
        summary.setTextFill(Color.WHITE);
        summary.setFont(Font.font(12));
        summary.setWrapText(true);
        HBox.setHgrow(summary, Priority.ALWAYS);

        card.getChildren().addAll(key, summary);
        return card;
    }

    /**
     * Called when the user clicks "Log time".
     * Runs the Jira API call on a background thread so the UI stays responsive,
     * then closes the window and calls onComplete on the JavaFX thread.
     */
    private void handleLogTime(Stage stage, Label statusLabel) {
        // Decide which key to log to — selected subtask, or parent if none
        String targetKey = selectedSubtask != null
                ? selectedSubtask.key()
                : JiraClient.extractKey(entry.url());
        String baseUrl = JiraClient.extractBaseUrl(entry.url());

        statusLabel.setTextFill(Color.web("#888888"));
        statusLabel.setText("Logging...");

        // API calls must NOT run on the JavaFX thread — that would freeze the UI
        // Platform.runLater() schedules code to run back on the JavaFX thread after the work is done
        new Thread(() -> {
            try {
                jiraClient.logWork(targetKey, baseUrl, date,
                        entry.timeRange().start(), entry.timeRange().end(),
                        entry.description());
                Platform.runLater(() -> {
                    stage.close();
                    onComplete.run();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    statusLabel.setTextFill(Color.web("#FF6B6B"));
                    statusLabel.setText("Error: " + ex.getMessage());
                });
            }
        }).start();
    }

    private String cardStyle(boolean selected) {
        return "-fx-background-color: #2A2A3E; -fx-background-radius: 8; " +
               "-fx-border-radius: 8; -fx-cursor: hand; " +
               "-fx-border-color: " + (selected ? "#6C63FF" : "transparent") + "; " +
               "-fx-border-width: 2;";
    }
}
