package com.jirahourlogger.helper;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;

public class SphereHelper {

    public Circle buildMainSphere(double cx, double cy, double radius) {
        Circle c = new Circle(cx, cy, radius);
        c.setFill(new RadialGradient(
                0, 0, 0.35, 0.35, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#9C8FFF")),
                new Stop(1, Color.web("#5A52D5"))
        ));
        c.setEffect(new DropShadow(20, Color.web("#6C63FFAA")));
        c.setStyle("-fx-cursor: hand;");
        return c;
    }

    public static Circle buildBubbleCircle(double cx, double cy, double radius, String color) {
        Circle c = new Circle(cx, cy, radius);
        c.setFill(Color.web(color));
        c.setOpacity(0);
        c.setScaleX(0);
        c.setScaleY(0);
        c.setEffect(new DropShadow(10, Color.web("#00000055")));
        c.setStyle("-fx-cursor: hand;");
        return c;
    }
}
