package com.example.simple_music_player.Services;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class RealtimeVisualizerService {
    @FXML
    private Canvas waveformCanvas;
    @FXML
    private AnchorPane waveformCanvasParent;

    private static final int MAX_POINTS = 300;
    private final LinkedList<Float> amplitudes = new LinkedList<>();
    private volatile boolean decoding = false;
    private AnimationTimer timer;

    @FXML
    public void initialize() {
        // Bind canvas to parent size and listen for resize
        waveformCanvas.widthProperty().bind(waveformCanvasParent.widthProperty());
        waveformCanvas.heightProperty().bind(waveformCanvasParent.heightProperty());
        waveformCanvas.widthProperty().addListener((obs, o, n) -> drawWaveform());
        waveformCanvas.heightProperty().addListener((obs, o, n) -> drawWaveform());
    }

    /** Start decoding and drawing the real-time waveform for the given audio file. */
    public void startRealtimeWaveform(File audioFile) {
        if (audioFile == null || !audioFile.exists()) return;
        // Stop any previous decoding
        decoding = false;
        // Start the decoding thread
        new Thread(() -> decodeAudioStream(audioFile)).start();
        // Start the animation timer to redraw the Canvas continuously
        timer = new AnimationTimer() {
            @Override public void handle(long now) { drawWaveform(); }
        };
        timer.start();
    }

    /** Decode the audio file in real time, extract amplitudes. */
    private void decodeAudioStream(File audioFile) {
        decoding = true;
        try (InputStream in = new BufferedInputStream(new FileInputStream(audioFile))) {
            Bitstream bitstream = new Bitstream(in);
            Decoder decoder = new Decoder();
            while (decoding) {
                Header frameHeader = bitstream.readFrame();
                if (frameHeader == null) break;  // end of stream
                // Decode MP3 frame to PCM
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                short[] buffer = output.getBuffer(); 
                // Compute max absolute sample as amplitude (normalized 0..1)
                float maxAmp = 0;
                for (short s : buffer) {
                    float amp = Math.abs(s) / 32768.0f;
                    if (amp > maxAmp) maxAmp = amp;
                }
                // Append to sliding window (thread-safe)
                synchronized(amplitudes) {
                    amplitudes.add(maxAmp);
                    if (amplitudes.size() > MAX_POINTS) {
                        amplitudes.removeFirst();
                    }
                }
                bitstream.closeFrame();
                // Sleep for the frame duration to simulate real-time pacing
                Thread.sleep((int) frameHeader.ms_per_frame());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            decoding = false;
        }
    }

    /** Draw the waveform on the Canvas based on buffered amplitudes. */
    private void drawWaveform() {
        GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
        double width = waveformCanvas.getWidth();
        double height = waveformCanvas.getHeight();
        gc.clearRect(0, 0, width, height);
        gc.setFill(Color.LIGHTBLUE);

        // Make a snapshot of data to avoid concurrent modification
        List<Float> snapshot;
        synchronized(amplitudes) {
            snapshot = new ArrayList<>(amplitudes);
        }
        if (snapshot.isEmpty()) return;

        double barWidth = width / snapshot.size();
        for (int i = 0; i < snapshot.size(); i++) {
            double v = snapshot.get(i);
            double barH = v * height * 0.7;  // scale factor for visibility
            // Draw centered vertical bar for this amplitude
            gc.fillRect(i * barWidth, (height - barH) / 2, barWidth, barH);
        }
    }

    /** Stop decoding and clear the waveform. */
    public void stopRealtimeWaveform() {
        decoding = false;
        if (timer != null) timer.stop();
        synchronized(amplitudes) { amplitudes.clear(); }
        // Clear canvas in JavaFX thread
        Platform.runLater(() -> {
            waveformCanvas.getGraphicsContext2D().clearRect(0, 0,
                waveformCanvas.getWidth(), waveformCanvas.getHeight());
        });
    }
}
