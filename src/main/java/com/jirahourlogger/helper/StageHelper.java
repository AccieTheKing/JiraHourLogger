package com.jirahourlogger.helper;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * StageHelper is a wrapper around JavaFX's Stage class.
 *
 * In JavaFX, a Stage represents an OS window. Setting it up requires several
 * separate method calls, which can clutter the calling code. This helper uses
 * a pattern called "method chaining" (or "builder pattern") — each setter
 * returns `this` (the StageHelper itself), so you can chain calls like:
 *
 *   stageHelper
 *       .initStyle(StageStyle.TRANSPARENT)
 *       .setAlwaysOnTop(true)
 *       .setScene(scene)
 *       .show();
 *
 * Without this wrapper, you'd have to write four separate lines each referencing
 * the stage variable. Method chaining reads more like a description of what you
 * want and less like a series of one-off commands.
 */
public class StageHelper {
    // The real JavaFX Stage that this helper wraps
    private final Stage stage;

    /**
     * Constructor — creates a new blank Stage (OS window).
     * The stage has no style, scene, or position yet; those are set via the
     * setter methods below.
     */
    public StageHelper() {
        this.stage = new Stage();
    }

    /**
     * Sets the visual style of the window before it is shown.
     * Must be called before show() — JavaFX does not allow changing the style
     * after the window has been displayed.
     *
     * Common styles:
     *   StageStyle.TRANSPARENT — no window border or title bar; background can be see-through
     *   StageStyle.UNDECORATED — no title bar, but has an opaque white background
     *   StageStyle.DECORATED   — the default OS window with a title bar and close button
     *
     * Returns `this` so the call can be chained.
     */
    public StageHelper initStyle(StageStyle style) {
        stage.initStyle(style);
        return this;
    }

    /**
     * When true, this window will always appear on top of all other OS windows,
     * even when it is not the focused window. Useful for floating UI elements.
     *
     * Returns `this` so the call can be chained.
     */
    public StageHelper setAlwaysOnTop(boolean alwaysOnTop) {
        stage.setAlwaysOnTop(alwaysOnTop);
        return this;
    }

    /**
     * Attaches a Scene (the visible content) to this Stage (the window frame).
     * A Stage without a Scene will appear as an empty window.
     *
     * Returns `this` so the call can be chained.
     */
    public StageHelper setScene(Scene scene) {
        stage.setScene(scene);
        return this;
    }

    /**
     * Moves the window to the given screen coordinates.
     * (0, 0) is the top-left corner of the primary monitor.
     * x increases to the right; y increases downward.
     *
     * Returns `this` so the call can be chained.
     */
    public StageHelper setPosition(double x, double y) {
        stage.setX(x);
        stage.setY(y);
        return this;
    }

    /** Returns the current X position of the window's top-left corner on screen. */
    public double getX() {
        return stage.getX();
    }

    /** Returns the current Y position of the window's top-left corner on screen. */
    public double getY() {
        return stage.getY();
    }

    /** Moves the window horizontally to the given screen X coordinate. */
    public void setX(double x) {
        stage.setX(x);
    }

    /** Moves the window vertically to the given screen Y coordinate. */
    public void setY(double y) {
        stage.setY(y);
    }

    /**
     * Resizes the window to the given width and height (in pixels).
     * Note: the Scene inside the window may clip or scroll if it is larger than this size.
     */
    public void setSize(double width, double height) {
        stage.setWidth(width);
        stage.setHeight(height);
    }

    /**
     * Makes the window visible on screen.
     * Nothing appears until this is called — all the setup above is just configuration.
     */
    public void show() {
        stage.show();
    }

    /** Hides and destroys the window. The Stage cannot be shown again after this. */
    public void close() {
        stage.close();
    }
}
