package com.example.simple_music_player.Utility;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
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

    public static void scaleUp(Node node, double durationSeconds, double targetScale) {
        ScaleTransition scaleUp = new ScaleTransition(Duration.seconds(durationSeconds), node);
        scaleUp.setToX(targetScale);
        scaleUp.setToY(targetScale);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);
        scaleUp.play();
    }

    public static void applyGlow(Node node, String hexColor) {
        DropShadow glow = new DropShadow();
        glow.setColor(Color.web(hexColor));
        glow.setRadius(25);
        glow.setSpread(0.7);
        node.setEffect(glow);

        // Breathing pulse animation
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(glow.radiusProperty(), 15),
                        new KeyValue(glow.spreadProperty(), 0.6),
                        new KeyValue(glow.colorProperty(), Color.web(hexColor))),
                new KeyFrame(Duration.seconds(1.2),
                        new KeyValue(glow.radiusProperty(), 30),
                        new KeyValue(glow.spreadProperty(), 0.9))
        );

        pulse.setAutoReverse(true);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.play();
    }

    public static void removeGlow(Node node) {
        node.setEffect(null);
    }

    /**
     * Smoothly scrolls a ScrollPane to a target Vvalue
     */
    public static void smoothScrollTo(ScrollPane scrollPane, double targetVValue, double durationSeconds) {
        double currentV = scrollPane.getVvalue();
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(scrollPane.vvalueProperty(), currentV)
                ),
                new KeyFrame(Duration.seconds(durationSeconds),
                        new KeyValue(scrollPane.vvalueProperty(), targetVValue, Interpolator.EASE_OUT)
                )
        );
        timeline.play();
    }

    /**
     * Smooth fade in/out for Text hover effect
     */
    public static void applyHoverEffect(Text text, Color normalColor, Color hoverColor, double durationSeconds) {
        text.setOnMouseEntered(e -> {
            FillTransition ft = new FillTransition(Duration.seconds(durationSeconds), text, (Color) text.getFill(), hoverColor);
            ft.setInterpolator(Interpolator.EASE_BOTH);
            ft.play();
        });

        text.setOnMouseExited(e -> {
            FillTransition ft = new FillTransition(Duration.seconds(durationSeconds), text, (Color) text.getFill(), normalColor);
            ft.setInterpolator(Interpolator.EASE_BOTH);
            ft.play();
        });
    }

    /**
     * Smooth scale animation (like press effect)
     */
    public static void smoothScale(Node node, double scaleTo, double durationSeconds) {
        ScaleTransition st = new ScaleTransition(Duration.seconds(durationSeconds), node);
        st.setToX(scaleTo);
        st.setToY(scaleTo);
        st.setInterpolator(Interpolator.EASE_BOTH);
        st.play();
    }

    public static void animateSyncedLyricTransition(Label label) {
        FadeTransition fade = new FadeTransition(Duration.millis(400), label);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.setInterpolator(Interpolator.EASE_OUT);

        ScaleTransition scale = new ScaleTransition(Duration.millis(400), label);
        scale.setFromX(0.9);
        scale.setFromY(0.9);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition combo = new ParallelTransition(fade, scale);
        combo.play();
    }

}
