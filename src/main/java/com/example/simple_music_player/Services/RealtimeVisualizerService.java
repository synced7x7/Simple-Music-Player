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
    private volatile float[] currentMagnitudes;
    private AnimationTimer animationTimer;
    private boolean isRunning = false;

    public boolean getIsRunning () {
        return isRunning;
    }

    @FXML
    public void initialize() {
        waveformCanvas.widthProperty().bind(waveformCanvasParent.widthProperty());
        waveformCanvas.heightProperty().bind(waveformCanvasParent.heightProperty());
        currentMagnitudes = new float[256]; //bands, tested manually

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
        int len = Math.min(magnitudes.length, currentMagnitudes.length);
        for (int i = 0; i < len; i++) {
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
        double bandWidth = width / bands;
        double halfBands = bands / 2.0;

        // Gradient colors
        Color startColor = Color.web("#8e2de2");
        Color endColor = Color.web("#ff4b2b");

        // Draw bars
        for (int i = 0; i < bands / 2; i++) {
            double magnitude = Math.max(-60, currentMagnitudes[i]);
            double barHeight = height * ((magnitude + 60) / 60.0);

            double t = i / (halfBands - 1);
            Color color = startColor.interpolate(endColor, t);
            gc.setFill(color);

            double xRight = centerX + ((i / halfBands) * (width / 2.0));
            double xLeft  = centerX - ((i / halfBands) * (width / 2.0)) - bandWidth;
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
        waveformCanvas.setVisible(false);
        waveformCanvas.setManaged(false);
        clearCanvas();
        currentMagnitudes = new float[256];
    }

    private void clearCanvas() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, waveformCanvas.getWidth(), waveformCanvas.getHeight());
    }

}
