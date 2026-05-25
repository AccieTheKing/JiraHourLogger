package com.jirahourlogger.ui;

import com.jirahourlogger.helper.AnimationHelper;
import com.jirahourlogger.helper.PaneHelper;
import com.jirahourlogger.helper.SphereHelper;
import com.jirahourlogger.helper.StageHelper;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.List;

public class MainFloatingSphere {
    private final StageHelper stageHelper;
    private final PaneHelper paneHelper;
    private final AnimationHelper animationHelper;
    private final SphereHelper sphereHelper;

    private boolean menuOpen    = false;
    private boolean dragging    = false;
    private boolean isAnimating = false; // guard against overlapping toggleMenu calls
    private double dragOffsetX, dragOffsetY;

    /**
     * The sphere's centre position on screen — updated after every drag.
     * toggleMenu() uses this to keep the sphere stationary while the stage resizes.
     */
    private double screenSphereX, screenSphereY;

    /** Stored so toggleMenu() can animate the sphere position. */
    private Circle sphere;

    private final List<BubbleItem> bubblesLeaves;

    public MainFloatingSphere() {
        this.stageHelper = new StageHelper();
        this.paneHelper = new PaneHelper();
        this.animationHelper = new AnimationHelper();
        this.sphereHelper = new SphereHelper();

        onBuildMainSphere();

        this.bubblesLeaves = onBuildLeaveBubbles();
        bubblesLeaves.forEach(b -> paneHelper.addChildren(b, b.getText()));
    }

    public void show() {
        this.stageHelper
                .initStyle(StageStyle.TRANSPARENT)
                .setAlwaysOnTop(true);

        // Pane stays at full size (220×220) always — the stage window acts as a clip.
        // When collapsed the window is 86×86, revealing only the sphere in the corner.
        this.paneHelper
                .setPrefSize(PaneHelper.PANEL_SIZE, PaneHelper.PANEL_SIZE)
                .setStyle("-fx-background-color: transparent;");

        // Start with a collapsed scene (86×86)
        Scene scene = new Scene(paneHelper.getPane(),
                SphereHelper.COLLAPSED_SIZE, SphereHelper.COLLAPSED_SIZE);
        scene.setFill(Color.TRANSPARENT);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();

        // Position stage so the sphere sits 20 px from the screen edge
        double initX = screen.getMaxX() - SphereHelper.COLLAPSED_SIZE - 20;
        double initY = screen.getMaxY() - SphereHelper.COLLAPSED_SIZE - 20;

        this.stageHelper
                .setPosition(initX, initY)
                .setScene(scene)
                .show();

        // Cache the sphere's screen position (sphere centre = stage top-left + SPHERE_INITIAL_POS)
        screenSphereX = initX + SphereHelper.SPHERE_INITIAL_POS;
        screenSphereY = initY + SphereHelper.SPHERE_INITIAL_POS;
    }

    /**
     * Animates the stage and sphere together so the sphere stays perfectly still
     * on screen while the canvas grows (opens) or shrinks (closes).
     *
     * Three values change in lock-step via a single Timeline:
     *   • sphere.centerX / centerY  — move within the pane
     *   • stage X / Y               — compensate so sphere stays at screenSphereX/Y
     *   • stage width / height      — the actual window resize
     */
    private void toggleMenu() {
        // Ignore rapid clicks while a resize/bubble animation is already running
        if (isAnimating) return;
        isAnimating = true;
        menuOpen = !menuOpen;

        double fromPos  = menuOpen ? SphereHelper.SPHERE_INITIAL_POS  : SphereHelper.SPHERE_EXPANDED_POS;
        double toPos    = menuOpen ? SphereHelper.SPHERE_EXPANDED_POS : SphereHelper.SPHERE_INITIAL_POS;
        double fromSize = menuOpen ? SphereHelper.COLLAPSED_SIZE       : PaneHelper.PANEL_SIZE;
        double toSize   = menuOpen ? PaneHelper.PANEL_SIZE             : SphereHelper.COLLAPSED_SIZE;

        // A single 0→1 property drives all three values proportionally
        DoubleProperty progress = new SimpleDoubleProperty(0.0);
        progress.addListener((obs, oldVal, newVal) -> {
            double p    = newVal.doubleValue();
            double pos  = fromPos  + p * (toPos  - fromPos);
            double size = fromSize + p * (toSize - fromSize);

            sphere.setCenterX(pos);
            sphere.setCenterY(pos);

            // Move the stage so  stageX + spherePanePos == screenSphereX  always
            stageHelper.setX(screenSphereX - pos);
            stageHelper.setY(screenSphereY - pos);
            stageHelper.setSize(size, size);
        });

        // Opening: expand first (0 ms), then pop bubbles in (80 ms head-start)
        // Closing: pop bubbles out immediately, shrink stage after they've hidden (160 ms)
        int resizeDuration = 200;
        int resizeDelay    = menuOpen ? 0   : 160;
        int bubbleDelay    = menuOpen ? 80  : 0;

        Timeline resize = new Timeline(
                new KeyFrame(Duration.ZERO,                  new KeyValue(progress, 0.0)),
                new KeyFrame(Duration.millis(resizeDuration), new KeyValue(progress, 1.0, Interpolator.EASE_BOTH))
        );
        resize.setDelay(Duration.millis(resizeDelay));
        resize.setOnFinished(e -> isAnimating = false); // allow the next click once animation completes
        resize.play();

        for (int i = 0; i < bubblesLeaves.size(); i++) {
            animationHelper.animateBubble(bubblesLeaves.get(i), menuOpen, bubbleDelay + i * 60);
        }
    }

    /**
     * Method to initialize the Main Sphere
     */
    private void onBuildMainSphere() {
        sphere = sphereHelper.buildMainSphere();

        sphere.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stageHelper.getX();
            dragOffsetY = e.getScreenY() - stageHelper.getY();
            dragging = false;
        });

        sphere.setOnMouseDragged(e -> {
            dragging = true;
            stageHelper.setX(e.getScreenX() - dragOffsetX);
            stageHelper.setY(e.getScreenY() - dragOffsetY);
            // Keep the cached screen position in sync so resize stays accurate after a drag
            screenSphereX = stageHelper.getX() + sphere.getCenterX();
            screenSphereY = stageHelper.getY() + sphere.getCenterY();
        });

        sphere.setOnMouseClicked(e -> {
            if (!dragging) toggleMenu();
        });

        paneHelper.addChildren(sphere);
    }

    /**
     * Method that uses a list to create leave bubbles
     *
     * @return List<BubbleItem>
     */
    private List<BubbleItem> onBuildLeaveBubbles() {
        return sphereHelper.buildBubbleLeaves(List.of(
                new BubbleDef("Notes", 135.0, "#FF6B6B")
//          new BubbleDef("Timer",    90.0,  "#FFD93D"),
//          new BubbleDef("Settings", 180.0, "#6BCB77")
        ));
    }
}
