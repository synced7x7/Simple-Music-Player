package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.AlbumCoverController;
import com.example.simple_music_player.Controller.LibraryController;
import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Model.SongLocator;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.PlaylistsDAO;
import com.example.simple_music_player.db.TrackDAO;
import com.example.simple_music_player.db.UserPrefDAO;
import javafx.application.Platform;
import javafx.beans.property.*;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.sql.SQLException;
import java.util.Collections;
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
    public static List<Integer> playlist;// just keep IDs of songs in order
    @Setter
    NowPlayingController nowPlayingController;
    @Setter
    AlbumCoverController albumCoverController;
    private final UserPrefDAO userPrefDAO = new UserPrefDAO(DatabaseManager.getConnection());
    private final PlaylistsDAO playlistsDAO = new PlaylistsDAO(DatabaseManager.getConnection());

    @Getter
    private static final PlaybackService instance = new PlaybackService();

    @Getter
    @Setter
    private static LibraryController libraryController;

    public PlaybackService() {
    }



    public void setPlaylist(List<Integer> ids, boolean autoPlay) throws SQLException {
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
    public void setPlaylist(List<Integer> ids, int idx, String status, long ts) throws SQLException {
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
        System.out.println("Currently Playing:-> Song Id:: " + songId + " , Index:: " + idx);
        Media media = new Media(new File(t.getPath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);
        setupDurationListener(mediaPlayer);
        if (status == null) status = "Play";
        String finalStatus = status; //emergency handling
        mediaPlayer.setOnReady(() -> {
            mediaPlayer.seek(Duration.millis(ts));
            if (finalStatus.equals("Pause")) {
                mediaPlayer.pause();
            } else {
                mediaPlayer.play();
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            if (UserPref.repeat == 1) {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            } else {
                try {
                    next();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        if (UserPref.shuffle == 0) setListViewFocus(idx);
        else setListViewFocus(playlistsDAO.getPlaylistSongsIdx(2, songId));
    }

    private void setListViewFocus(int idx) {
        if (libraryController != null) {
            Platform.runLater(() -> {
                libraryController.getSongListView()
                        .getSelectionModel().select(idx);
                libraryController.getSongListView()
                        .scrollTo(idx);
            });
        }
    }

    private void setupDurationListener(MediaPlayer player) {
        player.currentTimeProperty().addListener((obs, oldT, newT) -> {
            if (playlist == null || playlist.isEmpty()) {
                progress.set(0);
                elapsedTime.set("00:00");
                remainingTime.set("00:00");
                return;
            }

            Duration total = player.getTotalDuration();

            if (total != null && !total.isUnknown() && total.toMillis() > 0) {
                double prog = newT.toMillis() / total.toMillis();
                progress.set(prog);

                // update waveform progress
                if (currentTrack.get() != null &&
                    NowPlayingController.visualizerController != null &&
                    !VisualizerService.progressBarDraggingCap) {
                    NowPlayingController.visualizerController.updateProgress(prog);
                }

                int currentSec = (int) newT.toSeconds();
                int totalSec = (int) total.toSeconds();

                elapsedTime.set(formatTime(currentSec));

                int remaining = Math.max(totalSec - currentSec, 0);
                remainingTime.set(formatTime(remaining));
            } else {
                progress.set(0);
                elapsedTime.set("00:00");
                remainingTime.set("00:00");
            }
        });

    }


    public void clearList() {
        playlist.clear();
        System.out.println("Playlist Cleared");
    }

    public void play(int index) throws SQLException {
        NowPlayingController.visualizerController.cleanup();
        if (index < 0 || index >= playlist.size()) return;
        //
        UserPref.playlistNo = index;
        //
        currentIndex = index;
        int songId = playlist.get(index);
        Track t = trackDao.getTrackById(songId);  // fetch from DB only now
        currentTrack.set(t);
        System.out.println("Currently Playing:-> Song Id:: " + songId + " , Index:: " + index);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        Media media = new Media(new File(t.getPath()).toURI().toString());
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> {
            mediaPlayer.play();
        });

        setupDurationListener(mediaPlayer);


        mediaPlayer.setOnEndOfMedia(() -> {
            if (UserPref.repeat == 1) {
                mediaPlayer.seek(Duration.ZERO);
                mediaPlayer.play();
            } else {
                try {
                    next();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        if (UserPref.shuffle == 0) setListViewFocus(index);
        else setListViewFocus(playlistsDAO.getPlaylistSongsIdx(2, songId));
    }

    public void setVolume(double volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume);
        } else System.out.println("MediaPlayer is null in setVolume function");
    }


    public void play() throws SQLException {
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

    public void togglePlayPause() throws SQLException {
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

    public void next() throws SQLException {
        if (playlist.isEmpty()) return;
        int nextIndex;
        if (checkRestartFromStart()) {
            currentIndex = 0;
            nextIndex = 0;
        } else
            nextIndex = (currentIndex + 1) % playlist.size();
        play(nextIndex);
    }

    public void previous() throws SQLException {
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
            Duration currentTime;
            currentTime = mediaPlayer.getCurrentTime();
            UserPref.timestamp = (long) currentTime.toMillis();
        }
        userPrefDAO.setUserPref();
        // if(UserPref.shuffle == 1) playlistsDAO.insertSongsInPlaylist(1, playlist);
        if (UserPref.shuffle == 0) playlistsDAO.deleteAllSongsFromPlaylist(1);
    }

    public void initialTimePropertyBinding() {
        nowPlayingController.bindTextPropertyToTime();
    }

    public void shufflePlaylist() throws SQLException {
        if (playlist.isEmpty() || currentIndex < 0 || currentIndex >= playlist.size()) {
            System.err.println("Invalid currentIndex or empty playlist");
            return;
        }
        libraryController.toggleSort(true);
        System.out.println("Playlist before shuffling: " + playlist);
        int songId = playlist.get(currentIndex);
        playlist.remove(Integer.valueOf(songId));
        Collections.shuffle(playlist);
        currentIndex = 0;
        playlist.addFirst(songId);
        playlistsDAO.deleteAllSongsFromPlaylist(1);
        playlistsDAO.insertSongsInPlaylist(1, playlist);
        UserPref.playlistNo = currentIndex;
        System.out.println("Playlist after shuffling: " + playlist);
    }

    public void songRelocator() {
        //System.out.println("Playlist before relocation: " + playlist);
        SongLocator songLocator = SongLocator.getCurrent();
        int currentSongId = playlist.get(currentIndex);
        String sort = songLocator.getLastSortBS();
        boolean rev = songLocator.getLastReverseBS() != 1;
        playlist.clear();
        playlist = trackDao.getAllIdsSorted(sort, rev);
        currentIndex = playlist.indexOf(currentSongId);
        //System.out.println("Playlist after relocation: " + playlist);
        libraryController.toggleSort(false);
    }


}
