package com.example.simple_music_player.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MiscDAO {
    private final Connection conn;

    public MiscDAO(Connection connection) {
        this.conn = connection;
    }

    public void upsertFileTimestamp(String path, long lastModified) throws SQLException {
        String sql = """
        INSERT INTO misc(path, last_modified)
        VALUES (?, ?)
        ON CONFLICT(path) DO UPDATE SET last_modified=excluded.last_modified;
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.setLong(2, lastModified);
            ps.executeUpdate();
        }
    }

    public boolean isFileModified(String path, long currentLastModified) throws SQLException {
        String sql = "SELECT last_modified FROM misc WHERE path = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long stored = rs.getLong("last_modified");
                    return stored != currentLastModified;
                }
            }
        }
        return true;
    }


}
