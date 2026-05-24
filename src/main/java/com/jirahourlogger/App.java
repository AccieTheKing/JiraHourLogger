package com.jirahourlogger;

import com.jirahourlogger.ui.MainFloatingSphere;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * App is the JavaFX application class.
 *
 * In JavaFX every application must have exactly one class that extends
 * `Application`. Think of Application as the "runtime host" — it boots
 * the JavaFX engine, sets up the UI thread, and calls start() when ready.
 *
 * The lifecycle is:
 *   1. Launcher.main()  →  calls App.main()
 *   2. App.main()       →  calls launch(), which boots the JavaFX engine
 *   3. JavaFX engine    →  calls start() on the UI thread
 *   4. start()          →  creates our FloatingSphere and shows it
 */
public class App extends Application {

    /**
     * start() is called by the JavaFX engine after it has finished booting.
     * `primaryStage` is a default window JavaFX creates for you automatically.
     * We don't need it (our UI lives in the FloatingSphere's own window),
     * so we just hide it.
     *
     * A Stage in JavaFX = an OS window.
     * A Scene in JavaFX = the content inside that window.
     */
    @Override
    public void start(Stage primaryStage) {
        // Hide the default blank window JavaFX creates on startup
        primaryStage.hide();

        // Create and show our custom floating sphere on the desktop
        new MainFloatingSphere().show();
    }

    /**
     * main() is required by Java as the program entry point.
     * `launch(args)` is a static method inherited from Application —
     * it starts the JavaFX engine and eventually calls start() above.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
