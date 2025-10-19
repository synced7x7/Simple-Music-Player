package com.example.simple_music_player.db;

import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private static final String DB_FILE_NAME = "library.db";
    private static final String DB_FOLDER = ".myplayer";  // will be inside user.home
    private static Connection connection;


    public static void initialize() {
        try {
            // Determine database path inside user's home directory
            String userHome = System.getProperty("user.home");
            File appDir = new File(userHome, DB_FOLDER);
            if (!appDir.exists()) appDir.mkdirs();

            File dbFile = new File(appDir, DB_FILE_NAME);
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

            connection = DriverManager.getConnection(url);
            if (connection != null) {
                System.out.println("✅ Connected to SQLite at: " + dbFile.getAbsolutePath());
                createTables();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Failed to initialize SQLite database.");
        }
    }

    private static void createTables() throws SQLException {
        String createSongsTable = """
                    --DROP TABLE if EXISTS songs;
                    CREATE TABLE IF NOT EXISTS songs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        path TEXT UNIQUE NOT NULL,
                        title TEXT,
                        artist TEXT,
                        album TEXT,
                        genre TEXT,
                        year TEXT,
                        format TEXT,
                        bitrate TEXT,
                        sampleRate TEXT,
                        channels TEXT,
                        length TEXT,
                        artwork BLOB,
                        compressed_artwork BLOB,
                        date_added TEXT
                    );
                """;

        String createIndexTitle = "CREATE INDEX IF NOT EXISTS idx_songs_title ON songs(title);";
        String createIndexArtist = "CREATE INDEX IF NOT EXISTS idx_songs_artist ON songs(artist);";
        String createIndexAlbum = "CREATE INDEX IF NOT EXISTS idx_songs_album ON songs(album);";

        String createUserPrefTable = """
                    CREATE TABLE IF NOT EXISTS user_pref (
                        id INTEGER PRIMARY KEY,
                        playlistNo INTEGER,
                        timestamp BIGINT,
                        status VARCHAR,
                        sortingPref VARCHAR,
                        reverse INTEGER,
                        repeat INTEGER,
                        shuffle INTEGER,
                        isRundown INTEGER,
                        volume DOUBLE,
                        playlistId INTEGER
                    );
                """;

        String createPlaylistsTable = """
                    CREATE TABLE IF NOT EXISTS playlists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name VARCHAR NOT NULL
                    );
                """;

        String createPlaylistSongsTable = """
                    CREATE TABLE IF NOT EXISTS playlist_songs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        playlist_id INTEGER NOT NULL,
                        song_id INTEGER NOT NULL,
                        FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE,
                        FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE
                    );
                """;


        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSongsTable);
            stmt.execute(createIndexTitle);
            stmt.execute(createIndexArtist);
            stmt.execute(createIndexAlbum);
            stmt.execute(createUserPrefTable);
            stmt.execute(createPlaylistsTable);
            stmt.execute(createPlaylistSongsTable);
        }
    }

    public static Connection getConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    public static void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔒 SQLite connection closed.");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
