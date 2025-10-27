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

    public void upsertFileTimestamp(long lastModified) throws SQLException {
        String sql = """
        INSERT OR REPLACE INTO misc (id, last_modified)
        VALUES (1, ?);
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, lastModified);
            ps.executeUpdate();
        }
    }


    public boolean isFileModified(long currentLastModified) throws SQLException {
        String sql = "SELECT last_modified FROM misc WHERE id = 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long stored = rs.getLong("last_modified");
                    System.out.println("current last modified: " + currentLastModified + " || lastModified: " + stored);
                    return stored != currentLastModified;
                }
            }
        }
        return true;
    }


}
