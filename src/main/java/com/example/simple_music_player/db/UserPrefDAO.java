package com.example.simple_music_player.db;

import com.example.simple_music_player.Controller.LibraryController;
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
        INSERT OR REPLACE INTO user_pref 
        (id, playlistNo, timestamp, status, repeat, shuffle, isRundown, volume, playlistId)
        VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)
    """;

        LibraryController libraryController = LibraryController.getInstance();
        if(UserPref.playlistId == 0) {
            libraryController.setCurrentPlaylistId(2);
            UserPref.playlistNo = getPlaylistNo();
            UserPref.timestamp = getTimeStamp();
            UserPref.status = getUserStatus();
            UserPref.repeat = getRepeat();
            UserPref.shuffle = getShuffle();
            UserPref.isRundown = getIsRundown();
            UserPref.volume = getVolume();
        }
        if(UserPref.volume == 0) UserPref.volume = 1;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            // 1 -> playlistNo
            ps.setInt(1, UserPref.playlistNo);

            // 2 -> timestamp
            ps.setLong(2, UserPref.timestamp);

            // 3 -> status
            if ((getUserStatus() == null || getUserStatus().isEmpty())
                && (UserPref.status == null || UserPref.status.isEmpty())) {
                UserPref.status = "Play";
            }
            ps.setString(3, UserPref.status != null ? UserPref.status : getUserStatus());

            // 4 -> repeat
            ps.setInt(4, UserPref.repeat);

            // 5 -> shuffle
            ps.setInt(5, UserPref.shuffle);

            // 6 -> isRundown
            ps.setInt(6, UserPref.isRundown);

            // 7 -> volume
            ps.setDouble(7, UserPref.volume);

            // 8 -> playlistId
            ps.setInt(8, libraryController.getCurrentPlaylistId());

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

    public double getVolume() throws SQLException {
        String sql = """
                    SELECT volume FROM user_pref
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getDouble("volume");
            }
        }
        return -1;
    }

    public int getPlaylistId() throws SQLException {
        String sql = """
                SELECT playlistId FROM user_pref
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
            if(rs.next()) {
                return rs.getInt("playlistId");
            }
        }
        System.out.println("No playlist id found in the table");
        return -1;
    }

}
