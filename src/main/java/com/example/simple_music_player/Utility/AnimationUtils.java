package com.example.simple_music_player.Utility;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

public class AnimationUtils {

    // Fade In
    public static void fadeIn(Node node, double durationSeconds) {
        node.setOpacity(0);
        node.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(durationSeconds), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    // Fade Out
    public static void fadeOut(Node node, double durationSeconds) {
        FadeTransition fadeOut = new FadeTransition(Duration.seconds(durationSeconds), node);
        fadeOut.setFromValue(node.getOpacity());
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> node.setVisible(false));
        fadeOut.play();
    }

    // Slide from Left
    public static void slideInFromLeft(Node node, double distance, double durationSeconds) {
        TranslateTransition slide = new TranslateTransition(Duration.seconds(durationSeconds), node);
        node.setTranslateX(-distance);
        slide.setFromX(-distance);
        slide.setToX(0);
        slide.play();
    }

    // Slide from Right
    public static void slideInFromRight(Node node, double distance, double durationSeconds) {
        TranslateTransition slide = new TranslateTransition(Duration.seconds(durationSeconds), node);
        node.setTranslateX(distance);
        slide.setFromX(distance);
        slide.setToX(0);
        slide.play();
    }

    // Pulse effect (for buttons or play/pause icons)
    public static void pulse(Node node, double durationSeconds) {
        ScaleTransition pulse = new ScaleTransition(Duration.seconds(durationSeconds), node);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(2);
        pulse.setAutoReverse(true);
        pulse.play();
    }

    public static void rotate(Node node, double angle, double durationSeconds) {
        RotateTransition rotate = new RotateTransition(Duration.seconds(durationSeconds), node);
        rotate.setByAngle(angle);
        rotate.setCycleCount(1);
        rotate.play();
    }

    public static void smoothPress(Node node, double scaleDownTo, double durationSeconds) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.seconds(durationSeconds / 2), node);
        scaleDown.setToX(scaleDownTo);
        scaleDown.setToY(scaleDownTo);
        scaleDown.setInterpolator(Interpolator.EASE_IN);

        ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(durationSeconds / 2), node);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        SequentialTransition press = new SequentialTransition(scaleDown, scaleUp);
        press.play();
    }

    public static void scaleDown(Node node, double targetScale, double durationSeconds) {
        ScaleTransition scaleDown = new ScaleTransition(Duration.seconds(durationSeconds), node);
        scaleDown.setToX(targetScale);
        scaleDown.setToY(targetScale);
        scaleDown.setInterpolator(Interpolator.EASE_IN);
        scaleDown.play();
    }

    public static void scaleUp(Node node, double durationSeconds) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(durationSeconds), node);
        scaleUp.setToX(1.0);
        scaleUp.setToY(1.0);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);
        scaleUp.play();
    }

}
