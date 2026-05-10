package com.jirahourlogger.ui;

import javafx.animation.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

public class FloatingSphere {

    private static final double SPHERE_RADIUS = 28;
    private static final double BUBBLE_RADIUS = 22;
    private static final double PANEL_SIZE    = 220;
    private static final double MARGIN        = 15;
    private static final double DISTANCE      = 85;

    private Stage stage;
    private boolean menuOpen = false;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;

    private final List<BubbleItem> bubbles = new ArrayList<>();

    public void show() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        Pane root = new Pane();
        root.setPrefSize(PANEL_SIZE, PANEL_SIZE);
        root.setStyle("-fx-background-color: transparent;");

        double sphereCX = PANEL_SIZE - SPHERE_RADIUS - MARGIN;
        double sphereCY = PANEL_SIZE - SPHERE_RADIUS - MARGIN;

        // angle (math convention: 0° = right, 90° = up), color, label
        Object[][] bubbleDefs = {
            {"Notes",    135.0, "#FF6B6B"},
            {"Timer",     90.0, "#FFD93D"},
            {"Settings", 180.0, "#6BCB77"},
        };

        for (Object[] def : bubbleDefs) {
            String label = (String)  def[0];
            double angle = (double)  def[1];
            String color = (String)  def[2];
            double rad = Math.toRadians(angle);
            double bx = sphereCX + Math.cos(rad) * DISTANCE;
            double by = sphereCY - Math.sin(rad) * DISTANCE;
            BubbleItem bubble = buildBubble(bx, by, label, color);
            bubbles.add(bubble);
            root.getChildren().addAll(bubble.circle(), bubble.label());
        }

        Circle sphere = buildSphere(sphereCX, sphereCY);
        root.getChildren().add(sphere); // sphere on top of bubbles

        sphere.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
            dragging = false;
        });
        sphere.setOnMouseDragged(e -> {
            dragging = true;
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });
        sphere.setOnMouseClicked(e -> {
            if (!dragging) toggleMenu();
        });

        Scene scene = new Scene(root, PANEL_SIZE, PANEL_SIZE);
        scene.setFill(Color.TRANSPARENT);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX(screen.getMaxX() - PANEL_SIZE - 20);
        stage.setY(screen.getMaxY() - PANEL_SIZE - 20);
        stage.setScene(scene);
        stage.show();
    }

    private Circle buildSphere(double cx, double cy) {
        Circle c = new Circle(cx, cy, SPHERE_RADIUS);
        c.setFill(new RadialGradient(
            0, 0, 0.35, 0.35, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#9C8FFF")),
            new Stop(1, Color.web("#5A52D5"))
        ));
        c.setEffect(new DropShadow(20, Color.web("#6C63FFAA")));
        c.setStyle("-fx-cursor: hand;");
        return c;
    }

    private BubbleItem buildBubble(double cx, double cy, String label, String color) {
        Circle c = new Circle(cx, cy, BUBBLE_RADIUS);
        c.setFill(Color.web(color));
        c.setOpacity(0);
        c.setScaleX(0);
        c.setScaleY(0);
        c.setEffect(new DropShadow(10, Color.web("#00000055")));
        c.setStyle("-fx-cursor: hand;");

        Text t = new Text(label);
        t.setFill(Color.WHITE);
        t.setFont(Font.font("System", FontWeight.BOLD, 9));
        t.setX(cx - label.length() * 3.0);
        t.setY(cy + BUBBLE_RADIUS + 12);
        t.setOpacity(0);

        if (label.equals("Notes")) {
            c.setOnMouseClicked(e -> {
                toggleMenu();
                new NotesView().show();
            });
        }

        return new BubbleItem(c, t);
    }

    private void toggleMenu() {
        menuOpen = !menuOpen;
        for (int i = 0; i < bubbles.size(); i++) {
            animateBubble(bubbles.get(i), menuOpen, i * 60);
        }
    }

    private void animateBubble(BubbleItem b, boolean show, int delayMs) {
        Duration delay = Duration.millis(delayMs);

        ScaleTransition scale = new ScaleTransition(Duration.millis(300), b.circle());
        scale.setDelay(delay);
        scale.setToX(show ? 1 : 0);
        scale.setToY(show ? 1 : 0);
        scale.setInterpolator(show
            ? Interpolator.SPLINE(0.34, 1.56, 0.64, 1)  // spring overshoot
            : Interpolator.EASE_IN);

        FadeTransition fadeCircle = new FadeTransition(Duration.millis(200), b.circle());
        fadeCircle.setDelay(delay);
        fadeCircle.setToValue(show ? 1 : 0);

        FadeTransition fadeLabel = new FadeTransition(Duration.millis(200), b.label());
        fadeLabel.setDelay(show ? delay.add(Duration.millis(100)) : delay);
        fadeLabel.setToValue(show ? 1 : 0);

        scale.play();
        fadeCircle.play();
        fadeLabel.play();
    }

    record BubbleItem(Circle circle, Text label) {}
}
