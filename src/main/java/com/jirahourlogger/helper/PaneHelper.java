package com.jirahourlogger.helper;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * PaneHelper is a wrapper around JavaFX's Pane class.
 *
 * A Pane is the simplest JavaFX layout container — it holds child nodes
 * (circles, labels, etc.) and positions them using absolute coordinates
 * (i.e. you set the exact x/y position of each child yourself).
 *
 * This helper uses the same method-chaining (builder) pattern as StageHelper:
 * each setter returns `this` so multiple calls can be written as one chain.
 *
 *   paneHelper
 *       .setPrefSize(220, 220)
 *       .setStyle("-fx-background-color: transparent;")
 *       .addChildren(sphere, label);
 *
 * The constant PANEL_SIZE is defined here because the pane's size (220 px)
 * is shared by multiple other classes that need to calculate positions
 * relative to that size.
 */
public class PaneHelper {

    /**
     * The size (width and height, in pixels) of the expanded floating panel.
     * Both the collapsed stage window and the bubble positions are calculated
     * relative to this value, so changing it here updates everything at once.
     */
    public static final double PANEL_SIZE = 220;

    // The real JavaFX Pane that this helper wraps
    private final Pane pane;

    /**
     * Constructor — creates a new empty Pane with no children or styling.
     */
    public PaneHelper() {
        this.pane = new Pane();
    }

    /**
     * Sets the preferred width and height of the pane.
     * "Preferred" means JavaFX will try to make it this size; it may be
     * overridden if the pane's parent layout forces a different size.
     *
     * Returns `this` so the call can be chained.
     */
    public PaneHelper setPrefSize(double width, double height) {
        pane.setPrefSize(width, height);
        return this;
    }

    /**
     * Applies a CSS style string to the pane.
     * JavaFX uses a subset of CSS — for example:
     *   "-fx-background-color: transparent;" makes the background invisible.
     *   "-fx-background-color: #1E1E2E;"     sets a dark navy background.
     *
     * Returns `this` so the call can be chained.
     */
    public PaneHelper setStyle(String style) {
        pane.setStyle(style);
        return this;
    }

    /**
     * Adds one or more child nodes (circles, labels, etc.) to the pane.
     * The varargs syntax (Node...) means you can pass any number of nodes
     * separated by commas:
     *   addChildren(sphere)
     *   addChildren(sphere, label, icon)
     *
     * Returns `this` so the call can be chained.
     */
    public PaneHelper addChildren(Node... nodes) {
        pane.getChildren().addAll(nodes);
        return this;
    }

    /**
     * Returns the underlying JavaFX Pane so it can be passed to a Scene or
     * other JavaFX APIs that expect a Pane directly.
     */
    public Pane getPane() {
        return pane;
    }
}
