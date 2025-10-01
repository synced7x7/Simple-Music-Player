package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Model.Track;
import javafx.beans.property.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PlaybackService {
    private final List<Track> playlist = new ArrayList<>(); //songs
    private int currentIndex = -1; //tracks the song that is active
    private MediaPlayer mediaPlayer; //plays sound


    private final ObjectProperty<Track> currentTrack = new SimpleObjectProperty<>(null); //value changes to listener automatically
    //double value combined with listener changes //used in progress bar
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty elapsedTime = new SimpleStringProperty("00:00");
    private final StringProperty remainingTime = new SimpleStringProperty("00:00");

    public ReadOnlyStringProperty elapsedTimeProperty() { return elapsedTime; }
    public ReadOnlyStringProperty remainingTimeProperty() { return remainingTime; }

    public ReadOnlyObjectProperty<Track> currentTrackProperty() { return currentTrack; }
    public ReadOnlyDoubleProperty progressProperty() { return progress; }


    public void setPlaylist(List<Track> tracks) {
        playlist.clear(); playlist.addAll(tracks);
        if (!playlist.isEmpty()) play(0);
    }

    public void play(int index) {
        if (index < 0 || index >= playlist.size()) return;
        currentIndex = index;
        Track t = playlist.get(index);
        currentTrack.set(t);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        Media media = new Media(new File(t.getPath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            mediaPlayer.play();
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
            Duration total = mediaPlayer.getTotalDuration();

            if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                double prog = newT.toMillis() / total.toMillis();
                progress.set(prog);

                // update waveform progress
                if (currentTrack.get() != null && NowPlayingController.visualizerController != null && VisualizerService.progressBarDraggingCap == false) {
                    NowPlayingController.visualizerController.updateProgress(prog);
                }

                // calculate run-up (elapsed)
                int currentSec = (int) newT.toSeconds();
                int totalSec   = (int) total.toSeconds();

                elapsedTime.set(formatTime(currentSec));

                // calculate countdown (remaining)
                int remaining = Math.max(totalSec - currentSec, 0);
                remainingTime.set(formatTime(remaining));
            } else {
                progress.set(0);
                elapsedTime.set("00:00");
                remainingTime.set("00:00");
            }
        });

        mediaPlayer.setOnEndOfMedia(this::next); //when song finishes automatically move to next song
    }

    public void play() {
        if (mediaPlayer != null) mediaPlayer.play();
        else if (currentIndex >= 0) play(currentIndex);
    }
    public void pause() { if (mediaPlayer != null) mediaPlayer.pause(); }

    public void togglePlayPause() {
        if (mediaPlayer == null) { play(); return; }
        MediaPlayer.Status s = mediaPlayer.getStatus();
        if (s == MediaPlayer.Status.PLAYING) pause(); else play();
    }

    public void next() {
        if (playlist.isEmpty()) return;
        play((currentIndex + 1) % playlist.size());
    }
    public void previous() {
        if (playlist.isEmpty()) return;
        int prev = (currentIndex - 1 + playlist.size()) % playlist.size();
        play(prev);
    }

    public void updateProgressFromMouse(double currentProgress) {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            Duration newTime = mediaPlayer.getTotalDuration().multiply(currentProgress);
            mediaPlayer.seek(newTime);
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int sec = seconds % 60;
        return String.format("%02d:%02d", minutes, sec);
    }

}
