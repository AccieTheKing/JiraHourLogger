package com.jirahourlogger.helper;

import com.jirahourlogger.ui.BubbleItem;
import javafx.animation.*;
import javafx.util.Duration;

/**
 * AnimationHelper handles the show/hide animations for the leaf bubble items
 * that radiate from the main floating sphere when the menu opens.
 *
 * Each bubble has two visual parts that animate independently:
 *   - Circle  — scales in/out (grows from nothing, or shrinks away)
 *   - Label   — fades in/out (text appears slightly after the circle)
 *
 * KEY JavaFX ANIMATION CLASSES:
 *
 * ScaleTransition    — smoothly changes a node's scale (size) over time.
 *                      scaleX=0 means invisible; scaleX=1 means normal size.
 *
 * FadeTransition     — smoothly changes a node's opacity (0 = invisible, 1 = fully visible).
 *
 * SequentialTransition — plays a list of animations one after the other.
 *
 * ParallelTransition — plays a list of animations at the same time.
 *
 * Duration.millis()  — specifies how long an animation should take, in milliseconds.
 *
 * setDelay()         — waits before starting. Used here so each bubble pops in
 *                      with a stagger effect rather than all at once.
 *
 * Interpolator.EASE_OUT — starts fast, slows toward the end (feels natural, like settling).
 * Interpolator.EASE_IN  — starts slow, speeds up toward the end (feels like being pulled in).
 */
public class AnimationHelper {

    /**
     * Animates a single bubble either into view (show=true) or out of view (show=false).
     *
     * @param bubble   the BubbleItem (circle + label) to animate
     * @param show     true = animate IN (menu opening), false = animate OUT (menu closing)
     * @param delayMs  how many milliseconds to wait before starting this bubble's animation.
     *                 Each bubble gets a slightly larger delay so they pop in one at a time.
     */
    public void animateBubble(BubbleItem bubble, boolean show, int delayMs) {
        Duration delay = Duration.millis(delayMs);

        if (show) {
            // ── Showing the bubble ──────────────────────────────────────────
            // Step 1: scale from 0 up to 1.2 (a little bigger than normal — the "overshoot")
            ScaleTransition overshoot = new ScaleTransition(Duration.millis(200), bubble.getCircle());
            overshoot.setFromX(0);
            overshoot.setFromY(0);
            overshoot.setToX(1.2);  // 1.2 = 120% of normal size
            overshoot.setToY(1.2);
            overshoot.setInterpolator(Interpolator.EASE_OUT); // decelerates at the end

            // Step 2: scale back down to 1.0 (normal size) — the "settle"
            // Together these two steps create a satisfying springy "pop" effect
            ScaleTransition settle = new ScaleTransition(Duration.millis(100), bubble.getCircle());
            settle.setToX(1.0);
            settle.setToY(1.0);
            settle.setInterpolator(Interpolator.EASE_IN);

            // Run overshoot, then settle, one after the other
            SequentialTransition bounce = new SequentialTransition(overshoot, settle);
            bounce.setDelay(delay);
            bounce.play();

        } else {
            // ── Hiding the bubble ───────────────────────────────────────────
            // Simple scale-down from current size to 0 (disappear)
            ScaleTransition scale = new ScaleTransition(Duration.millis(180), bubble.getCircle());
            scale.setDelay(delay);
            scale.setToX(0);
            scale.setToY(0);
            scale.setInterpolator(Interpolator.EASE_IN); // accelerates as it shrinks (feels snappy)
            scale.play();
        }

        // ── Fade the circle ─────────────────────────────────────────────────
        // Show: fade opacity from current value to 1 (fully visible)
        // Hide: fade opacity from current value to 0 (invisible)
        FadeTransition fadeCircle = new FadeTransition(Duration.millis(200), bubble.getCircle());
        fadeCircle.setDelay(delay);
        fadeCircle.setToValue(show ? 1 : 0);

        // ── Fade the label (text under the circle) ──────────────────────────
        // When showing: the label starts fading in 100 ms after the circle so
        // the circle appears first, giving the label a slight "trailing" entrance.
        // When hiding: the label fades out at the same time as the circle.
        FadeTransition fadeLabel = new FadeTransition(Duration.millis(200), bubble.getText());
        fadeLabel.setDelay(show ? delay.add(Duration.millis(100)) : delay);
        fadeLabel.setToValue(show ? 1 : 0);

        fadeCircle.play();
        fadeLabel.play();
    }
}
