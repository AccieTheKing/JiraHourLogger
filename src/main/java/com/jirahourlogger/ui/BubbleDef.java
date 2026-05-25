package com.jirahourlogger.ui;

/**
 * BubbleDef is a simple data holder that describes one leaf bubble in the menu.
 *
 * A Java `record` is a shorthand for a class that only holds data. The compiler
 * automatically generates the constructor, getters (label(), angle(), color()),
 * equals(), hashCode(), and toString() for you — no boilerplate needed.
 *
 * Fields:
 *   label  — the text shown under the bubble (e.g. "Notes")
 *   angle  — the direction (in degrees) from the main sphere where this bubble is placed.
 *             0° = right, 90° = up, 135° = upper-left, 180° = left.
 *   color  — the fill color of the bubble circle as a hex string (e.g. "#FF6B6B")
 *
 * Example:
 *   new BubbleDef("Notes", 135.0, "#FF6B6B")
 *   → a red bubble placed at 135° (upper-left) from the main sphere, labelled "Notes"
 */
public record BubbleDef(String label, double angle, String color) {}
