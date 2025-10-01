package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LibraryController {

    @FXML
    private GridPane songGrid;

    @FXML
    private TextField searchField;

    List<Track> prevFiltered;

    private final int COLUMN_COUNT = 3; 
    private final double CARD_WIDTH = 120;
    private final double CARD_HEIGHT = 150;

    private List<Track> allTracks = new ArrayList<>(); // full list
    PlaybackService playbackService = NowPlayingController.getPlaybackService();

    @FXML
    public void initialize() {
        File musicDir = new File("C:/music");
        loadSongsFromDirectory(musicDir);

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTracks(newVal));
    }

    private void loadSongsFromDirectory(File dir) {
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> {
            String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase();
            return ext.equals("mp3") || ext.equals("wav");
        });

        if (files == null) return;

        allTracks.clear();
        for (File f : files) allTracks.add(new Track(f.getAbsolutePath()));

        // set playlist in PlaybackService
        playbackService.setPlaylist(allTracks);

        refreshGrid(allTracks);
    }

    private void filterTracks(String query) {
        if (query == null || query.isEmpty()) {
            refreshGrid(allTracks);
            return;
        }

        String lowerQuery = query.toLowerCase();

        List<Track> filtered = allTracks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(lowerQuery)
                        || (t.getArtist() != null && t.getArtist().toLowerCase().contains(lowerQuery))
                        || (t.getAlbum() != null && t.getAlbum().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());
        if(filtered.equals(prevFiltered)){
            return;
        }
        prevFiltered = filtered;
        refreshGrid(filtered);
    }

    private void refreshGrid(List<Track> tracks) {
        songGrid.getChildren().clear();

        int col = 0, row = 0;
        for (Track track : tracks) {
            AnchorPane card = createCard(track);
            songGrid.add(card, col, row);

            col++;
            if (col >= COLUMN_COUNT) {
                col = 0;
                row++;
            }
        }
    }

    private AnchorPane createCard(Track track) {
        AnchorPane card = new AnchorPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setStyle("-fx-border-color: gray; -fx-background-color: #f0f0f0;");

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

        card.setOnMouseClicked(e -> playbackService.play(allTracks.indexOf(track)));

        return card;
    }
}
