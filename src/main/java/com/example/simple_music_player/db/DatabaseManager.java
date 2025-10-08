package com.example.simple_music_player.db;

import java.io.File;
import java.sql.*;

public class DatabaseManager {

    private static final String DB_FILE_NAME = "library.db";
    private static final String DB_FOLDER = ".myplayer";  // will be inside user.home
    private static Connection connection;

    /**
     * Initializes the SQLite database connection and creates tables if not existing.
     */
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

    /**
     * Creates required tables if they do not exist.
     */
    private static void createTables() throws SQLException {
        String createSongsTable = """
                    --DROP TABLE IF EXISTS songs;
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
                        compressed_artwork BLOB
                    );
                CREATE INDEX IF NOT EXISTS idx_songs_title ON songs(title);
                CREATE INDEX IF NOT EXISTS idx_songs_artist ON songs(
                            artist);
                CREATE INDEX IF NOT EXISTS idx_songs_album ON songs(album);
                """;
        //BLOB = binary large object //here used for images
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createSongsTable);
        }
    }


    /**
     * Returns the shared database connection.
     */
    public static Connection getConnection() {
        if (connection == null) {
            initialize();
        }
        return connection;
    }

    /**
     * Closes the database connection gracefully on app exit.
     */
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
