package com.jirahourlogger.ui;

import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;


public class BubbleItem extends Circle {
    private static final double BUBBLE_RADIUS = 22;
    private final Text text;

    public BubbleItem(double cx, double cy, String label, String color) {
        this.setCenterX(cx);          // tell the circle where it is
        this.setCenterY(cy);          // ↑
        this.setRadius(BUBBLE_RADIUS); // give it a size

        this.setFill(Color.web(color));
        this.setOpacity(0);
        this.setScaleX(0);
        this.setScaleY(0);
        this.setEffect(new DropShadow(10, Color.web("#00000055")));
        this.setStyle("-fx-cursor: hand;");

        // Styling for the text w the bubble
        this.text = new Text(label);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("System", FontWeight.BOLD, 9));
        text.setOpacity(0);
        text.setX(cx - label.length() * 3.0);
        text.setY(cy + BUBBLE_RADIUS + 12);

        if (label.equals("Notes")) {
            this.setOnMouseClicked((e) -> {
                new NotesView().show();
            });
        }
    }

    // Add a getter so callers can add the text to the pane
    public Text getText() {
        return text;
    }

    public Circle getCircle() {
        return this;
    }
}
