package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.TrackDAO;
import com.example.simple_music_player.db.UserPrefDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LibraryController {

    @FXML
    private ListView<Integer> songListView;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;

    @FXML
    private Button reverseButton;
    @FXML
    private Button directoryButton;
    @FXML
    private Label songCountLabel;

    private static final double CARD_WIDTH = 120;
    private static final double CARD_HEIGHT = 150;

    private final PlaybackService playbackService = NowPlayingController.getPlaybackService();

    private final TrackDAO trackDAO = new TrackDAO(DatabaseManager.getConnection());

    private File selectedDir;
    private final File defaultMusicDirOpener = new File("C:/Users/Asus/Music");
    private File[] prevFiles = null;
    private boolean dirChanged = false;

    private boolean ascending = true;
    private String prevCriteria = "";

    private final UserPrefDAO userPrefDAO = new UserPrefDAO(DatabaseManager.getConnection());

    @FXML
    public void initialize() throws SQLException {
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
        if (!criteria.equals("Reverse") && prevCriteria.equals(criteria)) {
            System.out.println("prevCriteria equals Present criteria");
            return;
        }
        System.out.println("Criteria: " + criteria);

        restartFromStart = true;
        new Thread(() -> {
            int songId = -1;
            List<Integer> sortedIds;

            if (criteria.equals("Reverse")) {
                UserPref.reverse = (UserPref.reverse == 0) ? 1 : 0;
                sortedIds = new ArrayList<>(PlaybackService.getPlaylist());
                songId = PlaybackService.getPlaylist().get(playbackService.getCurrentIndex());
                System.out.println("Song id before sorting: " + songId + "|| Index: " + playbackService.getCurrentIndex() + "|| Reverse: " + UserPref.reverse);
                Collections.reverse(sortedIds);
            } else {
                UserPref.sortingPref = criteria;
                //
                songId = PlaybackService.getPlaylist().get(playbackService.getCurrentIndex());
                // System.out.println("Song id before sorting: " + songId + "|| Index: " + playbackService.getCurrentIndex());
                if(UserPref.reverse == 1) ascending = false;
                sortedIds = trackDAO.getAllIdsSorted(criteria, ascending);
            }

            if(!criteria.equals("Reverse"))
                prevCriteria = criteria;


            int finalSongId = songId; //safeguard mechanism of java so that multiple threads doesn't update the same variable
                                        // so either use temp or atomic when using variable in a thread.

            Platform.runLater(() -> {
                playbackService.setPlaylist(sortedIds, false);
                songListView.getItems().setAll(sortedIds);
                int idx = PlaybackService.getPlaylist().indexOf(finalSongId);
                playbackService.setCurrentIndex(idx);
                UserPref.playlistNo = idx;
                System.out.println("Song id after sorting: " + finalSongId + "|| Index: " + idx);
            });
        }).start();
    }

    // --- Initial load ---
    private void loadInitialDirectoryFromDatabase() throws SQLException {
        String sortingPref = userPrefDAO.getSortingPref();
        List<Integer> idsToLoad;
        int reverse = userPrefDAO.getReverse();
        UserPref.reverse = reverse;
        if(reverse==1) ascending = false;

        if (sortingPref != null && !sortingPref.isEmpty()) {
            // Fetch from DB in sorted order directly
            idsToLoad = trackDAO.getAllIdsSorted(sortingPref, ascending);
            System.out.println("Loaded sorted order based on: " + sortingPref);
        } else {
            // Default: order by title
            idsToLoad = trackDAO.getAllIds();
        }

        int idx = userPrefDAO.getPlaylistNo();
        String status = userPrefDAO.getUserStatus();
        long ts = userPrefDAO.getTimeStamp();


        if (trackDAO.getTrackPath() != null) {
            File dir = new File(trackDAO.getTrackPath());
            prevFiles = dir.listFiles(this::isAudioFile);
        }

        Platform.runLater(() -> {
            countSongs(idsToLoad.size());
            songListView.getItems().setAll(idsToLoad);
            playbackService.setPlaylist(idsToLoad, idx, status, ts);
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

            List<Integer> allIds = trackDAO.getAllIds();//order by title
            UserPref.setUserPref(0, 0, "Play" , "Title", 0);
            ascending = true;
            
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
