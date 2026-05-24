package com.jirahourlogger.helper;

import com.jirahourlogger.ui.BubbleItem;
import javafx.animation.*;
import javafx.util.Duration;

public class AnimationHelper {

    public void animateBubble(BubbleItem bubble, boolean show, int delayMs) {
        Duration delay = Duration.millis(delayMs);

        if (show) {
            ScaleTransition overshoot = new ScaleTransition(Duration.millis(200), bubble.getCircle());
            overshoot.setFromX(0);
            overshoot.setFromY(0);
            overshoot.setToX(1.2);
            overshoot.setToY(1.2);
            overshoot.setInterpolator(Interpolator.EASE_OUT);

            ScaleTransition settle = new ScaleTransition(Duration.millis(100), bubble.getCircle());
            settle.setToX(1.0);
            settle.setToY(1.0);
            settle.setInterpolator(Interpolator.EASE_IN);

            SequentialTransition bounce = new SequentialTransition(overshoot, settle);
            bounce.setDelay(delay);
            bounce.play();
        } else {
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), bubble.getCircle());
            scale.setDelay(delay);
            scale.setToX(0);
            scale.setToY(0);
            scale.setInterpolator(Interpolator.EASE_IN);
            scale.play();
        }

        FadeTransition fadeCircle = new FadeTransition(Duration.millis(200), bubble.getCircle());
        fadeCircle.setDelay(delay);
        fadeCircle.setToValue(show ? 1 : 0);

        // Label fades in slightly after the circle so the circle appears first
        FadeTransition fadeLabel = new FadeTransition(Duration.millis(200), bubble.getText());
        fadeLabel.setDelay(show ? delay.add(Duration.millis(100)) : delay);
        fadeLabel.setToValue(show ? 1 : 0);

        fadeCircle.play();
        fadeLabel.play();
    }
}
