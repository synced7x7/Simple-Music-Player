package com.example.simple_music_player.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPrefRealtimeDAO {
    private final Connection conn;

    public UserPrefRealtimeDAO(Connection connection) {
        this.conn = connection;
    }
    // --- Setters ---
    public void setIsHiddenLibrary(boolean isHidden) throws SQLException {
        String sql = """
                    INSERT OR REPLACE INTO user_pref_realtime (id, isHiddenLibrary, isHiddenAlbum)
                    VALUES (1, ?, COALESCE((SELECT isHiddenAlbum FROM user_pref_realtime WHERE id = 1), 0));
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isHidden ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public void setIsHiddenAlbum(boolean isHidden) throws SQLException {
        String sql = """
                    INSERT OR REPLACE INTO user_pref_realtime (id, isHiddenLibrary, isHiddenAlbum)
                    VALUES (1, COALESCE((SELECT isHiddenLibrary FROM user_pref_realtime WHERE id = 1), 0), ?);
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, isHidden ? 1 : 0);
            ps.executeUpdate();
        }
    }

    // --- Getters ---
    public boolean getIsHiddenLibrary() throws SQLException {
        String sql = "SELECT isHiddenLibrary FROM user_pref_realtime WHERE id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("isHiddenLibrary") != 0;
            }
        }
        return false;
    }

    public boolean getIsHiddenAlbum() throws SQLException {
        String sql = "SELECT isHiddenAlbum FROM user_pref_realtime WHERE id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("isHiddenAlbum") != 0;
            }
        }
        return false;
    }


}
