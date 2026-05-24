package com.jirahourlogger.helper;

import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class StageHelper {
    private final Stage stage;

    public StageHelper() {
        this.stage = new Stage();
    }

    public StageHelper initStyle(StageStyle style) {
        stage.initStyle(style);
        return this;
    }

    public StageHelper setAlwaysOnTop(boolean alwaysOnTop) {
        stage.setAlwaysOnTop(alwaysOnTop);
        return this;
    }

    public StageHelper setScene(Scene scene) {
        stage.setScene(scene);
        return this;
    }

    public StageHelper setPosition(double x, double y) {
        stage.setX(x);
        stage.setY(y);
        return this;
    }

    public double getX() {
        return stage.getX();
    }

    public double getY() {
        return stage.getY();
    }

    public void setX(double x) {
        stage.setX(x);
    }

    public void setY(double y) {
        stage.setY(y);
    }

    public void show() {
        stage.show();
    }

    public void close() {
        stage.close();
    }
}
