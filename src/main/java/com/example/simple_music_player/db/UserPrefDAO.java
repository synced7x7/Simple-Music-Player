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
                    INSERT OR REPLACE INTO user_pref (id, playlistNo, timestamp, status, sortingPref, reverse, repeat, shuffle, isRundown)
                    VALUES (1, ?, ?, ?, ?, ?, ? ,?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, UserPref.playlistNo);
            ps.setLong(2, UserPref.timestamp);

            if ((getUserStatus() == null || getUserStatus().isEmpty() ) && (UserPref.status == null || UserPref.status.isEmpty())) UserPref.status = "Play";
            if (UserPref.status != null) ps.setString(3, UserPref.status);
            else ps.setString(3, getUserStatus());

            if ((getSortingPref() == null || getSortingPref().isEmpty()) && (UserPref.sortingPref == null || UserPref.sortingPref.isEmpty())) UserPref.sortingPref = "Title"; //1st time
            if (UserPref.sortingPref !=null) ps.setString(4, UserPref.sortingPref);
            else ps.setString(4, getSortingPref());

            if(UserPref.repeat != 0) ps.setInt(4, UserPref.repeat);

            ps.setInt(5, UserPref.reverse);
            ps.setInt(6, UserPref.repeat);
            ps.setInt(7, UserPref.shuffle);
            ps.setInt(8, UserPref.isRundown);

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

    public int getReverse() throws SQLException {
        String sql = """
                    SELECT reverse FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getInt("reverse");
            }
        }
        return 0;
    }

    public int getShuffle() throws SQLException {
        String sql = """
                    SELECT shuffle FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getInt("shuffle");
            }
        }
        return 0;
    }

    public int getRepeat() throws SQLException {
        String sql = """
                    SELECT repeat FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getInt("repeat");
            }
        }
        return 0;
    }

    public int getIsRundown() throws SQLException {
        String sql = """
                    SELECT isRundown FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getInt("isRundown");
            }
        }
        return 0;
    }

}
