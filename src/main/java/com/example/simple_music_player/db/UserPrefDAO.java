package com.example.simple_music_player.db;

import com.example.simple_music_player.Model.UserPref;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserPrefDAO {
    private final Connection conn;

    public UserPrefDAO(Connection connection) {
        this.conn = connection;
    }

    public void setUserPref() throws SQLException {
        String sql = """
                    INSERT OR REPLACE INTO user_pref (id, playlistNo, timestamp, status, sortingPref)
                    VALUES (1, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, UserPref.playlistNo);
            ps.setLong(2, UserPref.timestamp);
            if (getUserStatus().isEmpty() && UserPref.status == null) UserPref.status = "Play";
            if (UserPref.status != null)
                ps.setString(3, UserPref.status);
            else
                ps.setString(3, getUserStatus());
            ps.setString(4, UserPref.sortingPref);
            ps.executeUpdate();
        }
    }

    public String getUserStatus() throws SQLException {
        String sql = """
                    SELECT status  FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getString("status");
            }
        }
        return "";
    }

    public Long getTimeStamp() throws SQLException {
        String sql = """
                    SELECT timestamp FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getLong("timestamp");
            }
        }
        return 0L;
    }

    public int getPlaylistNo() throws SQLException {
        String sql = """
                    SELECT playlistNo FROM user_pref
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getInt("playlistNo");
            }
        }
        return 0;
    }
    
    public String getSortingPref() throws SQLException {
        String sql = """
                    SELECT sortingPref FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getString("sortingPref");
            }
        }
        return "";
    }

}
