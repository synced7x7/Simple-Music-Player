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

public class LibraryController {

    @FXML
    private GridPane songGrid;

    private final int COLUMN_COUNT = 2; // how many columns per row

    PlaybackService playbackService = NowPlayingController.getPlaybackService();

    public void loadSongsFromDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            return ext.equals("mp3") || ext.equals("wav"); // add more later
        });

        if (files == null) return;

        int col = 0;
        int row = 0;

        try {
            for (File f : files) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/simple_music_player/song_card.fxml"));
                AnchorPane card = loader.load();

                ImageView cover = (ImageView) card.lookup("#coverImage");
                Label songName = (Label) card.lookup("#songNameLabel");

                playbackService.currentTrackProperty().addListener((obs, oldT, newT) -> {
                    //Name
                    songName.setText(newT.getTitle());
                    if (newT.getCover() != null) {
                        cover.setImage(newT.getCover());
                    } else {
                        cover.setImage(null);
                    }
                });

                songGrid.add(card, col, row);

                col++;
                if (col >= COLUMN_COUNT) {
                    col = 0;
                    row++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
