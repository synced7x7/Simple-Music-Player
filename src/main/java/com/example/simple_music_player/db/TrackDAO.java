package com.example.simple_music_player.db;

import com.example.simple_music_player.Model.Track;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class TrackDAO {

    private final Connection conn;

    public TrackDAO(Connection connection) {
        this.conn = connection;
    }

    public void updateTracks(Track track) {
        String sql = """
            INSERT OR IGNORE INTO songs
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
            e.printStackTrace();
        }
    }


    public void deleteAllTracks() {
        String sql = "DELETE FROM songs";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
            System.out.println("Deletion of all songs from the database is successful.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateTrackPath(int trackId, String newPath) throws SQLException {
        String sql = "UPDATE songs SET path = ? WHERE id = ?;";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newPath);
            ps.setInt(2, trackId);
            int updated = ps.executeUpdate();
            if (updated == 0) {
                System.out.println("No track found with ID: " + trackId);
            } else {
                System.out.println("Track path updated for ID: " + trackId);
            }
        }
    }

    public String getTrackPath() {
        String sql = "SELECT path FROM songs LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String fullPath = rs.getString("path");
                if (fullPath != null) {
                    File file = new File(fullPath);
                    String parentDir = file.getParent();
                    // Normalize to forward slashes
                    return parentDir.replace("\\", "/");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "";
    }

    public String getFileLocationById(int id) throws SQLException {
        String path = null;
        String sql = "SELECT path FROM songs WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    path = rs.getString("path");
                }
            }
        }
        return path;
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

    public List<Integer> getAllIdsSortByDefault() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM songs ORDER BY title";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ids;
    }

    public Track getTrackById(Integer id) {
        String sql = "SELECT * FROM songs WHERE id = ? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRowToTrack(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Track getTrackCompressedArtworkAndTitleById(Integer id) {
        String sql = "SELECT title, compressed_artwork FROM songs WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Track t = new Track();
                    t.setTitle(rs.getString("title"));
                    t.setCompressedArtworkData(rs.getBytes("compressed_artwork"));
                    return t;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Integer> getAllIdsSorted(int playlistId, String criteria, boolean ascending) {
        List<Integer> ids = new ArrayList<>();

        if (criteria.equals("Date Added"))
            ascending = !ascending;  // reverse for date added if that's your behavior

        String order = ascending ? "ASC" : "DESC";

        // Determine the sorting column in songs table
        String orderByColumn = switch (criteria) {
            case "Title" -> "s.title";
            case "Artist" -> "s.artist";
            case "Album" -> "s.album";
            case "Length" -> "CAST(s.length AS INTEGER)";
            case "Date Added" -> "s.date_added";
            default -> "s.title"; // fallback
        };

        String sql = """
        SELECT s.id
        FROM playlist_songs ps
        JOIN songs s ON ps.song_id = s.id
        WHERE ps.playlist_id = ?
        ORDER BY %s %s
    """.formatted(orderByColumn, order);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, playlistId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ids;
    }



    public List<Integer> searchTrackIds(int playlistId, String query, String sortBy, boolean ascending) {
        List<Integer> ids = new ArrayList<>();
        if (query == null || query.isEmpty()) {
            return getAllIdsSorted(playlistId, sortBy, ascending);
        }

        String order = ascending ? "ASC" : "DESC";

        String sql = """
        SELECT s.id
        FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.song_id
        WHERE ps.playlist_id = ?
          AND (LOWER(s.title) LIKE ? OR LOWER(s.artist) LIKE ? OR LOWER(s.album) LIKE ?)
        ORDER BY %s %s
    """.formatted(sortBy, order);

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String likeQuery = "%" + query.toLowerCase() + "%";
            ps.setInt(1, playlistId);
            ps.setString(2, likeQuery);
            ps.setString(3, likeQuery);
            ps.setString(4, likeQuery);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ids;
    }

    public void removeFromLibrary(int id) throws SQLException {
        String sql = """
                DELETE FROM songs WHERE id = ?
                """;
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

}
