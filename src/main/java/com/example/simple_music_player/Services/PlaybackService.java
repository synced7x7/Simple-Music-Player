package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.AlbumCoverController;
import com.example.simple_music_player.Controller.LibraryController;
import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Model.SongLocator;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.SimpleMusicPlayer;
import com.example.simple_music_player.db.*;
import javafx.application.Platform;
import javafx.beans.property.*;

import javafx.scene.control.ListView;
import javafx.scene.media.Media;
import javafx.scene.media.MediaException;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import ws.schild.jave.Encoder;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlaybackService {
    @Getter
    @Setter
    private int currentIndex = -1; //tracks the song that is active
    @Getter
    private MediaPlayer mediaPlayer; //plays sound

    private final TempTrackDAO tempTrackDAO = new TempTrackDAO(DatabaseManager.getConnection());
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

    public Track getCurrentTrack() {
        return currentTrack.get();
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
        Track t;
        if(SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
            t = trackDao.getTrackById(songId);
        else{ //for commandline Argument
            t = tempTrackDAO.getTrackById(songId);
            double vol = userPrefDAO.getVolume();
            setVolume(vol);
            nowPlayingController.setInitialVolumeSliderControllerValue(vol);
            UserPref.volume = vol;
            nowPlayingController.toggleCountdown();
        }
        currentTrack.set(t);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            //mediaPlayer.dispose();
        }
        System.out.println("Currently Playing:-> Song Id:: " + songId + " , Index:: " + idx);
        //Check if it is flac
        //Checkpoint 1
        File audioFile = new File(t.getPath());
        if (!audioFile.exists()) {
            System.out.println("Cannot find the file specified: " + audioFile.getAbsolutePath());
            safelyDeleteSong(songId);
            // Move to next track safely
            try {
                next();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            return; // exit early
        }

        String ext = getFileExtension(audioFile);

        if (ext.equals("flac")) {
            audioFile = convertFlacToTempWav(audioFile);
        }

        Media media;
        try {
            media = new Media(audioFile.toURI().toString());
        } catch (MediaException me) {
            System.out.println("MediaException, renaming original file: " + audioFile.getName());

            File finalAudioFile = audioFile;
            CompletableFuture.runAsync(() -> {
                File safeFile = new File(finalAudioFile.getParentFile(), "ini_syncedX_" + System.currentTimeMillis() + "." + ext);
                try {
                    // Move the file safely
                    Files.move(finalAudioFile.toPath(), safeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Update DB
                    trackDao.updateTrackPath(songId, safeFile.getAbsolutePath());

                    // Now construct Media on FX thread
                    Platform.runLater(() -> {
                        try {
                            Media newMedia = new Media(safeFile.toURI().toString());
                            mediaPlayer = new MediaPlayer(newMedia);
                            mediaPlayer.play();
                        } catch (MediaException e2) {
                            System.err.println("Failed to load renamed file: " + safeFile.getAbsolutePath());
                            safelyDeleteSong(songId);
                            Platform.runLater(() -> {
                                try { next(); } catch (SQLException ex) { throw new RuntimeException(ex); }
                            });
                        }
                    });
                } catch (Exception ex) {
                    System.err.println("Could not rename file: " + finalAudioFile.getAbsolutePath());
                    safelyDeleteSong(songId);
                    Platform.runLater(() -> {
                        try { next(); } catch (SQLException e) { throw new RuntimeException(e); }
                    });
                }
            });

            return; // exit play() immediately to avoid blocking FX thread
        }

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
        else {
            SongLocator songLocator = SongLocator.getCurrent();
            int reverse = songLocator.getLastReverseBS();
            boolean ascending = reverse != 1;
            setListViewFocus(playlistsDAO.getPlaylistSongsIdx(libraryController.getCurrentPlaylistId(), songId, songLocator.getLastSortBS(), ascending));
        }
    }

    public void setListViewFocus(int idx) {
        if (libraryController != null) {
            Platform.runLater(() -> {
                if (libraryController.getCurrentPlaylistId() == UserPref.playlistId) {
                    libraryController.getSongListView().getSelectionModel().select(idx);
                    libraryController.getSongListView().scrollTo(idx);
                }
            });
        }
    }

    public void setupVisualizerListener(MediaPlayer mediaPlayer) {
        // === VISUALIZER HOOK ===
        if (NowPlayingController.getInstance().getRealtimeVisualizerController() != null) {
            RealtimeVisualizerService visualizer = NowPlayingController.getInstance().getRealtimeVisualizerController();
            mediaPlayer.setAudioSpectrumNumBands(128);     // Number of frequency bars
            mediaPlayer.setAudioSpectrumInterval(0.02);    // ~50 FPS update rate
            mediaPlayer.setAudioSpectrumThreshold(-80);    // Sensitivity
            mediaPlayer.setAudioSpectrumListener((timestamp, duration, magnitudes, phases) -> visualizer.updateSpectrum(magnitudes));
        }
    }

    private void restartVisualizer() {
        RealtimeVisualizerService   realtimeVisualizerService = NowPlayingController.getInstance().getRealtimeVisualizerController();
        if(!realtimeVisualizerService.getIsRunning()) return;
        realtimeVisualizerService.stopVisualizer();
        realtimeVisualizerService.startVisualizer();
        setupVisualizerListener(mediaPlayer);
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
                if (currentTrack.get() != null && NowPlayingController.visualizerController != null && !VisualizerService.progressBarDraggingCap) {
                    NowPlayingController.visualizerController.updateProgress(prog);
                }

                // Highlight current lyric - WRAP IN Platform.runLater
                if (currentTrack.get() != null && nowPlayingController != null) {
                    Platform.runLater(() -> {
                        nowPlayingController.highlightCurrentLyric(newT);
                        nowPlayingController.updateSyncedLyricLabel(newT);
                    });

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


        UserPref.playlistNo = index;
        currentIndex = index;
        int songId = playlist.get(index);
        Track t;
        if(SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
            t = trackDao.getTrackById(songId);
        else{
            System.out.println("Song Id: " + songId);
            t = tempTrackDAO.getTrackById(songId);
            System.out.println("temp track = " + t);
        }


        currentTrack.set(t);
        System.out.println("Currently Playing:-> Song Id:: " + songId + " , Index:: " + index);

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        //Check if it is flac //Checkpoint 2
        File audioFile = new File(t.getPath());
        if (!audioFile.exists()) {
            System.out.println("Cannot find the file specified: " + audioFile.getAbsolutePath());

            // Safely remove it from DB and playlist
            safelyDeleteSong(songId);

            // Move to next track safely
            try {
                next();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            return; // exit early
        }

        String ext = getFileExtension(audioFile);

        if (ext.equals("flac")) {
            audioFile = convertFlacToTempWav(audioFile);
        }

        Media media;
        try {
            media = new Media(audioFile.toURI().toString());
        } catch (MediaException me) {
            System.out.println("MediaException, renaming original file: " + audioFile.getName());

            File finalAudioFile = audioFile;
            CompletableFuture.runAsync(() -> {
                File safeFile = new File(finalAudioFile.getParentFile(), "syncedX_" + System.currentTimeMillis() + "." + ext);
                try {
                    // Move the file safely
                    Files.move(finalAudioFile.toPath(), safeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    // Update DB
                    trackDao.updateTrackPath(songId, safeFile.getAbsolutePath());

                    // Now construct Media on FX thread
                    Platform.runLater(() -> {
                        try {
                            Media newMedia = new Media(safeFile.toURI().toString());
                            mediaPlayer = new MediaPlayer(newMedia);
                            mediaPlayer.play();
                        } catch (MediaException e2) {
                            System.err.println("Failed to load renamed file: " + safeFile.getAbsolutePath());
                            safelyDeleteSong(songId);
                            Platform.runLater(() -> {
                                try { next(); } catch (SQLException ex) { throw new RuntimeException(ex); }
                            });
                        }
                    });
                } catch (Exception ex) {
                    System.err.println("Could not rename file: " + finalAudioFile.getAbsolutePath());
                    safelyDeleteSong(songId);
                    Platform.runLater(() -> {
                        try { next(); } catch (SQLException e) { throw new RuntimeException(e); }
                    });
                }
            });

            return;
        }
        
        mediaPlayer = new MediaPlayer(media);

        mediaPlayer.setOnReady(() -> mediaPlayer.play());

        setVolume(UserPref.volume);
        setupDurationListener(mediaPlayer);
        restartVisualizer();

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
        else {
            SongLocator songLocator = SongLocator.getCurrent();
            int reverse = songLocator.getLastReverseBS();
            boolean ascending = reverse != 1;
            setListViewFocus(playlistsDAO.getPlaylistSongsIdx(libraryController.getCurrentPlaylistId(), songId, songLocator.getLastSortBS(), ascending));
        }
    }

    private void safelyDeleteSong(int songId) {
        try {
            Track t = trackDao.getTrackById(songId);
            if (t == null) return;

            String path = t.getPath();
            if (path != null && !path.isEmpty()) {
                File file = new File(path);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (!deleted) {
                        System.err.println("Could not delete file: " + path);
                    } else {
                        System.out.println("Deleted file: " + path);
                    }
                }
            }

            // Remove from database and playlists
            trackDao.removeFromLibrary(songId);
            playlistsDAO.deleteSongFromAllPlaylists(songId);

            // Remove from UI and playback list
            ListView<Integer> songListView = libraryController.getSongListView();
            Platform.runLater(() -> {
                songListView.getItems().remove(Integer.valueOf(songId));
                PlaybackService.playlist.remove((Integer) songId);
            });

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
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
        QueueService queueService = AppContext.getQueueService();
        LinkedList<Integer> queueList = queueService.getQueueList();
        while (!queueList.isEmpty()) {
            int nextId = queueList.pollFirst();
            int indexInPlaylist = playlist.indexOf(nextId);
            queueService.refreshSongListView();
            if (indexInPlaylist != -1) {
                currentIndex = indexInPlaylist;
                play(currentIndex);
                return;
            }
        }

        if (playlist.isEmpty()) return;
        int nextIndex;
        if (checkRestartFromStart()) {
            currentIndex = 0;
            nextIndex = 0;
        } else nextIndex = (currentIndex + 1) % playlist.size();
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

    public String formatTime(int seconds) {
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
        // Clean up temporary WAV file
        if (tempWavFile != null && tempWavFile.exists()) {
            tempWavFile.delete();
            tempWavFile = null;
        }
        userPrefDAO.setUserPref();
        tempTrackDAO.deleteSongsFromTempTable();
    }

    public void initialTimePropertyBinding() {
        nowPlayingController.bindTextPropertyToTime();
    }

    public void shufflePlaylist() throws SQLException {
        if (playlist.isEmpty() || currentIndex < 0 || currentIndex >= playlist.size()) {
            System.err.println("Invalid currentIndex or empty playlist");
            return;
        }
        SongLocator.create(libraryController.getSortStatusOfPlaylist(libraryController.getCurrentPlaylistId()), playlistsDAO.getReverse(libraryController.getCurrentPlaylistId()));
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

    public void songRelocator() throws SQLException {
        //System.out.println("Playlist before relocation: " + playlist);
        SongLocator songLocator = SongLocator.getCurrent();
        int currentSongId = playlist.get(currentIndex);
        String sort = songLocator.getLastSortBS();
        boolean rev = songLocator.getLastReverseBS() != 1;
        playlist.clear();
        playlist = trackDao.getAllIdsSorted(UserPref.playlistId, sort, rev);
        currentIndex = playlist.indexOf(currentSongId);
        //System.out.println("Playlist after relocation: " + playlist);
        libraryController.toggleSort(false);
        playlistsDAO.deleteAllSongsFromPlaylist(1);
    }

    public void seek(Duration timestamp) {
        if (mediaPlayer != null && mediaPlayer.getTotalDuration() != null) {
            mediaPlayer.seek(timestamp);
        }
    }

    private File tempWavFile = null; // Store temp file reference
    private Encoder encoder;

    public String getFileExtension(File file) {
        String name = file.getName();
        int lastDot = name.lastIndexOf('.');
        if (lastDot == -1) return "";
        return name.substring(lastDot + 1).toLowerCase();
    }

    private File convertFlacToTempWav(File flacFile) {
        try {
            // Clean up previous temp file
            if (tempWavFile != null && tempWavFile.exists()) {
                tempWavFile.delete();
            }

            // Create temporary WAV file
            tempWavFile = File.createTempFile("flac_temp_", ".wav");
            tempWavFile.deleteOnExit();

            // Set Audio Attributes
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            // Set encoding attributes
            EncodingAttributes attributes = new EncodingAttributes();
            attributes.setOutputFormat("wav");
            attributes.setAudioAttributes(audio);

            // Encode
            encoder = encoder != null ? encoder : new Encoder();
            encoder.encode(new MultimediaObject(flacFile), tempWavFile, attributes);

            System.out.println("Converted FLAC to temporary WAV: " + tempWavFile.getAbsolutePath());
            return tempWavFile;

        } catch (Exception e) {
            System.err.println("Failed to convert FLAC to WAV, returning original file");
            return flacFile; // Fallback to original (will fail but prevents crash)
        }
    }

}
