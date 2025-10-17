package com.example.simple_music_player.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlaylistsDAO {
    private final Connection conn;

    public PlaylistsDAO(Connection connection) {
        this.conn = connection;
    }

    public void insertShuffledPlaylist() throws SQLException {
        String insertPlaylist = """
                    INSERT OR IGNORE INTO playlists (id, name)
                    VALUES (1, 'Shuffled Playlist')
                """;
        try (PreparedStatement ps = conn.prepareStatement(insertPlaylist)) {
            ps.executeUpdate();
        }
    }

    public void deleteShuffledPlaylistSongs() throws SQLException {
        String deleteExisting = """
                    DELETE FROM playlist_songs WHERE playlist_id = 1
                """;
        try (PreparedStatement ps = conn.prepareStatement(deleteExisting)) {
            ps.executeUpdate();
        }
    }

    public void insertSongsInPlaylist(int playlistId, List<Integer> songIds) throws SQLException {
        String insertSongs = """
            INSERT INTO playlist_songs (playlist_id, song_id)
            VALUES (?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(insertSongs)) {
            for (Integer songId : songIds) {
                ps.setInt(1, playlistId);
                ps.setInt(2, songId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }


    public List<Integer> getSongsFromPlaylist(int playlistId) throws SQLException {
        String sql = "SELECT song_id FROM playlist_songs WHERE playlist_id = ? ORDER BY id";

        List<Integer> songIds = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    songIds.add(rs.getInt("song_id"));
                }
            }
        }
        return songIds;
    }

    public void createPlaylist() throws SQLException {
        String createPlaylist = """
                INSERT OR IGNORE INTO playlists (id, name) VALUES (1, 'Shuffled Playlist')
        """;
        try (PreparedStatement ps = conn.prepareStatement(createPlaylist)) {
            ps.executeUpdate();
        }
    }


}
