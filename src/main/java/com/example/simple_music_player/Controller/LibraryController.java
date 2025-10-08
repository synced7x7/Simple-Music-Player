package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Utility.ThumbnailCaching;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.TrackDAO;
import javafx.application.Platform;
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    private static final int PAGE_SIZE = 50;
    private final int COLUMN_COUNT = 3;
    public static final double CARD_WIDTH = 120;
    public static final double CARD_HEIGHT = 150;

    //JavaFX controls like ListView, TableView, ComboBox, etc. are designed to work with ObservableList.
    PlaybackService playbackService = NowPlayingController.getPlaybackService();
    public static boolean restartFromStart = false;
    File selectedDir;
    private final ThumbnailCaching thumbnailCaching = new ThumbnailCaching();
    private final TrackDAO trackDAO = new TrackDAO(DatabaseManager.getConnection());
    //Default Directory Path
    private final File defaultMusicDirOpener = new File("C:/Users/Asus/Music");
    private File prevDir = null;
    private boolean dirChanged = false;
    private File[] prevFiles = null;

    @FXML
    public void initialize() {
        // Load default directory from DB (sorted by title)
        loadInitialDirectoryFromDatabase();
        if (trackDAO.getTrackPath() != null) {
            selectedDir = new File(trackDAO.getTrackPath());
        }

        directoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Music Directory");
            directoryChooser.setInitialDirectory(
                    Objects.requireNonNullElse(selectedDir, defaultMusicDirOpener)
            );

            File newDir = directoryChooser.showDialog(directoryButton.getScene().getWindow());
            if (newDir == null) {
                return;
            }

            if (selectedDir == null || !selectedDir.equals(newDir)) {
                System.out.println("Directory changed: clearing old tracks");
                dirChanged = true;
                trackDAO.deleteAllTracks();
                playbackService.clearList();
            }

            selectedDir = newDir;
            loadSongsFromDirectory(selectedDir);
            prevDir = selectedDir;
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

    private boolean ascending = true;
    private String prevCriteria = "Title";

    private void sortLibrary(String criteria) {
       
        if (!criteria.equals("Shuffle") && !criteria.equals("Reverse") && prevCriteria.equals(criteria)) {
            return;
        }

        restartFromStart = true;

        new Thread(() -> {
            List<Integer> sortedIds;

            switch (criteria) {
                case "Shuffle":
                    sortedIds = PlaybackService.getPlaylist();
                    Collections.shuffle(sortedIds);
                    prevCriteria = "Shuffle";
                    break;

                case "Reverse":
                    sortedIds = PlaybackService.getPlaylist();
                    Collections.reverse(sortedIds);
                    prevCriteria = "Reverse";
                    break;

                default:
                    sortedIds = trackDAO.getAllIdsSorted(criteria, ascending);
                    prevCriteria = criteria;
                    break;
            }

            Platform.runLater(() -> {
                playbackService.setPlaylist(sortedIds, false);
                refreshGrid(sortedIds);
            });
        }).start();
    }



    private void loadInitialDirectoryFromDatabase() {
        List<Integer> allIds = trackDAO.getAllIds();

        if (trackDAO.getTrackPath() != null) {
            File dir = new File(trackDAO.getTrackPath());
            prevFiles = dir.listFiles((d, name) -> {
                int dotIndex = name.lastIndexOf('.');
                if (dotIndex == -1) return false; // no extension
                String ext = name.substring(dotIndex + 1).toLowerCase();
                return ext.equals("mp3") || ext.equals("wav");
            });
        }

        Platform.runLater(() -> {
            refreshGrid(allIds);
            playbackService.setPlaylist(allIds, true);
        });
    }


    private void loadSongsFromDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;


        // Filter only audio files safely
        File[] files = dir.listFiles((d, name) -> {
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex == -1) return false; // no extension
            String ext = name.substring(dotIndex + 1).toLowerCase();
            return ext.equals("mp3") || ext.equals("wav");
        });
        if (prevFiles != null && Arrays.equals(prevFiles, files)) {
            System.out.println("No songs changed in the directory");
            return;
        }
        prevFiles = files;

        if (files == null || files.length == 0) {
            System.out.println("No songs in the directory");
            playbackService.setPlaylist(Collections.emptyList(), true);
            return;
        }

        // Heavy work off the FX thread (database + metadata)
        CompletableFuture.runAsync(() -> {
            for (File f : files) {
                try {
                    Track t = new Track(f.getAbsolutePath());
                    trackDAO.updateTracks(t);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            List<Integer> allIds = trackDAO.getAllIds();
            System.out.println("Directory changed = " + dirChanged);
            Platform.runLater(() -> {
                if (dirChanged) {
                    playbackService.setPlaylist(allIds, true);
                    refreshGrid(allIds);
                    dirChanged = false;
                } else
                    playbackService.setPlaylist(allIds, false);
            });
        });
    }

    private void filterTracks(String query) {
        // Get current sort criteria from comboBox
        String sortBy = sortComboBox.getValue() != null ? sortComboBox.getValue() : "title";
        boolean ascending = true; // or maintain your ascending flag

        CompletableFuture.runAsync(() -> {
            List<Integer> filteredIds = trackDAO.searchTrackIds(query, sortBy, ascending);

            Platform.runLater(() -> {
                refreshGrid(filteredIds);
            });
        });
    }

    private void refreshGrid(List<Integer> allIds) {
        songGrid.getChildren().clear();
        int col = 0, row = 0;
        for (Integer id : allIds) {
            AnchorPane card = createCard(id);
            songGrid.add(card, col, row);

            col++;
            if (col >= COLUMN_COUNT) {
                col = 0;
                row++;
            }
        }
    }

    private AnchorPane createCard(Integer id) {
        AnchorPane card = new AnchorPane();
        card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        card.setStyle("-fx-border-color: gray; -fx-background-color: #f0f0f0;");

        // --- Cover Image ---
        ImageView cover = new ImageView();
        cover.setFitWidth(CARD_WIDTH);
        cover.setFitHeight(CARD_WIDTH);
        cover.setPreserveRatio(true);

        Track track = trackDAO.getTrackArtworkAndTitleById(id);
        /*byte[] artworkData = track.getCompressedArtworkData();
        Image thumbnail = null;
        if (artworkData != null) {
            thumbnail = new Image(new ByteArrayInputStream(artworkData));
        }*/
        Image thumbnail = thumbnailCaching.loadThumbnail(id, track);
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
            int index = PlaybackService.playlist.indexOf(id);
            System.out.println("Id:" + id + " Corresponding index" + index);
            if (index != -1) {
                playbackService.play(index);
            }
        });

        return card;
    }

}
