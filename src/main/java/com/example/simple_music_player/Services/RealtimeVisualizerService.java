package com.example.simple_music_player.Services;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import lombok.Setter;

public class RealtimeVisualizerService {

    @FXML
    private Canvas waveformCanvas;
    @FXML
    private AnchorPane waveformCanvasParent;

    // Smoothed magnitudes for nicer animation
    private volatile float[] currentMagnitudes;

    // For drawing at ~60 FPS
    private AnimationTimer animationTimer;
    @Setter
    private boolean isRunning = false;

    @FXML
    public void initialize() {
        // Canvas resizes dynamically with parent
        waveformCanvas.widthProperty().bind(waveformCanvasParent.widthProperty());
        waveformCanvas.heightProperty().bind(waveformCanvasParent.heightProperty());

        // Initialize array to avoid NPE
        currentMagnitudes = new float[128]; // default band count

    }

    public void startVisualizer() {
        if (animationTimer == null) {
            createAnimationTimer();
        }
        if (!isRunning) {
            animationTimer.start();
            isRunning = true;
        }
        waveformCanvas.setVisible(true);
        waveformCanvas.setManaged(true);
    }

    private void createAnimationTimer() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                drawSpectrum();
            }
        };
        animationTimer.start();
    }


    public void updateSpectrum(float[] magnitudes) {
        if (magnitudes == null) return;
        // Copy and smooth values
        int len = Math.min(magnitudes.length, currentMagnitudes.length);
        for (int i = 0; i < len; i++) {
            // Simple smoothing: gradual rise and decay
            currentMagnitudes[i] += (magnitudes[i] - currentMagnitudes[i]) * 0.3f;
        }
    }

    private void drawSpectrum() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        double width = waveformCanvas.getWidth();
        double height = waveformCanvas.getHeight();

        gc.clearRect(0, 0, width, height);

        if (currentMagnitudes == null) return;

        int bands = currentMagnitudes.length;
        double centerX = width / 2.0;
        double bandWidth = width / (bands * 2.0); // both sides total

        for (int i = 0; i < bands; i++) {
            double magnitude = Math.max(-60, currentMagnitudes[i]); // clamp
            double barHeight = (height / 2.0) * ((magnitude + 60) / 60.0); // map -60..0 → 0..half height

            double xRight = centerX + i * bandWidth;
            double xLeft = centerX - (i + 1) * bandWidth;

            // Choose color gradient
            Color color = Color.hsb(200 - (i * 180.0 / bands), 0.9, 1.0);

            gc.setFill(color);
            // Draw mirrored bars from center
            gc.fillRect(xRight, (height / 2.0) - barHeight, bandWidth - 1, barHeight * 2);
            gc.fillRect(xLeft, (height / 2.0) - barHeight, bandWidth - 1, barHeight * 2);
        }
    }

    public void stopVisualizer() {
        if(!isRunning) return;
        isRunning = false;
        if (animationTimer != null ) {
            animationTimer.stop();
        }

        // Hide canvas
        waveformCanvas.setVisible(false);
        waveformCanvas.setManaged(false);

        // Optionally clear visual memory
        clearCanvas();

        // Allow GC to reclaim unused memory
        currentMagnitudes = new float[128];
    }

    private void clearCanvas() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, waveformCanvas.getWidth(), waveformCanvas.getHeight());
    }

}
