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
    public static final double MARGIN        = 15;
    public static final double DISTANCE      = 85;

    /**
     * Collapsed window size — just big enough to hold the sphere with its margin.
     * 86 = (28 + 15) * 2
     */
    public static final double COLLAPSED_SIZE = 2 * (SPHERE_RADIUS + MARGIN); // 86

    /**
     * Sphere centre inside the COLLAPSED window (bottom-right corner of the small pane).
     * 43 = 86 − 28 − 15
     */
    public static final double SPHERE_INITIAL_POS  = SPHERE_RADIUS + MARGIN;  // 43

    /**
     * Sphere centre inside the EXPANDED (full-size) panel.
     * 177 = 220 − 28 − 15
     */
    public static final double SPHERE_EXPANDED_POS = PaneHelper.PANEL_SIZE - SPHERE_RADIUS - MARGIN; // 177

    public Circle buildMainSphere() {
        // Start at the collapsed position; toggleMenu() will animate to SPHERE_EXPANDED_POS
        Circle c = new Circle(SPHERE_INITIAL_POS, SPHERE_INITIAL_POS, SPHERE_RADIUS);
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
        // Leaf positions are calculated relative to the EXPANDED sphere position.
        // Leaves are invisible (opacity 0, scale 0) while the menu is closed,
        // so it doesn't matter that they sit outside the collapsed stage window.
        return defs.stream().map(def -> {
            double rad = Math.toRadians(def.angle());
            double bx = SPHERE_EXPANDED_POS + Math.cos(rad) * DISTANCE;
            double by = SPHERE_EXPANDED_POS - Math.sin(rad) * DISTANCE;
            return new BubbleItem(bx, by, def.label(), def.color());
        }).toList();
    }
}
