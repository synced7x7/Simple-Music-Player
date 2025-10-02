package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.NowPlayingController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
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

    private double[] waveform; // normalized waveform array
    @FXML
    private AnchorPane waveformCanvasParent;

    public static Boolean progressBarDraggingCap = false;


    @FXML
    public void initialize() {
        // canvas track parent size
        waveformCanvas.widthProperty().bind(waveformCanvasParent.widthProperty());
        waveformCanvas.heightProperty().bind(waveformCanvasParent.heightProperty());

        waveformCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawWaveform());
        waveformCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawWaveform());

        waveformCanvas.setOnMousePressed(event -> {
            progressBarDraggingCap = true;
            updateProgressFromMouse(event.getX(), false); // just update visual, no seek
        });

        waveformCanvas.setOnMouseDragged(event -> {
            progressBarDraggingCap = true;
            updateProgressFromMouse(event.getX(), false); // just update visual, no seek
        });

        waveformCanvas.setOnMouseReleased(event -> {
            progressBarDraggingCap = false;
            updateProgressFromMouse(event.getX(), true);
        });
    }

    private void updateProgressFromMouse(double mouseX, boolean seek) {
        double canvasWidth = waveformCanvas.getWidth();
        double currentProgress = Math.max(0, Math.min(mouseX / canvasWidth, 1.0));


        // Update the drawing
        updateProgress(currentProgress);

        //seek
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
                if (ext.equals("mp3") || ext.equals("m4a")) {
                    loadMP3Waveform(audioFile);
                } else if (ext.equals("wav"))  {
                    loadWAVWaveform(audioFile);
                } else {
                    System.out.println("Unsupported audio format: " + ext);
                }

                drawWaveform();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    //Pulse Code Modulation
    private void loadMP3Waveform(File mp3File) {
        try (InputStream fis = new FileInputStream(mp3File)) {
            Bitstream bitstream = new Bitstream(fis);
            Decoder decoder = new Decoder();

            java.util.List<Double> samples = new java.util.ArrayList<>();

            javazoom.jl.decoder.Header frameHeader;
            while ((frameHeader = bitstream.readFrame()) != null) {
                SampleBuffer output = (SampleBuffer) decoder.decodeFrame(frameHeader, bitstream);
                short[] buffer = output.getBuffer();
                for (short s : buffer) {
                    samples.add((double) Math.abs(s) / 32768.0); // normalize
                }
                bitstream.closeFrame();
            }

            //Undersampling
            int targetSize = 1000;
            waveform = new double[targetSize];
            int step = samples.size() / targetSize;
            if (step == 0) step = 1;

            for (int i = 0; i < targetSize; i++) {
                int idx = i * step;
                if (idx >= samples.size()) idx = samples.size() - 1;
                waveform[i] = samples.get(idx);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWAVWaveform(File wavFile) {
        try {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(wavFile);
            AudioFormat format = audioStream.getFormat();

            int bytesPerFrame = format.getFrameSize();
            if (bytesPerFrame == AudioSystem.NOT_SPECIFIED) {
                bytesPerFrame = 1;
            }

            int numBytes = (int) audioStream.getFrameLength() * bytesPerFrame;
            byte[] audioBytes = new byte[numBytes];
            int bytesRead = audioStream.read(audioBytes);

            waveform = new double[1000]; // fixed size
            int step = bytesRead / waveform.length; // downsample to fit

            for (int i = 0; i < waveform.length; i++) {
                int idx = i * step;
                if (idx + 1 < audioBytes.length) {
                    short sample = (short) ((audioBytes[idx + 1] << 8) | (audioBytes[idx] & 0xff));
                    waveform[i] = Math.abs(sample) / 32768.0; // normalize
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawWaveform() {
        if (waveform == null || waveform.length == 0) return;

        Platform.runLater(() -> {
            GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, waveformCanvas.getWidth(), waveformCanvas.getHeight());
            gc.setFill(Color.LIGHTBLUE);

            double canvasWidth = waveformCanvas.getWidth();
            double canvasHeight = waveformCanvas.getHeight();
            int length = waveform.length;
            double barWidth = canvasWidth / length;
            System.out.println("Length: " + length + " Bar width: " + barWidth);
            for (int i = 0; i < length; i++) {
                double value = waveform[i];
                double barHeight = value * canvasHeight;
                gc.fillRect(i * barWidth, (canvasHeight - barHeight) / 2, barWidth, barHeight);//xpos//ypos//rect width//rect height
            }
        });
    }

    public void updateProgress(double progress) {
        if (waveform == null || waveform.length == 0) return;

        Platform.runLater(() -> {
            GraphicsContext gc = waveformCanvas.getGraphicsContext2D();
            gc.clearRect(0, 0, waveformCanvas.getWidth(), waveformCanvas.getHeight());

            double canvasWidth = waveformCanvas.getWidth();
            double canvasHeight = waveformCanvas.getHeight();
            int length = waveform.length;
            double barWidth = canvasWidth / length;

            int cutoff = (int) (length * progress);

            for (int i = 0; i < length; i++) {
                double value = waveform[i];
                double barHeight = value * canvasHeight;

                // color: dark blue if <= progress, light blue otherwise
                if (i <= cutoff) gc.setFill(Color.DARKBLUE);
                else gc.setFill(Color.LIGHTBLUE);

                gc.fillRect(i * barWidth, (canvasHeight - barHeight) / 2, barWidth, barHeight);
            }
        });
    }

}
