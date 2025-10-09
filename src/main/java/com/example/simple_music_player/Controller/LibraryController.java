package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.TrackDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LibraryController {

    @FXML private ListView<Integer> songListView;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortComboBox;
    @FXML private Button shuffleButton;
    @FXML private Button reverseButton;
    @FXML private Button directoryButton;
    @FXML private Label songCountLabel;

    private static final double CARD_WIDTH = 120;
    private static final double CARD_HEIGHT = 150;

    private final PlaybackService playbackService = NowPlayingController.getPlaybackService();
    private final TrackDAO trackDAO = new TrackDAO(DatabaseManager.getConnection());

    private File selectedDir;
    private final File defaultMusicDirOpener = new File("C:/Users/Asus/Music");
    private File[] prevFiles = null;
    private boolean dirChanged = false;

    private boolean ascending = true;
    private String prevCriteria = "Title";

    @FXML
    public void initialize() {
        //Load initial library from DB
        loadInitialDirectoryFromDatabase();

        if (trackDAO.getTrackPath() != null) {
            selectedDir = new File(trackDAO.getTrackPath());
        }

        //Setup directory chooser
        directoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Music Directory");
            directoryChooser.setInitialDirectory(
                    Objects.requireNonNullElse(selectedDir, defaultMusicDirOpener)
            );

            File newDir = directoryChooser.showDialog(directoryButton.getScene().getWindow());
            if (newDir == null) return;

            if (selectedDir == null || !selectedDir.equals(newDir)) {
                System.out.println("Directory changed: clearing old tracks");
                dirChanged = true;
                trackDAO.deleteAllTracks();
                playbackService.clearList();
            }

            selectedDir = newDir;
            loadSongsFromDirectory(selectedDir);
        });

        // Setup search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTracks(newVal));

        // Sort ComboBox
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) sortLibrary(newVal);
        });

        // Shuffle & Reverse
        shuffleButton.setOnAction(e -> sortLibrary("Shuffle"));
        reverseButton.setOnAction(e -> sortLibrary("Reverse"));

        // ListView Cell Factory (virtualized cards)
        songListView.setCellFactory(list -> new ListCell<>() {
            private final AnchorPane card = new AnchorPane();
            private final ImageView cover = new ImageView();
            private final Label nameLabel = new Label();

            {
                card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
                card.setStyle("-fx-border-color: gray; -fx-background-color: #f0f0f0;");

                cover.setFitWidth(CARD_WIDTH);
                cover.setFitHeight(CARD_WIDTH);
                cover.setPreserveRatio(true);

                nameLabel.setPrefWidth(CARD_WIDTH);
                nameLabel.setLayoutY(CARD_WIDTH + 5);
                nameLabel.setWrapText(true);

                card.getChildren().addAll(cover, nameLabel);
            }

            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty); //Old songs are cleared automatically

                if (empty || id == null) {
                    setGraphic(null);
                } else {
                    Track track = trackDAO.getTrackCompressedArtworkAndTitleById(id);
                    nameLabel.setText(track.getTitle());
                    cover.setImage(null);

                    // Async thumbnail load
                    CompletableFuture
                            .supplyAsync(() -> new Image(new ByteArrayInputStream(track.getCompressedArtworkData())))
                            .thenAccept(thumbnail -> {
                                if (thumbnail != null) {
                                    Platform.runLater(() -> cover.setImage(thumbnail));
                                }
                            });

                    card.setOnMouseClicked(e -> {
                        LibraryController.restartFromStart = false;
                        int index = PlaybackService.playlist.indexOf(id);
                        if (index != -1) playbackService.play(index);
                    });

                    setGraphic(card);
                }
            }
        });
    }

    // --- Sorting ---
    private void sortLibrary(String criteria) {
        if (!criteria.equals("Shuffle") && !criteria.equals("Reverse") && prevCriteria.equals(criteria)) {
            return;
        }

        restartFromStart = true;

        new Thread(() -> {
            List<Integer> sortedIds;
            switch (criteria) {
                case "Shuffle" -> {
                    sortedIds = new ArrayList<>(PlaybackService.getPlaylist());
                    Collections.shuffle(sortedIds);
                }
                case "Reverse" -> {
                    sortedIds = new ArrayList<>(PlaybackService.getPlaylist());
                    Collections.reverse(sortedIds);
                }
                default -> sortedIds = trackDAO.getAllIdsSorted(criteria, ascending);
            }
            prevCriteria = criteria;

            Platform.runLater(() -> {
                playbackService.setPlaylist(sortedIds, false);
                songListView.getItems().setAll(sortedIds);
            });
        }).start();
    }

    // --- Initial load ---
    private void loadInitialDirectoryFromDatabase() {
        List<Integer> allIds = trackDAO.getAllIds();
        if (trackDAO.getTrackPath() != null) {
            File dir = new File(trackDAO.getTrackPath());
            prevFiles = dir.listFiles(this::isAudioFile);
        }

        Platform.runLater(() -> {
            countSongs(allIds.size());
            songListView.getItems().setAll(allIds);
            playbackService.setPlaylist(allIds, true);
        });
    }

    // --- Directory Load ---
    private void loadSongsFromDirectory(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles(this::isAudioFile);
        if (Arrays.equals(prevFiles, files)) {
            System.out.println("No songs changed in the directory");
            return;
        }
        prevFiles = files;

        if (files == null || files.length == 0) {
            playbackService.setPlaylist(Collections.emptyList(), true);
            songListView.getItems().clear();
            return;
        }

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
            Platform.runLater(() -> {
                countSongs(allIds.size());
                playbackService.setPlaylist(allIds, dirChanged);
                songListView.getItems().setAll(allIds);
                dirChanged = false;
            });
        });
    }

    private void countSongs(int count) {
        songCountLabel.setText(count + " songs");
    }

    // --- Search ---
    private void filterTracks(String query) {
        String sortBy = sortComboBox.getValue() != null ? sortComboBox.getValue() : "Title";
        if (sortBy.equals("Date Added")) sortBy = "date_added";

        String finalSortBy = sortBy;
        CompletableFuture.runAsync(() -> {
            List<Integer> filteredIds = trackDAO.searchTrackIds(query, finalSortBy, ascending);
            Platform.runLater(() -> songListView.getItems().setAll(filteredIds));
        });
    }

    private boolean isAudioFile(File d, String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1) return false;
        String ext = name.substring(dotIndex + 1).toLowerCase();
        return ext.equals("mp3") || ext.equals("wav");
    }

    public static boolean restartFromStart = false;
}
