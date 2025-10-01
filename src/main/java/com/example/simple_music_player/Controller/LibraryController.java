package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

public class LibraryController {

    @FXML
    private GridPane songGrid;

    private final int COLUMN_COUNT = 3; // 3 cards per row
    private final double CARD_WIDTH = 120;
    private final double CARD_HEIGHT = 150;

    PlaybackService playbackService = NowPlayingController.getPlaybackService();

    @FXML
    public void initialize() {
        File musicDir = new File("C:/music");
        loadSongsFromDirectory(musicDir);
    }

    public void loadSongsFromDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            return ext.equals("mp3") || ext.equals("wav");
        });

        if (files == null) return;

        List<Track> trackList = new ArrayList<>();
        int col = 0, row = 0;

        for (File f : files) {
            Track track = new Track(f.getAbsolutePath());
            trackList.add(track);

            // --- create card dynamically ---
            AnchorPane card = new AnchorPane();
            card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);

            ImageView cover = new ImageView();
            cover.setFitWidth(CARD_WIDTH);
            cover.setFitHeight(CARD_WIDTH);
            cover.setPreserveRatio(true);
            if (track.getCover() != null) cover.setImage(track.getCover());

            Label nameLabel = new Label(track.getTitle());
            nameLabel.setPrefWidth(CARD_WIDTH);
            nameLabel.setLayoutY(CARD_WIDTH + 5);
            nameLabel.setWrapText(true);

            card.getChildren().addAll(cover, nameLabel);

            songGrid.add(card, col, row);

            col++;
            if (col >= COLUMN_COUNT) {
                col = 0;
                row++;
            }
        }

        // set playlist once
        playbackService.setPlaylist(trackList);
    }
}

