package com.example.simple_music_player.Services;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.SimpleMusicPlayer;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.TempTrackDAO;
import com.example.simple_music_player.db.TrackDAO;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TempPlaylistService {

    private final PlaybackService playbackService;
    private final TempTrackDAO tempTrackDAO = new TempTrackDAO(DatabaseManager.getConnection());

    public TempPlaylistService(PlaybackService playbackService) {
        this.playbackService = playbackService;
    }

    public void loadTempPlaylist() throws SQLException {
        List<String> filePaths = SimpleMusicPlayer.argument;
        List<Integer> idsToLoad = new ArrayList<>();
        for (String path : filePaths) {
            try {
                Track track = new Track(path); // Create Track from file
                tempTrackDAO.insertIntoTempSongs(track);
            } catch (Exception e) {
                System.out.println("Failed to load track: " + path);
                throw new RuntimeException(e);
            }
        }

        idsToLoad = (tempTrackDAO.getAllIdsSortByDefault());
        System.out.println("idsToLoad: " + idsToLoad);
        playbackService.setPlaylist(idsToLoad, 0, "Play", 0);
    }
}
