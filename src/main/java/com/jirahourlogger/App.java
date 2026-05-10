package com.jirahourlogger;

import com.jirahourlogger.ui.FloatingSphere;
import javafx.application.Application;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.hide();
        new FloatingSphere().show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
