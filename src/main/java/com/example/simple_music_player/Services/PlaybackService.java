package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.AlbumCoverController;
import com.example.simple_music_player.Controller.LibraryController;
import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.TrackDAO;
import com.example.simple_music_player.db.UserPrefDAO;
import javafx.beans.property.*;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class PlaybackService {
    @Getter
    @Setter
    private int currentIndex = -1; //tracks the song that is active
    private MediaPlayer mediaPlayer; //plays sound

    private final TrackDAO trackDao = new TrackDAO(DatabaseManager.getConnection());
    private final ObjectProperty<Track> currentTrack = new SimpleObjectProperty<>(null); //value changes to listener automatically
    //double value combined with listener changes //used in progress bar
    private final DoubleProperty progress = new SimpleDoubleProperty(0);
    private final StringProperty elapsedTime = new SimpleStringProperty("00:00");
    private final StringProperty remainingTime = new SimpleStringProperty("00:00");

    public ReadOnlyStringProperty elapsedTimeProperty() {
        return elapsedTime;
    }

    public ReadOnlyStringProperty remainingTimeProperty() {
        return remainingTime;
    }

    public ReadOnlyObjectProperty<Track> currentTrackProperty() {
        return currentTrack;
    }

    public ReadOnlyDoubleProperty progressProperty() {
        return progress;
    }

    @Getter
    public static List<Integer> playlist; // just keep IDs of songs in order
    @Setter
    NowPlayingController nowPlayingController;
    @Setter
    AlbumCoverController albumCoverController;

    private final UserPrefDAO userPrefDAO = new UserPrefDAO(DatabaseManager.getConnection());

    @Getter
    private static final PlaybackService instance = new PlaybackService();

    public PlaybackService() {}

    public int getPlaylistId(int idx) {
        System.out.println("Index: " + idx + " songId: " + playlist.get(idx));
        return playlist.get(idx);
    }

    public void setPlaylist(List<Integer> ids, boolean autoPlay) {
        playlist = ids;
        if (playlist.isEmpty()) {
            nowPlayingController.clearScreen();
            albumCoverController.clearCover();
            pause();
            currentIndex = -1;
        }
        if (autoPlay && !ids.isEmpty()) {
            currentIndex = 0;
            play(currentIndex);
        }
    }

    //Playlist for initial loading for directory
    public void setPlaylist(List<Integer> ids, int idx, String status, long ts) {
        playlist = ids;
        if (playlist.isEmpty()) {
            nowPlayingController.clearScreen();
            albumCoverController.clearCover();
            pause();
            currentIndex = -1;
            return;
        }


        // Prepare the player for the desired index
        currentIndex = idx;
        System.out.println("Initial Playing: " + idx);
        UserPref.playlistNo = currentIndex;
        int songId = playlist.get(idx);
        Track t = trackDao.getTrackById(songId);
        currentTrack.set(t);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        Media media = new Media(new File(t.getPath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        if(status == null) status = "Play";
        String finalStatus = status; //emergency handling
        mediaPlayer.setOnReady(() -> {
            mediaPlayer.seek(Duration.millis(ts));
            if (finalStatus.equals("Pause")) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });

        mediaPlayer.setOnEndOfMedia(this::next);

    }


    public void clearList() {
        playlist.clear();
        System.out.println("Playlist Cleared");
    }

    public void play(int index) {
        if (index < 0 || index >= playlist.size()) return;
        System.out.println("Currently playing " + index);
        //
        UserPref.playlistNo = index;
        //
        currentIndex = index;
        int songId = playlist.get(index);
        Track t = trackDao.getTrackById(songId);  // fetch from DB only now
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

        //Duration formatter
        mediaPlayer.currentTimeProperty().addListener((obs, oldT, newT) -> {
            if (playlist == null || playlist.isEmpty()) {
                progress.set(0);
                elapsedTime.set("00:00");
                remainingTime.set("00:00");
                return;
            }

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
                int totalSec = (int) total.toSeconds();

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
        if (mediaPlayer != null) {
            mediaPlayer.play();
        } else if (currentIndex >= 0) {
            play(currentIndex);
        }
    }

    public void pause() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
            System.out.println("Media paused");
        }
    }

    public void togglePlayPause() {
        if (mediaPlayer == null) {
            play();
            return;
        }
        MediaPlayer.Status s = mediaPlayer.getStatus();
        if (s == MediaPlayer.Status.PLAYING) {
            UserPref.status = "Pause";
            pause();
        } else {
            UserPref.status = "Play";
            play();
        }
    }

    public void next() {
        if (playlist.isEmpty()) return;
        if (checkRestartFromStart()) currentIndex = -1;
        int nextIndex = (currentIndex + 1) % playlist.size();
        play(nextIndex);
    }

    public void previous() {
        if (playlist.isEmpty()) return;
        if (checkRestartFromStart()) currentIndex = 1;
        int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        play(prevIndex);
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

    private boolean checkRestartFromStart() {
        if (LibraryController.restartFromStart) {
            LibraryController.restartFromStart = false;
            return true;
        }
        return false;
    }

    public void closePlaybackService() throws SQLException {
        if (mediaPlayer != null) {
            Duration currentTime = mediaPlayer.getCurrentTime();
            UserPref.timestamp = (long) currentTime.toMillis();
        }
        userPrefDAO.setUserPref();
    }

}
