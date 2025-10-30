package com.example.simple_music_player.Services;

import javafx.animation.AnimationTimer;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

public class RealtimeVisualizerService {

    @FXML
    private Canvas waveformCanvas;
    @FXML
    private AnchorPane waveformCanvasParent;

    // Smoothed magnitudes for nicer animation
    private volatile float[] currentMagnitudes;

    // For drawing at ~60 FPS
    private AnimationTimer animationTimer;
    private boolean isRunning = false;

    public boolean getIsRunning () {
        return isRunning;
    }

    @FXML
    public void initialize() {
        // Canvas resizes dynamically with parent
        waveformCanvas.widthProperty().bind(waveformCanvasParent.widthProperty());
        waveformCanvas.heightProperty().bind(waveformCanvasParent.heightProperty());
        currentMagnitudes = new float[256]; // default band count

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

        // ✅ Full width coverage
        double centerX = width / 2.0;
        double bandWidth = width / (bands); // half for each side total

        // ✅ Scaling factor so bars fill from center to border
        double halfBands = bands / 2.0;

        for (int i = 0; i < bands / 2; i++) {
            double magnitude = Math.max(-60, currentMagnitudes[i]);
            // map -60..0 → 0..height/2 (vertical full height)
            double barHeight = height * ((magnitude + 60) / 60.0);

            // Positions for both sides
            double xRight = centerX + ((i / halfBands) * (width / 2.0));
            double xLeft  = centerX - ((i / halfBands) * (width / 2.0)) - bandWidth;

            // ✅ Smooth color gradient (blue → cyan → green)
            Color color = Color.hsb(200 - (i * 180.0 / (bands / 2.0)), 0.9, 1.0);
            gc.setFill(color);

            // ✅ Draw bars vertically centered, touching top/bottom
            double topY = (height - barHeight) / 2.0;
            gc.fillRect(xRight, topY, bandWidth - 1, barHeight);
            gc.fillRect(xLeft, topY, bandWidth - 1, barHeight);
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
        currentMagnitudes = new float[256];
    }

    private void clearCanvas() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, waveformCanvas.getWidth(), waveformCanvas.getHeight());
    }

}
