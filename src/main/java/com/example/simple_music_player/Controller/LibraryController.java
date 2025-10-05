package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Utility.thumbnailCaching;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class LibraryController {

    @FXML
    private GridPane songGrid;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private Button shuffleButton;
    @FXML
    private Button reverseButton;
    @FXML
    private Button directoryButton;


    private final int COLUMN_COUNT = 3;
    public static final double CARD_WIDTH = 120;
    public static final double CARD_HEIGHT = 150;

    private static final ObservableList<Track> allTracks = FXCollections.observableArrayList(); //Special kind of list that notifies listeners when its content changes (add, remove, update, sort).
    //JavaFX controls like ListView, TableView, ComboBox, etc. are designed to work with ObservableList.
    PlaybackService playbackService = NowPlayingController.getPlaybackService();
    public static boolean restartFromStart = false;
    File selectedDir;
    private thumbnailCaching optimization = new thumbnailCaching();

    @FXML
    public void initialize() {

        File musicDir = new File("C:/music");
        loadSongsFromDirectory(musicDir);

        directoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Music Directory");

            //previous/default directory
            directoryChooser.setInitialDirectory(Objects.requireNonNullElse(selectedDir, musicDir));
            selectedDir = directoryChooser.showDialog(directoryButton.getScene().getWindow());
            if (selectedDir != null) {
                loadSongsFromDirectory(selectedDir);
            }
        });

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTracks(newVal));
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                sortLibrary(newVal);
            }
        });
        shuffleButton.setOnMouseClicked(e -> {
            sortLibrary("Shuffle");
        });
        reverseButton.setOnMouseClicked(e -> {
            sortLibrary("Reverse");
        });
    }

    //All Tracks changing
    private void sortLibrary(String criteria) {
        Comparator<Track> comparator;
        restartFromStart = true;
        switch (criteria) {
            case "Title":
                comparator = Comparator.comparing(Track::getTitle, String.CASE_INSENSITIVE_ORDER);
                break;
            case "Artist":
                comparator = Comparator.comparing(Track::getArtist, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Album":
                comparator = Comparator.comparing(Track::getAlbum, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
                break;
            case "Duration":
                comparator = Comparator.comparingInt(t -> Integer.parseInt(t.getLength()));
                break;
            case "Date Added":
                comparator = Comparator.comparing(t -> new File(t.getPath()).lastModified());
                break;
            case "Shuffle":
                FXCollections.shuffle(allTracks);
                refreshGrid(allTracks);
                return;
            case "Reverse":
                FXCollections.reverse(allTracks);
                refreshGrid(allTracks);
                return;
            default:
                return;
        }
        FXCollections.sort(allTracks, comparator);
        refreshGrid(allTracks);
    }

    //All tracks changing
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
        playbackService.setPlaylist(allTracks, true);

        refreshGrid(allTracks);
    }

    private void filterTracks(String query) {
        restartFromStart = true;
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

        refreshGrid(filtered);
    }

    private void refreshGrid(List<Track> tracks) {
        songGrid.getChildren().clear();

        // set visible playlist as the one in playback service
        playbackService.setPlaylist(tracks, false);

        int col = 0, row = 0;
        for (Track track : tracks) {
            AnchorPane card = createCard(track, tracks);
            songGrid.add(card, col, row);

            col++;
            if (col >= COLUMN_COUNT) {
                col = 0;
                row++;
            }
        }
    }

    private AnchorPane createCard(Track track, List<Track> visibleList) {
        AnchorPane card = new AnchorPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setStyle("-fx-border-color: gray; -fx-background-color: #f0f0f0;");

        // --- Cover Image ---
        ImageView cover = new ImageView();
        cover.setFitWidth(CARD_WIDTH);
        cover.setFitHeight(CARD_WIDTH);
        cover.setPreserveRatio(true);

        // Load thumbnail (lazy)
        Image thumbnail = optimization.loadThumbnail(track);
        if (thumbnail != null) {
            cover.setImage(thumbnail);
        }

        // --- Title ---
        Label nameLabel = new Label(track.getTitle());
        nameLabel.setPrefWidth(CARD_WIDTH);
        nameLabel.setLayoutY(CARD_WIDTH + 5);
        nameLabel.setWrapText(true);

        card.getChildren().addAll(cover, nameLabel);

        // --- Click Handler ---
        card.setOnMouseClicked(e -> {
            restartFromStart = false;
            playbackService.play(visibleList.indexOf(track));
        });

        return card;
    }
}
