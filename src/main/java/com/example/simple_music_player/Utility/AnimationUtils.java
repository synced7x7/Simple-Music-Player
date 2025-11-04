package com.example.simple_music_player.Utility;

import javafx.animation.*;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class AnimationUtils {
    public static void fadeIn(Node node, double durationSeconds) {
        node.setOpacity(0);
        node.setVisible(true);
        FadeTransition fadeIn = new FadeTransition(Duration.seconds(durationSeconds), node);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    public static void slideInFromRight(Node node, double distance, double durationSeconds) {
        node.setTranslateX(distance);
        node.setOpacity(0);

        TranslateTransition slide = new TranslateTransition(Duration.seconds(durationSeconds), node);
        slide.setFromX(distance);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.EASE_BOTH);

        FadeTransition fade = new FadeTransition(Duration.seconds(durationSeconds * 0.8), node);
        fade.setFromValue(0);
        fade.setToValue(1);

        // Combine for smoother feel
        ParallelTransition smoothSlide = new ParallelTransition(slide, fade);
        smoothSlide.play();
    }

    public static void whitePulse(Node node) {
        if (!(node instanceof Region region)) return;

        // Create an overlay Rectangle (acts as the white pulse layer)
        Rectangle overlay = new Rectangle();
        overlay.setManaged(false);
        overlay.setMouseTransparent(true);
        overlay.setFill(Color.rgb(255, 255, 255, 0.5)); // soft white
        overlay.setOpacity(0);

        // Match overlay size to region
        overlay.widthProperty().bind(region.widthProperty());
        overlay.heightProperty().bind(region.heightProperty());

        // Add overlay to region’s parent (if it’s a Pane)
        if (region.getParent() instanceof Pane parent) {
            parent.getChildren().add(overlay);
            overlay.layoutXProperty().bind(region.layoutXProperty());
            overlay.layoutYProperty().bind(region.layoutYProperty());
        } else {
            return; // can't overlay, skip
        }

        // Animate opacity pulse
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(overlay.opacityProperty(), 0)),
                new KeyFrame(Duration.seconds(0.12), new KeyValue(overlay.opacityProperty(), 1, Interpolator.EASE_OUT)),
                new KeyFrame(Duration.seconds(0.35), new KeyValue(overlay.opacityProperty(), 0, Interpolator.EASE_IN))
        );

        pulse.setCycleCount(1);
        pulse.setOnFinished(e -> ((Pane) region.getParent()).getChildren().remove(overlay));
        pulse.play();
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

    public static void smoothScrollTo(ScrollBar scrollBar, double targetValue, double durationSeconds) {
        double currentV = scrollBar.getValue();
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.seconds(0),
                        new KeyValue(scrollBar.valueProperty(), currentV)
                ),
                new KeyFrame(Duration.seconds(durationSeconds),
                        new KeyValue(scrollBar.valueProperty(), targetValue, Interpolator.EASE_OUT)
                )
        );
        timeline.play();
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
