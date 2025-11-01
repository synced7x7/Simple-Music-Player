package com.example.simple_music_player.Utility;

import javafx.scene.Node;
import javafx.stage.Stage;

public class WindowUtils {

    private static class Delta {
        double x, y;
    }

    public static void makeDraggable(Stage stage, Node dragArea) {
        Delta delta = new Delta();

        dragArea.setOnMousePressed(e -> {
            delta.x = e.getSceneX();
            delta.y = e.getSceneY();
        });

        dragArea.setOnMouseDragged(e -> {
            stage.setX(e.getScreenX() - delta.x);
            stage.setY(e.getScreenY() - delta.y);
        });
    }
}
