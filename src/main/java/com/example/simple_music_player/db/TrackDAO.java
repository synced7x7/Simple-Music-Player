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

    /**
     * Inserts a track into the database, ignores if path already exists.
     */
    public void updateTracks(Track track) {
        String sql = """
            INSERT OR IGNORE INTO songs
            (path, title, artist, album, genre, year, format, bitrate, sampleRate, channels, length, artwork)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            } else {
                ps.setNull(12, Types.BLOB);
            }
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
        return null;
    }



    /**
     * Retrieves all tracks (optionally with LIMIT/OFFSET for lazy loading).
     */
    public List<Track> getAllTracks(int limit, int offset) {
        List<Track> tracks = new ArrayList<>();
        String sql = "SELECT * FROM songs LIMIT ? OFFSET ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tracks.add(mapRowToTrack(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tracks;
    }

    /**
     * Searches tracks by keyword in title, artist, or album.
     */
    public List<Track> searchTracks(String keyword, int limit, int offset) {
        List<Track> tracks = new ArrayList<>();
        String sql = """
            SELECT * FROM songs
            WHERE title LIKE ? OR artist LIKE ? OR album LIKE ?
            LIMIT ? OFFSET ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setInt(4, limit);
            ps.setInt(5, offset);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tracks.add(mapRowToTrack(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tracks;
    }

    /**
     * Deletes a track by path.
     */
    public void deleteTrackByPath(String path) {
        String sql = "DELETE FROM songs WHERE path = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, path);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Maps a SQL row to a Track object.
     */
    private Track mapRowToTrack(ResultSet rs) throws SQLException {
        String path = rs.getString("path");

        // Reconstruct Track manually (without rereading file)
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
                rs.getBytes("artwork")
        );
    }

    // return all stored ids (ordered by title). Use for playlist building (lightweight)
    public List<Integer> getAllIds() {
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


    // get a single Track reconstructed from DB row (no re-parsing of audio file)
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

    public List<Track> getTracks(int limit, int offset) {
        List<Track> tracks = new ArrayList<>();

        String sql = "SELECT id, title, artist, album, length, path FROM songs LIMIT ? OFFSET ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            pstmt.setInt(2, offset);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Track t = new Track();
                    t.setId(rs.getInt("id"));
                    t.setTitle(rs.getString("title"));
                    t.setArtist(rs.getString("artist"));
                    t.setAlbum(rs.getString("album"));
                    t.setLength(String.valueOf(rs.getInt("length")));
                    t.setPath(rs.getString("path"));

                    tracks.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tracks;
    }


}
