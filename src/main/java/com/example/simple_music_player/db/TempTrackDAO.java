package com.example.simple_music_player.db;

import com.example.simple_music_player.Model.Track;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TempTrackDAO {
    private final Connection conn;

    public TempTrackDAO(Connection connection) {
        this.conn = connection;
    }

    public void insertIntoTempSongs(Track track) {
        String sql = """
                    INSERT OR IGNORE INTO temp_songs
                    (path, title, artist, album, genre, year, format, bitrate, sampleRate, channels, length, artwork, compressed_artwork, date_added)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, track.getPath());
            ps.setString(2, track.getTitle());
            ps.setString(3, track.getArtist());
            ps.setString(4, track.getAlbum());
            ps.setString(5, track.getGenre());
            ps.setString(6, track.getYear());
            ps.setString(7, track.getFormat());
            ps.setString(8, track.getBitrate());
            ps.setString(9, track.getSampleRate());
            ps.setString(10, track.getChannels());
            ps.setString(11, track.getLength());
            if (track.getArtworkData() != null) {
                ps.setBytes(12, track.getArtworkData());
                ps.setBytes(13, track.getCompressedArtworkData());
            } else {
                ps.setNull(12, Types.BLOB);
                ps.setNull(13, Types.BLOB);
            }
            ps.setString(14, track.getDateAdded());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Track getTrackById(int id) {
        String sql = "SELECT * FROM temp_songs WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToTrack(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Integer> getAllIdsSortByDefault() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM temp_songs ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return ids;
    }

    private Track mapRowToTrack(ResultSet rs) throws SQLException {
        String path = rs.getString("path");

        return new Track(
                path,
                rs.getString("title"),
                rs.getString("artist"),
                rs.getString("album"),
                rs.getString("genre"),
                rs.getString("year"),
                rs.getString("format"),
                rs.getString("bitrate"),
                rs.getString("sampleRate"),
                rs.getString("channels"),
                rs.getString("length"),
                rs.getBytes("artwork"),
                rs.getString("date_added")
        );
    }

    public void deleteSongsFromTempTable() {
        String sql = "DELETE FROM temp_songs;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
