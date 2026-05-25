package com.jirahourlogger.helper;

import com.jirahourlogger.ui.BubbleDef;
import com.jirahourlogger.ui.BubbleItem;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.*;
import javafx.scene.shape.Circle;

import java.util.List;

/**
 * This is a helper class that helps with the creation of Sphere formed objects,
 * now it's mainly Circles.
 *
 * @author Acdaling Edusei
 * @date 25-05-2026
 */
public class SphereHelper {
    public static final double SPHERE_RADIUS = 28;
    public static final double MARGIN = 15;
    public static final double DISTANCE = 85;

    private final double SPHERE_X_POS = PaneHelper.PANEL_SIZE - SPHERE_RADIUS - MARGIN;
    private final double SPHERE_Y_POS = PaneHelper.PANEL_SIZE - SPHERE_RADIUS - MARGIN;

    public Circle buildMainSphere() {
        Circle c = new Circle(SPHERE_X_POS, SPHERE_Y_POS, SPHERE_RADIUS);
        c.setFill(new RadialGradient(
                0, 0, 0.35, 0.35, 1, true,
                CycleMethod.NO_CYCLE, new Stop(0, Color.web("#9C8FFF")),
                new Stop(1, Color.web("#5A52D5"))
        ));
        c.setEffect(new DropShadow(20, Color.web("#6C63FFAA")));
        c.setStyle("-fx-cursor: hand;");
        return c;
    }

    public List<BubbleItem> buildBubbleLeaves(List<BubbleDef> defs) {
        return defs.stream().map(def -> {
            double rad = Math.toRadians(def.angle());
            double bx = SPHERE_X_POS + Math.cos(rad) * DISTANCE;
            double by = SPHERE_Y_POS - Math.sin(rad) * DISTANCE;
            return new BubbleItem(bx, by, def.label(), def.color());
        }).toList();
    }
}
