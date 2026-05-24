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

import java.util.ArrayList;
import java.util.List;

public class MainFloatingSphere {

    private static final double SPHERE_RADIUS = 28;
    private static final double MARGIN = 15;
    private static final double DISTANCE = 85;

    private final StageHelper stageHelper;
    private final PaneHelper paneHelper;
    private final AnimationHelper animationHelper;
    private final SphereHelper sphereHelper;

    private boolean menuOpen = false;
    private boolean dragging = false;
    private double dragOffsetX, dragOffsetY;

    // List where all the created bubbles will live
    private final List<BubbleItem> bubbles = new ArrayList<>();

    // Each bubbles needs a calculated position
    double sphereCXPosition = PaneHelper.PANEL_SIZE - SPHERE_RADIUS - MARGIN;
    double sphereCYPosition = PaneHelper.PANEL_SIZE - SPHERE_RADIUS - MARGIN;

    Object[][] bubbleLeaves = {
            {"Notes", 135.0, "#FF6B6B"},
//              {"Timer",     90.0, "#FFD93D"},
//              {"Settings", 180.0, "#6BCB77"},
    };

    public MainFloatingSphere() {
        this.stageHelper = new StageHelper();
        this.paneHelper = new PaneHelper();
        this.animationHelper = new AnimationHelper();
        this.sphereHelper = new SphereHelper();
    }

    public void show() {
        this.stageHelper
                .initStyle(StageStyle.TRANSPARENT)
                .setAlwaysOnTop(true);

        this.paneHelper
                .setPrefSize(PaneHelper.PANEL_SIZE, PaneHelper.PANEL_SIZE)
                .setStyle("-fx-background-color: red;");

        for (Object[] def : bubbleLeaves) {
            String label = (String) def[0];
            double angle = (double) def[1];
            String color = (String) def[2];

            double rad = Math.toRadians(angle);
            double bx = sphereCXPosition + Math.cos(rad) * DISTANCE;
            double by = sphereCYPosition - Math.sin(rad) * DISTANCE;

            BubbleItem bubble = new BubbleItem(bx, by, label, color);
            bubbles.add(bubble);
            paneHelper.addChildren(bubble);
        }

        Circle sphere = sphereHelper.buildMainSphere(sphereCXPosition, sphereCYPosition, SPHERE_RADIUS);
        paneHelper.addChildren(sphere);

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
        for (int i = 0; i < bubbles.size(); i++) {
            animationHelper.animateBubble(bubbles.get(i), menuOpen, i * 60);
        }
    }
}
