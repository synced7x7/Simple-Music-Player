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
                         INSERT OR IGNORE INTO playlists (id, name, sort, rev) VALUES (1, 'Shuffled Playlist', 'Title', 0)
                """;
        try (PreparedStatement ps = conn.prepareStatement(createPlaylist)) {
            ps.executeUpdate();
        }
    }

    public void createNormalPlaylist() throws SQLException {
        String createPlaylist = """
                INSERT OR IGNORE INTO playlists (id, name, sort, rev) VALUES (2, 'All Songs', 'Title', 0)
                """;
        try (PreparedStatement ps = conn.prepareStatement(createPlaylist)) {
            ps.executeUpdate();
        }
    }

    public void createFavPlaylist() throws SQLException {
        String createPlaylist = """
                INSERT OR IGNORE INTO playlists (id, name, sort, rev) VALUES (3, 'Favourite', 'Title', 0)
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

    public int getPlaylistSongsIdx(int playlistId, int songId, String criteria, boolean ascending) throws SQLException {
        if (criteria.equals("Date Added"))
            ascending = !ascending;

        String order = ascending ? "ASC" : "DESC";

        String orderByColumn = switch (criteria) {
            case "Title" -> "s.title";
            case "Artist" -> "s.artist";
            case "Album" -> "s.album";
            case "Length" -> "CAST(s.length AS INTEGER)";
            case "Date Added" -> "s.date_added";
            default -> "s.title"; // fallback
        };

        String sql = """
            SELECT row_number FROM (
                SELECT s.id AS song_id,
                       ROW_NUMBER() OVER (ORDER BY %s %s) AS row_number
                FROM playlist_songs ps
                JOIN songs s ON ps.song_id = s.id
                WHERE ps.playlist_id = ?
            )
            WHERE song_id = ?
        """.formatted(orderByColumn, order);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("row_number") - 1;  // zero-based index
                }
            }
        }

        return -1;  // not found
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
        String createPlaylist = "INSERT INTO playlists (id, name, sort, rev) VALUES (?, ?, 'Title', 0)";

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
        try (PreparedStatement ps = conn.prepareStatement(del)) {
            ps.setInt(1, playlistId);
            ps.executeUpdate();
        }
    }

    public void setPlaylistSort(int playlistId, String sort) throws SQLException {
        String sql = """
        UPDATE playlists
        SET sort = ?
        WHERE id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Update the targeted playlist
            ps.setString(1, sort);
            ps.setInt(2, playlistId);
            ps.executeUpdate();
        }

        // Also update playlist ID 1 to keep sort in sync
        if (playlistId != 1) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sort);
                ps.setInt(2, 1);
                ps.executeUpdate();
            }
        }
    }

    public void setPlaylistRev(int playlistId, int rev) throws SQLException {
        String sql = """
        UPDATE playlists
        SET rev = ?
        WHERE id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // Update the targeted playlist
            ps.setInt(1, rev);
            ps.setInt(2, playlistId);
            ps.executeUpdate();
        }

        // Also update playlist ID 1 to keep rev in sync
        if (playlistId != 1) {
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, rev);
                ps.setInt(2, 1);
                ps.executeUpdate();
            }
        }
    }

    public String getSortingPref(int playlistId) throws SQLException {
        String sql = """
        SELECT sort FROM playlists WHERE id = ?;
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("sort");
                }
            }
        }
        return "";
    }

    public int getReverse(int playlistId) throws SQLException {
        String sql = """
        SELECT rev FROM playlists WHERE id = ?;
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("rev");
                }
            }
        }
        return 0;
    }

    public String getPlaylistName(int playlistId) throws SQLException {
        String sql = """
                SELECT name FROM playlists WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        }
        return "";
    }

    public void deleteSongFromAllPlaylists(int songId) throws SQLException {
        String sql = "DELETE FROM playlist_songs WHERE song_id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, songId);
            ps.executeUpdate();
        }
    }

    public void deleteSongFromPlaylist(int songId, int playlistId) throws SQLException {
        String sql = "DELETE FROM playlist_songs WHERE song_id = ? AND playlist_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, songId);
            ps.setInt(2, playlistId);
            ps.executeUpdate();
        }
    }

}
