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

    public void deletePlaylist(int playlistId) throws SQLException {
        String deletePlaylist = """
                DELETE FROM playlist_songs WHERE playlist_id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(deletePlaylist)) {
            ps.setInt(1, playlistId);
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

    public void createShuffledPlaylist() throws SQLException {
        String createPlaylist = """
                        INSERT OR IGNORE INTO playlists (id, name) VALUES (1, 'Shuffled Playlist')
                """;
        try (PreparedStatement ps = conn.prepareStatement(createPlaylist)) {
            ps.executeUpdate();
        }
    }

    public void createNormalPlaylist() throws SQLException {
        String createPlaylist = """
                INSERT OR IGNORE INTO playlists (id, name) VALUES (2, 'Normal')
                """;
        try (PreparedStatement ps = conn.prepareStatement(createPlaylist)) {
            ps.executeUpdate();
        }
    }

    public void replaceSongsInPlaylist(int playlistId, List<Integer> newSongIds) throws SQLException {
        String deleteSql = "DELETE FROM playlist_songs WHERE playlist_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, playlistId);
            ps.executeUpdate();
        }

        String insertSql = """
                INSERT INTO playlist_songs (playlist_id, song_id)
                VALUES (?, ?)
                """;
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            for (Integer songId : newSongIds) {
                ps.setInt(1, playlistId);
                ps.setInt(2, songId);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public int getPlaylistSongsIdx(int playlistId, int songId) throws SQLException {
        String sql = """
        SELECT row_number FROM (
            SELECT song_id,
                   ROW_NUMBER() OVER (ORDER BY id) AS row_number
            FROM playlist_songs
            WHERE playlist_id = ?
        )
        WHERE song_id = ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("row_number") - 1;
                }
            }
        }
        return -1; // not found
    }

}
