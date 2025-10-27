package com.example.simple_music_player.Utility;

import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.TrackDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;

public class SongDetailsUtility {
    private final TrackDAO trackDAO = new TrackDAO(DatabaseManager.getConnection());

    public void openSongDetails(int songId) {
        PlaybackService playbackService = NowPlayingController.getPlaybackService();
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("🎵 Song Details");

        Track t = trackDAO.getTrackById(songId);
        if (t == null) {
            System.err.println("No track found for id " + songId);
            return;
        }

        // --- Image ---
        ImageView imageView = new ImageView(t.getCover());
        imageView.setFitWidth(400);
        imageView.setFitHeight(400);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0.3, 0, 4);");

        // --- Info Section ---
        Label title = styledLabel("Title: " + safe(t.getTitle()), true);
        Label artist = styledLabel("Artist: " + safe(t.getArtist()), false);
        Label album = styledLabel("Album: " + safe(t.getAlbum()), false);
        Label genre = styledLabel("Genre: " + safe(t.getGenre()), false);
        Label year = styledLabel("Year: " + safe(t.getYear()), false);
        Label format = styledLabel("Format: " + safe(t.getFormat()), false);
        Label bitrate = styledLabel("Bitrate: " + safe(t.getBitrate()), false);
        Label sampleRate = styledLabel("Sample Rate: " + safe(t.getSampleRate()), false);
        Label channels = styledLabel("Channels: " + safe(t.getChannels()), false);
        Label duration = styledLabel("Duration: " + playbackService.formatTime(Integer.parseInt(t.getLength())), false);
        Label dateAdded = styledLabel("Added: " + safe(t.getDateAdded()), false);

        // --- Clickable Path Label ---
        Label path = styledLabel("Path: " + safe(t.getPath()), false);
        path.setWrapText(true);
        path.setMaxWidth(380);
        path.setTextFill(Color.LIGHTBLUE);
        path.setCursor(Cursor.HAND);
        path.setOnMouseEntered(e -> path.setStyle("-fx-font-size: 14px; -fx-underline: true; -fx-text-fill: cyan;"));
        path.setOnMouseExited(e -> path.setStyle("-fx-font-size: 14px; -fx-text-fill: lightblue;"));
        path.setOnMouseClicked(e -> {
            if (t.getPath() != null && !t.getPath().isEmpty()) {
                try {
                    File file = new File(t.getPath());
                    if (file.exists()) {
                        Runtime.getRuntime().exec("explorer /select,\"" + file.getAbsolutePath() + "\"");
                    } else {
                        System.err.println("File not found: " + t.getPath());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        VBox infoBox = new VBox(6, title, artist, album, genre, year, format,
                bitrate, sampleRate, channels, duration, dateAdded, path);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.setPadding(new Insets(10, 20, 20, 20));

        // --- Combine ---
        VBox root = new VBox(15, imageView, infoBox);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_CENTER);
        root.setStyle("""
            -fx-background-color: linear-gradient(to bottom right, #1a1a1a, #2e2e2e);
            -fx-background-radius: 15;
            -fx-border-radius: 15;
            -fx-border-color: #444;
            -fx-border-width: 1;
        """);

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.sizeToScene();
        stage.show();
    }

    private Label styledLabel(String text, boolean isTitle) {
        Label lbl = new Label(text);
        lbl.setTextFill(Color.WHITE);
        lbl.setWrapText(true);
        lbl.setStyle(isTitle
                ? "-fx-font-size: 18px; -fx-font-weight: bold;"
                : "-fx-font-size: 14px; -fx-opacity: 0.85;");
        return lbl;
    }

    private String safe(String s) {
        return s == null ? "Unknown" : s;
    }
}


