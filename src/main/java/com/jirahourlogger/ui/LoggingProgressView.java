package com.jirahourlogger.ui;

import com.jirahourlogger.api.JiraClient;
import com.jirahourlogger.model.LogEntry;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * LoggingProgressView shows a live status window while hours are being logged.
 *
 * Each log entry appears as a card with four possible states:
 *   PENDING  — grey bar,   waiting to be processed
 *   RUNNING  — purple bar, spinning arc + glow effect
 *   SUCCESS  — green bar,  bounce-in checkmark
 *   ERROR    — red bar,    red ✕
 *
 * All public methods are safe to call from any thread — they internally
 * schedule UI updates on the JavaFX thread via Platform.runLater().
 */
public class LoggingProgressView {

    public enum Status { PENDING, RUNNING, SUCCESS, ERROR }

    private final List<EntryCard> cards = new ArrayList<>();
    private int errorCount = 0;
    private Stage stage;
    private Label titleLabel;
    private ScrollPane scroll;   // kept as field so markRunning() can auto-scroll
    private Runnable onSuccess;  // called once after the window fades out (errors = 0 only)

    /**
     * Register a callback to run after everything logged successfully and the
     * window has finished its fade-out.  Call this before show().
     */
    public void setOnSuccess(Runnable callback) {
        this.onSuccess = callback;
    }

    /** Opens the window and shows all entries as PENDING. */
    public void show(List<LogEntry> entries) {
        stage = new Stage();
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setAlwaysOnTop(true);

        VBox root = new VBox();
        root.setStyle("-fx-background-color: #1E1E2E; -fx-background-radius: 14;");

        // ── Header ────────────────────────────────────────────────────────
        titleLabel = new Label("Logging hours…");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        titleLabel.setTextFill(Color.WHITE);

        Label countLabel = new Label(entries.size() + " entr" + (entries.size() == 1 ? "y" : "ies"));
        countLabel.setFont(Font.font(11));
        countLabel.setTextFill(Color.web("#555566"));

        Label closeBtn = new Label("✕");
        closeBtn.setTextFill(Color.web("#555566"));
        closeBtn.setFont(Font.font(13));
        closeBtn.setStyle("-fx-cursor: hand;");
        closeBtn.setOnMouseClicked(e -> stage.close());

        VBox titleBlock = new VBox(2, titleLabel, countLabel);
        HBox.setHgrow(titleBlock, Priority.ALWAYS);

        HBox header = new HBox();
        header.setPadding(new Insets(18, 18, 14, 18));
        header.setAlignment(Pos.CENTER_LEFT);
        header.getChildren().addAll(titleBlock, closeBtn);

        // ── Entry cards ───────────────────────────────────────────────────
        VBox list = new VBox(6);
        list.setPadding(new Insets(0, 14, 18, 14));

        for (LogEntry entry : entries) {
            EntryCard card = new EntryCard(entry);
            cards.add(card);
            list.getChildren().add(card.getNode());
        }

        // ── Scrollable container ──────────────────────────────────────────
        // Cap height at 520 px so the window never grows beyond the screen.
        // setFitToWidth keeps cards stretching to full width.
        // Horizontal scrollbar is hidden — cards always fit the fixed window width.
        scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroll.setMaxHeight(520);
        scroll.setStyle(
                "-fx-background: #1E1E2E;" +
                "-fx-background-color: #1E1E2E;" +
                "-fx-border-color: transparent;"
        );

        root.getChildren().addAll(header, scroll);

        // ── Slide-up + fade-in entrance ───────────────────────────────────
        root.setOpacity(0);
        root.setTranslateY(12);

        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        stage.setWidth(460);
        stage.show();

        FadeTransition fade = new FadeTransition(Duration.millis(220), root);
        fade.setToValue(1);

        TranslateTransition slide = new TranslateTransition(Duration.millis(220), root);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        new ParallelTransition(fade, slide).play();
    }

    // ── Public status setters (safe from any thread) ──────────────────────

    public void markRunning(int index) {
        Platform.runLater(() -> {
            cards.get(index).setStatus(Status.RUNNING);
            // Keep the active card visible — scroll proportionally through the list
            if (cards.size() > 1) {
                scroll.setVvalue((double) index / (cards.size() - 1));
            }
        });
    }

    public void markSuccess(int index) {
        Platform.runLater(() -> cards.get(index).setStatus(Status.SUCCESS));
    }

    public void markError(int index, String message) {
        Platform.runLater(() -> {
            cards.get(index).setStatus(Status.ERROR, message);
            errorCount++;
        });
    }

    /**
     * Call once all entries have been processed.
     * Updates the header and auto-closes after a pause when everything succeeded.
     */
    public void markAllDone() {
        Platform.runLater(() -> {
            if (errorCount > 0) {
                titleLabel.setText("Done — " + errorCount + " error" + (errorCount > 1 ? "s" : ""));
                titleLabel.setTextFill(Color.web("#FF6B6B"));
            } else {
                titleLabel.setText("All done ✓");
                titleLabel.setTextFill(Color.web("#4CAF50"));
                // Auto-close after 2.5 s when everything went well, then notify the caller
                new Timeline(new KeyFrame(Duration.millis(2500), e -> {
                    FadeTransition fadeOut = new FadeTransition(Duration.millis(200), stage.getScene().getRoot());
                    fadeOut.setToValue(0);
                    fadeOut.setOnFinished(ev -> {
                        stage.close();
                        if (onSuccess != null) onSuccess.run();
                    });
                    fadeOut.play();
                })).play();
            }
        });
    }

    // ═════════════════════════════════════════════════════════════════════
    // Inner class: one animated card per LogEntry
    // ═════════════════════════════════════════════════════════════════════

    private static class EntryCard {
        private final HBox card;
        private final Rectangle statusBar;
        private final StackPane indicatorPane;
        private Timeline spinTimeline;

        EntryCard(LogEntry entry) {
            // Left coloured stripe — colour changes with status
            statusBar = new Rectangle(4, 44);
            statusBar.setArcWidth(4);
            statusBar.setArcHeight(4);
            statusBar.setFill(Color.web("#333344"));

            // Time range + ticket key
            String timeText = entry.timeRange().start() + " – " + entry.timeRange().end();
            String key = JiraClient.extractKey(entry.url());

            Label time = new Label(timeText);
            time.setFont(Font.font("System", FontWeight.BOLD, 12));
            time.setTextFill(Color.web("#999999"));
            time.setMinWidth(Region.USE_PREF_SIZE);

            Label ticketKey = new Label(key);
            ticketKey.setFont(Font.font(12));
            ticketKey.setTextFill(Color.web("#6C63FF"));
            ticketKey.setMinWidth(Region.USE_PREF_SIZE);

            HBox topRow = new HBox(10, time, ticketKey);
            topRow.setAlignment(Pos.CENTER_LEFT);

            VBox content = new VBox(3, topRow);
            HBox.setHgrow(content, Priority.ALWAYS);

            // Optional description — trimmed to keep cards compact
            entry.description().ifPresent(desc -> {
                String trimmed = desc.length() > 65 ? desc.substring(0, 62) + "…" : desc;
                Label descLabel = new Label(trimmed);
                descLabel.setFont(Font.font(10));
                descLabel.setTextFill(Color.web("#555566"));
                content.getChildren().add(descLabel);
            });

            // Right-side indicator (pending dot → spinner → ✓ / ✕)
            indicatorPane = new StackPane();
            indicatorPane.setMinSize(24, 24);
            indicatorPane.setPrefSize(24, 24);
            indicatorPane.setMaxSize(24, 24);
            applyPending();

            card = new HBox(10);
            card.setAlignment(Pos.CENTER_LEFT);
            card.setPadding(new Insets(10, 12, 10, 0));
            card.setStyle(bg("#252535"));
            card.getChildren().addAll(statusBar, content, indicatorPane);
        }

        HBox getNode() { return card; }

        void setStatus(Status status) { setStatus(status, null); }

        void setStatus(Status status, String errorMsg) {
            stopSpinner();
            indicatorPane.getChildren().clear();

            switch (status) {
                case RUNNING -> {
                    statusBar.setFill(Color.web("#6C63FF"));
                    card.setStyle(bg("#25254A"));
                    card.setEffect(new DropShadow(16, Color.web("#6C63FF40")));
                    applySpinner();
                }
                case SUCCESS -> {
                    statusBar.setFill(Color.web("#4CAF50"));
                    card.setStyle(bg("#1A2E1A"));
                    card.setEffect(null);
                    applyCheckmark();
                }
                case ERROR -> {
                    statusBar.setFill(Color.web("#FF6B6B"));
                    card.setStyle(bg("#2E1A1A"));
                    card.setEffect(null);
                    applyError();
                }
                case PENDING -> {
                    statusBar.setFill(Color.web("#333344"));
                    card.setStyle(bg("#252535"));
                    card.setEffect(null);
                    applyPending();
                }
            }
        }

        // ── Indicator states ──────────────────────────────────────────────

        private void applyPending() {
            Label dot = new Label("·");
            dot.setTextFill(Color.web("#444455"));
            dot.setFont(Font.font(22));
            indicatorPane.getChildren().add(dot);
        }

        private void applySpinner() {
            // A 250° arc that appears to spin by animating its startAngle.
            // This avoids RotateTransition bounding-box issues with Arc nodes.
            Pane pane = new Pane();
            pane.setPrefSize(22, 22);
            pane.setMaxSize(22, 22);
            pane.setMinSize(22, 22);

            double cx = 11, cy = 11, r = 7.5;

            // Dim background ring
            Arc track = new Arc(cx, cy, r, r, 0, 360);
            track.setType(ArcType.OPEN);
            track.setStroke(Color.web("#6C63FF33"));
            track.setStrokeWidth(2);
            track.setFill(Color.TRANSPARENT);

            // Bright moving arc
            Arc arc = new Arc(cx, cy, r, r, 90, 250);
            arc.setType(ArcType.OPEN);
            arc.setStroke(Color.web("#6C63FF"));
            arc.setStrokeWidth(2);
            arc.setFill(Color.TRANSPARENT);
            arc.setStrokeLineCap(StrokeLineCap.ROUND);

            pane.getChildren().addAll(track, arc);
            indicatorPane.getChildren().add(pane);

            // Animate startAngle through 360° on a loop
            spinTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,        new KeyValue(arc.startAngleProperty(), 90.0)),
                new KeyFrame(Duration.millis(850), new KeyValue(arc.startAngleProperty(), 90.0 + 360.0, Interpolator.LINEAR))
            );
            spinTimeline.setCycleCount(Animation.INDEFINITE);
            spinTimeline.play();
        }

        private void applyCheckmark() {
            Label check = new Label("✓");
            check.setTextFill(Color.web("#4CAF50"));
            check.setFont(Font.font("System", FontWeight.BOLD, 14));
            check.setScaleX(0);
            check.setScaleY(0);
            indicatorPane.getChildren().add(check);

            // Overshoot then settle (same bounce pattern used in BubbleItem)
            ScaleTransition grow   = new ScaleTransition(Duration.millis(180), check);
            grow.setToX(1.3); grow.setToY(1.3);
            grow.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition settle = new ScaleTransition(Duration.millis(100), check);
            settle.setToX(1.0); settle.setToY(1.0);

            new SequentialTransition(grow, settle).play();
        }

        private void applyError() {
            Label x = new Label("✕");
            x.setTextFill(Color.web("#FF6B6B"));
            x.setFont(Font.font("System", FontWeight.BOLD, 13));
            indicatorPane.getChildren().add(x);
        }

        private void stopSpinner() {
            if (spinTimeline != null) {
                spinTimeline.stop();
                spinTimeline = null;
            }
        }

        private String bg(String color) {
            return "-fx-background-color: " + color + "; -fx-background-radius: 8;";
        }
    }
}
