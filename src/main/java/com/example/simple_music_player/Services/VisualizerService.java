package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.NowPlayingController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;

import javax.sound.sampled.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.SampleBuffer;

public class VisualizerService {

    @FXML
    private Canvas waveformCanvas;

    private float[] waveform; // Use float instead of double (half the memory)
    @FXML
    private AnchorPane waveformCanvasParent;

    public static Boolean progressBarDraggingCap = false;

    private WritableImage waveformImage;

    @FXML
    public void initialize() {
        // canvas track parent size
        waveformCanvas.widthProperty().bind(waveformCanvasParent.widthProperty());
        waveformCanvas.heightProperty().bind(waveformCanvasParent.heightProperty());

        waveformCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawWaveform());
        waveformCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawWaveform());

        waveformCanvas.setOnMousePressed(event -> {
            progressBarDraggingCap = true;
            updateProgressFromMouse(event.getX(), false);
        });

        waveformCanvas.setOnMouseDragged(event -> {
            progressBarDraggingCap = true;
            updateProgressFromMouse(event.getX(), false);
        });

        waveformCanvas.setOnMouseReleased(event -> {
            progressBarDraggingCap = false;
            updateProgressFromMouse(event.getX(), true);
        });
    }

    private void updateProgressFromMouse(double mouseX, boolean seek) {
        double canvasWidth = waveformCanvas.getWidth();
        double currentProgress = Math.max(0, Math.min(mouseX / canvasWidth, 1.0));

        updateProgress(currentProgress);

        if (seek) {
            PlaybackService service = NowPlayingController.getPlaybackService();
            service.updateProgressFromMouse(currentProgress);
        }
    }

    public void loadWaveform(File audioFile) {
        if (!audioFile.exists()) {
            System.out.println("File not found: " + audioFile.getAbsolutePath());
            return;
        }

        String ext = audioFile.getName().substring(audioFile.getName().lastIndexOf('.') + 1).toLowerCase();

        new Thread(() -> {
            try {
                // Clear old waveform to free memory
                waveform = null;
                System.gc(); // Suggest garbage collection

                if (ext.equals("mp3")) {
                    loadMP3Waveform(audioFile);
                } else if (ext.equals("wav")) {
                    loadWAVWaveform(audioFile);
                } else {
                    System.out.println("Unsupported audio format: " + ext);
                }

                Platform.runLater(this::drawWaveform);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadMP3Waveform(File mp3File) {
        try (InputStream fis = new FileInputStream(mp3File)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();

            int targetSize = 500;
            waveform = new float[targetSize];

            // Pre-allocate with fixed size instead of dynamic ArrayList
            int samplesPerBar = 0;
            int barIndex = 0;
            float maxInBar = 0;

            javazoom.jl.decoder.Header frameHeader;
            long totalSamples = 0;

            // First pass: count samples to calculate step size
            while ((frameHeader = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                totalSamples += output.getBufferLength();
                bitstream.closeFrame();
            }

            samplesPerBar = (int) (totalSamples / targetSize);
            if (samplesPerBar == 0) samplesPerBar = 1;

            // Close and reopen stream for second pass
            bitstream.close();
            fis.close();

            // Second pass: downsample on the fly
            try (InputStream fis2 = new FileInputStream(mp3File)) {
                bitstream = new Bitstream(fis2);
                decoder = new Decoder();

                int sampleCount = 0;

                while ((frameHeader = bitstream.readFrame()) != null && barIndex < targetSize) {
                    SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                    short[] buffer = output.getBuffer();

                    for (short s : buffer) {
                        float normalized = Math.abs(s) / 32768.0f;
                        maxInBar = Math.max(maxInBar, normalized);
                        sampleCount++;

                        if (sampleCount >= samplesPerBar) {
                            if (waveform != null)
                                waveform[barIndex++] = maxInBar;
                            maxInBar = 0;
                            sampleCount = 0;

                            if (barIndex >= targetSize) break;
                        }
                    }
                    bitstream.closeFrame();
                }

                // Fill remaining if needed
                while (barIndex < targetSize) {
                    waveform[barIndex++] = maxInBar;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWAVWaveform(File wavFile) {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat format = audioStream.getFormat();

            int bytesPerFrame = format.getFrameSize();
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                bytesPerFrame = 1;
            }

            long frameLength = audioStream.getFrameLength();
            int targetSize = 500;
            waveform = new float[targetSize];

            int samplesPerBar = (int) (frameLength / targetSize);
            if (samplesPerBar == 0) samplesPerBar = 1;

            byte[] buffer = new byte[bytesPerFrame * samplesPerBar];
            int barIndex = 0;

            while (barIndex < targetSize) {
                int bytesRead = audioStream.read(buffer);
                if (bytesRead <= 0) break;

                float maxInBar = 0;
                for (int i = 0; i + 1 < bytesRead; i += 2) {
                    short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
                    float normalized = Math.abs(sample) / 32768.0f;
                    maxInBar = Math.max(maxInBar, normalized);
                }

                waveform[barIndex++] = maxInBar;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawWaveform() {
        if (waveform == null || waveform.length == 0) return;

        Platform.runLater(() -> {
            double canvasWidth = waveformCanvas.getWidth();
            double canvasHeight = waveformCanvas.getHeight();
            int length = waveform.length;
            double barWidth = canvasWidth / length;

            // Create canvas for off-screen rendering
            Canvas offscreenCanvas = new Canvas(canvasWidth, canvasHeight);
            GraphicsContext imgGc = offscreenCanvas.getGraphicsContext2D();

            imgGc.setFill(Color.LIGHTBLUE);

            for (int i = 0; i < length; i++) {
                double value = waveform[i];
                double barHeight = value * canvasHeight * 0.8;
                imgGc.fillRect(i * barWidth, (canvasHeight - barHeight) / 2, barWidth, barHeight);
            }

            // Cache the waveform as image
            waveformImage = offscreenCanvas.snapshot(null, null);

            // Draw initial view
            GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, canvasWidth, canvasHeight);
            gc.drawImage(waveformImage, 0, 0, canvasWidth, canvasHeight);
        });
    }

    public void updateProgress(double progress) {
        if (waveformImage == null) return;

        Platform.runLater(() -> {
            GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
            double canvasWidth = waveformCanvas.getWidth();
            double canvasHeight = waveformCanvas.getHeight();

            // Draw the cached waveform image
            gc.clearRect(0, 0, canvasWidth, canvasHeight);
            gc.drawImage(waveformImage, 0, 0, canvasWidth, canvasHeight);

            // Draw progress overlay (dark blue rectangle)
            gc.setGlobalAlpha(0.4); // optional transparency
            gc.setFill(Color.DARKBLUE);
            gc.fillRect(0, 0, canvasWidth * progress, canvasHeight);
            gc.setGlobalAlpha(1.0);
        });
    }

    // Call this when changing songs or closing the visualizer
    public void cleanup() {
        waveformImage = null;
        waveform = null;
        if (waveformCanvas != null) {
            GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, waveformCanvas.getWidth(), waveformCanvas.getHeight());
        }
    }
}