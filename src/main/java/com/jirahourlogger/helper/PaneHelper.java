package com.jirahourlogger.helper;

import javafx.scene.Node;
import javafx.scene.layout.Pane;

public class PaneHelper {
    public static final double PANEL_SIZE = 220;

    private final Pane pane;

    public PaneHelper() {
        this.pane = new Pane();
    }

    public PaneHelper setPrefSize(double width, double height) {
        pane.setPrefSize(width, height);
        return this;
    }

    public PaneHelper setStyle(String style) {
        pane.setStyle(style);
        return this;
    }

    public PaneHelper addChildren(Node... nodes) {
        pane.getChildren().addAll(nodes);
        return this;
    }

    public Pane getPane() {
        return pane;
    }
}
