package com.jirahourlogger;

/**
 * Launcher is the real entry point that Java/IntelliJ runs.
 *
 * WHY THIS EXISTS:
 * JavaFX has a quirk — if the class that contains `main()` also extends
 * `javafx.application.Application`, the JVM checks whether JavaFX was loaded
 * as a proper Java Module (via --module-path). When you run from IntelliJ with
 * Maven dependencies on the classpath, that check fails and you get:
 *   "JavaFX runtime components are missing"
 *
 * The fix is simple: keep main() in a plain class (no JavaFX imports).
 * By the time App.main() is called from here, JavaFX initialises fine.
 */
public class Launcher {

    public static void main(String[] args) {
        // Hand off to the real JavaFX application entry point
        App.main(args);
    }
}
