package com.jirahourlogger.ui;

import com.jirahourlogger.helper.AnimationHelper;
import com.jirahourlogger.helper.PaneHelper;
import com.jirahourlogger.helper.SphereHelper;
import com.jirahourlogger.helper.StageHelper;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Screen;
import javafx.stage.StageStyle;

import java.util.List;

public class MainFloatingSphere {
    private final StageHelper stageHelper;
    private final PaneHelper paneHelper;
    private final AnimationHelper animationHelper;
    private final SphereHelper sphereHelper;

    private boolean menuOpen = false;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;

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

        this.paneHelper
                .setPrefSize(PaneHelper.PANEL_SIZE, PaneHelper.PANEL_SIZE)
                .setStyle("-fx-background-color: red;"); // Should be transparent in production

        // Scene on the pane
        Scene scene = new Scene(paneHelper.getPane(), PaneHelper.PANEL_SIZE, PaneHelper.PANEL_SIZE);
        scene.setFill(Color.TRANSPARENT);

        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        this.stageHelper
                .setPosition(screen.getMaxX() - PaneHelper.PANEL_SIZE - 20, screen.getMaxY() - PaneHelper.PANEL_SIZE - 20)
                .setScene(scene)
                .show();
    }

    private void toggleMenu() {
        menuOpen = !menuOpen;
        for (int i = 0; i < bubblesLeaves.size(); i++) {
            animationHelper.animateBubble(bubblesLeaves.get(i), menuOpen, i * 60);
        }
    }

    /**
     * Method to initialize the Main Sphere
     */
    private void onBuildMainSphere() {
        // Main sphere circle
        Circle sphere = sphereHelper.buildMainSphere();

        // Adding mouse events to the main sphere
        sphere.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stageHelper.getX();
            dragOffsetY = e.getScreenY() - stageHelper.getY();
            dragging = false;
        });

        sphere.setOnMouseDragged(e -> {
            dragging = true;
            stageHelper.setX(e.getScreenX() - dragOffsetX);
            stageHelper.setY(e.getScreenY() - dragOffsetY);
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
