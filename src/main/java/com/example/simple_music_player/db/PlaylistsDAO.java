package com.example.simple_music_player.db;

import com.example.simple_music_player.Model.Playlist;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PlaylistsDAO {
    private final Connection conn;

    public PlaylistsDAO(Connection connection) {
        this.conn = connection;
    }

    public void deleteAllSongsFromPlaylist(int playlistId) throws SQLException {
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

    public void deleteSongsFromPlaylist(int playlistId, List<Integer> songIds) throws SQLException {
        if (songIds == null || songIds.isEmpty()) return; // nothing to delete

        // Build the placeholders (?, ?, ?, ...)
        String placeholders = String.join(", ", Collections.nCopies(songIds.size(), "?"));

        String deleteSql = """
        DELETE FROM playlist_songs
        WHERE playlist_id = ?
        AND song_id IN (""" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(deleteSql)) {
            ps.setInt(1, playlistId);
            for (int i = 0; i < songIds.size(); i++) {
                ps.setInt(i + 2, songIds.get(i)); // +2 because index 1 is playlistId
            }
            ps.executeUpdate();
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

    public boolean isSongInPlaylist(int playlistId, int songId) throws SQLException {
        String sql = """
        SELECT EXISTS(
            SELECT 1 FROM playlist_songs
            WHERE playlist_id = ?
            AND song_id = ?
        )
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) == 1;
                }
            }
        }
        return false;
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

    public void createFavPlaylist() throws SQLException {
        String createPlaylist = """
                INSERT OR IGNORE INTO playlists (id, name) VALUES (3, 'Favourite')
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

    public void createPlaylist(String name) throws SQLException {
        String getMaxId = "SELECT MAX(id) FROM playlists";
        int nextId = 4;

        try (PreparedStatement ps = conn.prepareStatement(getMaxId);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int maxId = rs.getInt(1);
                nextId = maxId + 1;
            }
        }

        // Insert with the calculated ID
        String createPlaylist = "INSERT INTO playlists (id, name) VALUES (?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(createPlaylist)) {
            ps.setInt(1, nextId);
            ps.setString(2, name);
            ps.executeUpdate();
        }
    }

    public List<Playlist> getAllPlaylistsAboveId(int minId) throws SQLException {
        String sql = "SELECT id, name FROM playlists WHERE id > ?";
        List<Playlist> playlists = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    playlists.add(new Playlist(rs.getInt("id"), rs.getString("name")));
                }
            }
        }
        return playlists;
    }

    public void renamePlaylist(int playlistId, String newName) throws SQLException {
        String sql = "UPDATE playlists SET name = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, playlistId);
            ps.executeUpdate();
        }
    }

    public void deletePlaylist(int playlistId) throws SQLException {
        String del = """
                DELETE FROM playlists WHERE id = ?
                """;
        deleteAllSongsFromPlaylist(playlistId);
        try(PreparedStatement ps = conn.prepareStatement(del)) {
            ps.setInt(1, playlistId);
            ps.executeUpdate();
        }
    }


}
