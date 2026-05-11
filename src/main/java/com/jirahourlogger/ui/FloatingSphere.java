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

/**
 * FloatingSphere creates the always-on-top circular widget that lives on
 * the user's desktop. It is the main entry point for the app's UI.
 *
 * KEY JAVAFX CONCEPTS USED HERE:
 *
 *   Stage  — an OS-level window. We configure ours to be transparent and
 *             always on top so it floats over everything else on screen.
 *
 *   Scene  — the content inside a Stage. Think of Stage as a picture frame
 *             and Scene as the canvas inside it.
 *
 *   Pane   — a layout container. Unlike VBox/HBox which arrange children
 *             automatically, Pane lets us position things at exact X/Y coordinates.
 *             We need this because the sphere and bubbles are placed precisely.
 *
 *   Circle — a JavaFX shape. We use it for both the main sphere and the bubbles.
 *
 *   Animation — JavaFX has a rich animation system. We use:
 *     ScaleTransition  → smoothly changes the size of a node (0 → 1 = grow in)
 *     FadeTransition   → smoothly changes the opacity (0 = invisible, 1 = fully visible)
 */
public class FloatingSphere {

    // --- Layout constants ---
    // Defining sizes as constants makes it easy to tweak the look later.
    private static final double SPHERE_RADIUS = 28;   // radius of the main sphere in pixels
    private static final double BUBBLE_RADIUS = 22;   // radius of each small bubble
    private static final double PANEL_SIZE    = 220;  // total width/height of the transparent window
    private static final double MARGIN        = 15;   // gap between sphere edge and window edge
    private static final double DISTANCE      = 85;   // how far bubbles travel from sphere center

    private Stage stage;

    // Tracks whether the bubble menu is currently open or closed
    private boolean menuOpen = false;

    // We need to tell the difference between a click and a drag.
    // If the user presses and moves the mouse, it's a drag (move window).
    // If they press and release without moving, it's a click (toggle menu).
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;

    // All the bubble items around the sphere (Notes, Timer, Settings)
    private final List<BubbleItem> bubbles = new ArrayList<>();

    /**
     * Creates the window, builds all UI nodes, and shows it on screen.
     */
    public void show() {
        stage = new Stage();

        // TRANSPARENT removes the OS window border/chrome entirely.
        // Combined with a transparent Scene background, only our drawn shapes are visible.
        stage.initStyle(StageStyle.TRANSPARENT);

        // Always render this window above all other windows on screen.
        stage.setAlwaysOnTop(true);

        // Pane is our root container. Everything is placed at absolute X/Y positions.
        Pane root = new Pane();
        root.setPrefSize(PANEL_SIZE, PANEL_SIZE);
        root.setStyle("-fx-background-color: transparent;"); // make the pane itself invisible

        // Calculate where the sphere should sit: bottom-right corner of the pane,
        // offset inward by MARGIN so it's not right at the edge.
        double sphereCX = PANEL_SIZE - SPHERE_RADIUS - MARGIN; // e.g. 220 - 28 - 15 = 177
        double sphereCY = PANEL_SIZE - SPHERE_RADIUS - MARGIN;

        // Define each bubble: label, angle, and color.
        // Angles follow standard math convention: 0° = right, 90° = up, 180° = left.
        // We fan the bubbles upward and to the left since the sphere is at the bottom-right.
        Object[][] bubbleDefs = {
            {"Notes",    135.0, "#FF6B6B"}, // upper-left diagonal, red
            {"Timer",     90.0, "#FFD93D"}, // straight up, yellow
            {"Settings", 180.0, "#6BCB77"}, // straight left, green
        };

        // Build each bubble and add it to the pane BEFORE the sphere,
        // so the sphere renders on top (later children = rendered on top in JavaFX).
        for (Object[] def : bubbleDefs) {
            String label = (String) def[0];
            double angle = (double) def[1];
            String color = (String) def[2];

            // Convert degrees to radians (Java's Math functions use radians)
            double rad = Math.toRadians(angle);

            // Calculate the bubble's center position using trigonometry:
            //   bx = sphereCenter + cos(angle) * distance
            //   by = sphereCenter - sin(angle) * distance  ← minus because Y increases downward in screen coords
            double bx = sphereCX + Math.cos(rad) * DISTANCE;
            double by = sphereCY - Math.sin(rad) * DISTANCE;

            BubbleItem bubble = buildBubble(bx, by, label, color);
            bubbles.add(bubble);

            // Add both the circle shape and its text label to the pane
            root.getChildren().addAll(bubble.circle(), bubble.label());
        }

        // Build the main sphere and add it last so it sits on top of the bubbles
        Circle sphere = buildSphere(sphereCX, sphereCY);
        root.getChildren().add(sphere);

        // --- Mouse event handlers for the sphere ---

        // Record where the mouse pressed relative to the window's top-left corner.
        // This offset is used to move the window smoothly during a drag.
        sphere.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
            dragging = false; // reset for each new press
        });

        // While the mouse is held and moving, reposition the window.
        sphere.setOnMouseDragged(e -> {
            dragging = true;
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        // A click fires after both press and release.
        // We only toggle the menu if the user didn't drag.
        sphere.setOnMouseClicked(e -> {
            if (!dragging) toggleMenu();
        });

        // Create the Scene with a transparent background.
        // The Color.TRANSPARENT fill is what makes the areas outside our shapes see-through.
        Scene scene = new Scene(root, PANEL_SIZE, PANEL_SIZE);
        scene.setFill(Color.TRANSPARENT);

        // Position the entire window at the bottom-right of the primary monitor.
        // Screen.getPrimary().getVisualBounds() gives the usable screen area
        // (excluding the macOS menu bar and dock).
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setX(screen.getMaxX() - PANEL_SIZE - 20);
        stage.setY(screen.getMaxY() - PANEL_SIZE - 20);

        stage.setScene(scene);
        stage.show();
    }

    /**
     * Builds the main purple sphere with a gradient fill and glow effect.
     *
     * RadialGradient creates a circular gradient — lighter in the top-left
     * (giving a 3D shiny look), darker toward the bottom-right.
     *
     * DropShadow adds a coloured glow around the circle, making it feel
     * like it's emitting light.
     */
    private Circle buildSphere(double cx, double cy) {
        Circle c = new Circle(cx, cy, SPHERE_RADIUS);

        // RadialGradient(focusAngle, focusDistance, centerX, centerY, radius,
        //                proportional, cycleMethod, stops...)
        // proportional=true means coordinates are 0.0–1.0 fractions of the shape's size
        c.setFill(new RadialGradient(
            0, 0, 0.35, 0.35, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#9C8FFF")), // light purple at the highlight
            new Stop(1, Color.web("#5A52D5"))  // dark purple at the edge
        ));

        // DropShadow(radius, color) — creates a soft glowing outline
        c.setEffect(new DropShadow(20, Color.web("#6C63FFAA"))); // AA = 67% opacity

        // Changes the mouse cursor to a hand pointer when hovering over the sphere
        c.setStyle("-fx-cursor: hand;");

        return c;
    }

    /**
     * Builds a single bubble with its label text.
     * Bubbles start invisible and scaled to zero — they animate in when the menu opens.
     *
     * @param cx     center X of the bubble
     * @param cy     center Y of the bubble
     * @param label  text shown below the bubble (e.g. "Notes")
     * @param color  hex color string for the bubble fill
     */
    private BubbleItem buildBubble(double cx, double cy, String label, String color) {
        Circle c = new Circle(cx, cy, BUBBLE_RADIUS);
        c.setFill(Color.web(color));

        // Start fully invisible and at scale 0 (zero size).
        // The animation will transition these values to 1 when the menu opens.
        c.setOpacity(0);
        c.setScaleX(0);
        c.setScaleY(0);

        c.setEffect(new DropShadow(10, Color.web("#00000055"))); // subtle dark shadow
        c.setStyle("-fx-cursor: hand;");

        // The label text that appears below the bubble circle
        Text t = new Text(label);
        t.setFill(Color.WHITE);
        t.setFont(Font.font("System", FontWeight.BOLD, 9));
        t.setOpacity(0); // also starts hidden

        // Position the text centered horizontally under the bubble.
        // label.length() * 3.0 is an approximation of half the text width.
        t.setX(cx - label.length() * 3.0);
        t.setY(cy + BUBBLE_RADIUS + 12); // 12px below the bubble's bottom edge

        // Wire up click actions per bubble.
        // Right now only "Notes" does something; Timer and Settings are ready to be built.
        if (label.equals("Notes")) {
            c.setOnMouseClicked(e -> {
                toggleMenu(); // close the bubble menu first
                new NotesView().show(); // then open the notes window
            });
        }

        return new BubbleItem(c, t);
    }

    /**
     * Opens or closes the bubble menu by animating all bubbles in or out.
     * Each bubble is staggered by 60ms so they appear one after another.
     */
    private void toggleMenu() {
        menuOpen = !menuOpen;
        for (int i = 0; i < bubbles.size(); i++) {
            animateBubble(bubbles.get(i), menuOpen, i * 60);
        }
    }

    /**
     * Animates a single bubble appearing (show=true) or disappearing (show=false).
     *
     * FadeTransition changes opacity over time (0=invisible, 1=fully visible).
     *
     * APPEARING — spring/bounce effect using two chained ScaleTransitions:
     *   Step 1: scale 0 → 1.2  (overshoot slightly past full size)
     *   Step 2: scale 1.2 → 1.0 (snap back to normal)
     *   This simulates a spring without needing an interpolator that goes outside [0,1].
     *   SequentialTransition runs them one after the other automatically.
     *
     * DISAPPEARING — a single ScaleTransition shrinks the bubble back to 0.
     *
     * @param b        the bubble to animate
     * @param show     true = animate in, false = animate out
     * @param delayMs  how many milliseconds to wait before starting this animation
     */
    private void animateBubble(BubbleItem b, boolean show, int delayMs) {
        Duration delay = Duration.millis(delayMs);

        if (show) {
            // Step 1: grow from 0 to 1.2 (slightly overshooting full size)
            ScaleTransition overshoot = new ScaleTransition(Duration.millis(200), b.circle());
            overshoot.setFromX(0);
            overshoot.setFromY(0);
            overshoot.setToX(1.2);
            overshoot.setToY(1.2);
            overshoot.setInterpolator(Interpolator.EASE_OUT);

            // Step 2: settle back from 1.2 to exactly 1.0
            ScaleTransition settle = new ScaleTransition(Duration.millis(100), b.circle());
            settle.setToX(1.0);
            settle.setToY(1.0);
            settle.setInterpolator(Interpolator.EASE_IN);

            // SequentialTransition runs overshoot, then settle, one after the other
            SequentialTransition bounce = new SequentialTransition(overshoot, settle);
            bounce.setDelay(delay);
            bounce.play();
        } else {
            // Shrink smoothly back to zero
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), b.circle());
            scale.setDelay(delay);
            scale.setToX(0);
            scale.setToY(0);
            scale.setInterpolator(Interpolator.EASE_IN);
            scale.play();
        }

        FadeTransition fadeCircle = new FadeTransition(Duration.millis(200), b.circle());
        fadeCircle.setDelay(delay);
        fadeCircle.setToValue(show ? 1 : 0);

        // The label fades in slightly after the circle so the circle appears first
        FadeTransition fadeLabel = new FadeTransition(Duration.millis(200), b.label());
        fadeLabel.setDelay(show ? delay.add(Duration.millis(100)) : delay);
        fadeLabel.setToValue(show ? 1 : 0);

        fadeCircle.play();
        fadeLabel.play();
    }

    /**
     * A simple record to group a bubble's Circle shape and its Text label together.
     *
     * `record` is a Java 16+ feature that auto-generates a constructor, getters,
     * equals(), hashCode(), and toString(). It's a concise way to create
     * simple data-holding classes.
     */
    record BubbleItem(Circle circle, Text label) {}
}
