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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

                switch (ext) {
                    case "mp3" -> loadMP3Waveform(audioFile);
                    case "wav" -> loadWAVWaveform(audioFile);
                    case "aac", "m4a" -> loadm4aWaveform(audioFile);
                    default -> System.out.println("Unsupported audio format: " + ext);
                }

                Platform.runLater(this::drawWaveform);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadM4AWaveform(File m4aFile) {
        try (InputStream fis = new FileInputStream(m4aFile)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();

            int targetSize = 300;
            List<Float> allMaxValues = new ArrayList<>();

            org.jaad.aac.decoder.Header frameHeader;
            int frameCount = 0;
            int frameSkip = 2; // Process every 2nd frame only

            // Collect max values from frames
            while ((frameHeader = bitstream.readFrame()) != null) {
                frameCount++;

                // Skip frames for speed
                if (frameCount % frameSkip != 0) {
                    bitstream.closeFrame();
                    continue;
                }

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                short[] buffer = output.getBuffer();

                // Find max in this frame
                float maxInFrame = 0;
                for (short s : buffer) {
                    float normalized = Math.abs(s) / 32768.0f;
                    maxInFrame = Math.max(maxInFrame, normalized);
                }

                allMaxValues.add(maxInFrame);
                bitstream.closeFrame();
            }

            // Now downsample the collected values to targetSize
            waveform = new float[targetSize];
            int collectedSize = allMaxValues.size();

            if (collectedSize == 0) {
                // No data collected
                Arrays.fill(waveform, 0);
            } else if (collectedSize <= targetSize) {
                // We have less data than target, stretch it
                for (int i = 0; i < targetSize; i++) {
                    int srcIndex = (int) ((double) i / targetSize * collectedSize);
                    srcIndex = Math.min(srcIndex, collectedSize - 1);
                    waveform[i] = allMaxValues.get(srcIndex);
                }
            } else {
                // We have more data, downsample by taking max of groups
                float step = (float) collectedSize / targetSize;
                for (int i = 0; i < targetSize; i++) {
                    int startIdx = (int) (i * step);
                    int endIdx = (int) ((i + 1) * step);
                    endIdx = Math.min(endIdx, collectedSize);

                    // Take max of this range
                    float maxInRange = 0;
                    for (int j = startIdx; j < endIdx; j++) {
                        maxInRange = Math.max(maxInRange, allMaxValues.get(j));
                    }
                    waveform[i] = maxInRange;
                }
            }

            bitstream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void loadMP3Waveform(File mp3File) {
        try (InputStream fis = new FileInputStream(mp3File)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();

            int targetSize = 300;
            List<Float> allMaxValues = new ArrayList<>();

            javazoom.jl.decoder.Header frameHeader;
            int frameCount = 0;
            int frameSkip = 2; // Process every 2nd frame only

            // Collect max values from frames
            while ((frameHeader = bitstream.readFrame()) != null) {
                frameCount++;

                // Skip frames for speed
                if (frameCount % frameSkip != 0) {
                    bitstream.closeFrame();
                    continue;
                }

                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                short[] buffer = output.getBuffer();

                // Find max in this frame
                float maxInFrame = 0;
                for (short s : buffer) {
                    float normalized = Math.abs(s) / 32768.0f;
                    maxInFrame = Math.max(maxInFrame, normalized);
                }

                allMaxValues.add(maxInFrame);
                bitstream.closeFrame();
            }

            // Now downsample the collected values to targetSize
            waveform = new float[targetSize];
            int collectedSize = allMaxValues.size();

            if (collectedSize == 0) {
                // No data collected
                Arrays.fill(waveform, 0);
            } else if (collectedSize <= targetSize) {
                // We have less data than target, stretch it
                for (int i = 0; i < targetSize; i++) {
                    int srcIndex = (int) ((double) i / targetSize * collectedSize);
                    srcIndex = Math.min(srcIndex, collectedSize - 1);
                    waveform[i] = allMaxValues.get(srcIndex);
                }
            } else {
                // We have more data, downsample by taking max of groups
                float step = (float) collectedSize / targetSize;
                for (int i = 0; i < targetSize; i++) {
                    int startIdx = (int) (i * step);
                    int endIdx = (int) ((i + 1) * step);
                    endIdx = Math.min(endIdx, collectedSize);

                    // Take max of this range
                    float maxInRange = 0;
                    for (int j = startIdx; j < endIdx; j++) {
                        maxInRange = Math.max(maxInRange, allMaxValues.get(j));
                    }
                    waveform[i] = maxInRange;
                }
            }

            bitstream.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWAVWaveform(File wavFile) {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(wavFile)) {
            AudioFormat format = audioStream.getFormat();

            int bytesPerFrame = format.getFrameSize();
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                bytesPerFrame = 2; // Assume 16-bit mono
            }

            long frameLength = audioStream.getFrameLength();
            int targetSize = 300;

            // Skip frames for faster processing
            int frameSkip = 2; // Process every 2nd frame
            List<Float> allMaxValues = new ArrayList<>();

            // Calculate chunk size for reading
            int framesPerChunk = 4096; // Read in chunks
            byte[] buffer = new byte[bytesPerFrame * framesPerChunk];

            long framesProcessed = 0;

            while (framesProcessed < frameLength) {
                int bytesRead = audioStream.read(buffer);
                if (bytesRead <= 0) break;

                int samplesRead = bytesRead / bytesPerFrame;

                // Process samples with skipping
                float maxInChunk = 0;
                for (int i = 0; i < bytesRead - 1; i += bytesPerFrame * frameSkip) {
                    short sample;

                    // Handle different bit depths
                    if (bytesPerFrame >= 2) {
                        sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xff));
                    } else {
                        sample = buffer[i];
                    }

                    float normalized = Math.abs(sample) / 32768.0f;
                    maxInChunk = Math.max(maxInChunk, normalized);
                }

                if (maxInChunk > 0) {
                    allMaxValues.add(maxInChunk);
                }

                framesProcessed += samplesRead;
            }

            // Downsample to target size
            waveform = new float[targetSize];
            int collectedSize = allMaxValues.size();

            if (collectedSize == 0) {
                Arrays.fill(waveform, 0);
            } else if (collectedSize <= targetSize) {
                // Stretch to target size
                for (int i = 0; i < targetSize; i++) {
                    int srcIndex = (int) ((double) i / targetSize * collectedSize);
                    srcIndex = Math.min(srcIndex, collectedSize - 1);
                    waveform[i] = allMaxValues.get(srcIndex);
                }
            } else {
                // Downsample by taking max of groups
                float step = (float) collectedSize / targetSize;
                for (int i = 0; i < targetSize; i++) {
                    int startIdx = (int) (i * step);
                    int endIdx = (int) ((i + 1) * step);
                    endIdx = Math.min(endIdx, collectedSize);

                    float maxInRange = 0;
                    for (int j = startIdx; j < endIdx; j++) {
                        maxInRange = Math.max(maxInRange, allMaxValues.get(j));
                    }
                    waveform[i] = maxInRange;
                }
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
                double barHeight = value * canvasHeight * 0.7;
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