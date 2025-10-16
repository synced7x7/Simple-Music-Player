package com.example.simple_music_player.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

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

    public void insertSongsInPlaylist(int playlistId, int songId) throws SQLException {
        String insertSongs = """
            INSERT INTO playlist_songs (playlist_id, song_id)
            VALUES (?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(insertSongs)) {
            ps.setInt(1, playlistId);
            ps.setInt(2, songId);
            ps.executeUpdate();
        }
    }
    

}
