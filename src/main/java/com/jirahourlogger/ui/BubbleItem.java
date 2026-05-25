package com.jirahourlogger.ui;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * BubbleItem represents a single leaf bubble in the radial menu.
 *
 * It extends Circle — meaning a BubbleItem IS a Circle, inheriting all of its
 * properties (center, radius, fill, etc.) while adding an extra label (Text)
 * that floats below the circle on screen.
 *
 * WHY EXTEND CIRCLE?
 * AnimationHelper animates both the circle and its label separately.
 * Having BubbleItem extend Circle means we can pass it directly to any
 * JavaFX API that expects a Circle — no unwrapping needed.
 *
 * The bubble starts invisible (opacity=0, scale=0) and is animated into
 * view by AnimationHelper when the menu opens.
 *
 * NOTE: The Text node is a sibling of the circle in the Pane (not a child),
 * because Pane uses absolute positioning — you must add both the circle
 * AND the text to the pane separately. That is why MainFloatingSphere does:
 *   paneHelper.addChildren(bubble, bubble.getText())
 */
public class BubbleItem extends Circle {

    // The radius (half-width) of every leaf bubble in pixels
    private static final double BUBBLE_RADIUS = 22;

    // The text label that appears below this bubble
    private final Text text;

    /**
     * Creates a BubbleItem at the given position with a label and colour.
     *
     * @param cx    the x coordinate of the bubble's centre on the pane
     * @param cy    the y coordinate of the bubble's centre on the pane
     * @param label the text shown below the bubble (e.g. "Notes")
     * @param color a hex colour string for the circle fill (e.g. "#FF6B6B")
     */
    public BubbleItem(double cx, double cy, String label, String color) {
        // Configure the circle (the parent class)
        this.setCenterX(cx);
        this.setCenterY(cy);
        this.setRadius(BUBBLE_RADIUS);
        this.setFill(Color.web(color));

        // Start fully invisible and at scale 0 so AnimationHelper can animate them in
        this.setOpacity(0);
        this.setScaleX(0);
        this.setScaleY(0);

        // Soft drop shadow gives the bubble a subtle depth on screen
        this.setEffect(new DropShadow(10, Color.web("#00000055")));

        // Show a hand cursor when hovering so the user knows it is clickable
        this.setStyle("-fx-cursor: hand;");

        // ── Label ────────────────────────────────────────────────────────────
        this.text = new Text(label);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("System", FontWeight.BOLD, 9));
        text.setOpacity(0); // also starts invisible; AnimationHelper fades it in

        // Position the label horizontally centred under the bubble.
        // label.length() * 3.0 is a rough estimate of half the text width in pixels.
        text.setX(cx - label.length() * 3.0);
        text.setY(cy + BUBBLE_RADIUS + 12); // 12 px gap below the bottom of the circle

        // ── Click actions ────────────────────────────────────────────────────
        // Each label type maps to a different action. "Notes" opens the Notes window.
        if (label.equals("Notes")) {
            this.setOnMouseClicked(e -> new NotesView().show());
        }
    }

    /**
     * Returns the Text label node so it can be added to the parent Pane separately.
     * (See the note in the class Javadoc about why Text is a sibling, not a child.)
     */
    public Text getText() {
        return text;
    }

    /**
     * Returns this BubbleItem as a plain Circle reference.
     * Used by AnimationHelper, which receives a Circle to animate.
     * Since BubbleItem extends Circle, `this` already IS a Circle — this
     * method just makes the intent explicit.
     */
    public Circle getCircle() {
        return this;
    }
}
