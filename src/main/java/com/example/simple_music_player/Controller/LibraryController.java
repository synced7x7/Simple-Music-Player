package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.SongLocator;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Services.PlaylistService;
import com.example.simple_music_player.Utility.SongIdAndIndexUtility;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.PlaylistsDAO;
import com.example.simple_music_player.db.TrackDAO;
import com.example.simple_music_player.db.UserPrefDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class LibraryController {

    @Getter
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
    @FXML
    private Button playlistManagerButton;

    private static final double CARD_WIDTH = 120;
    private static final double CARD_HEIGHT = 150;

    private final PlaybackService playbackService = NowPlayingController.getPlaybackService();

    private final TrackDAO trackDAO = new TrackDAO(DatabaseManager.getConnection());

    private File selectedDir;
    private final File defaultMusicDirOpener = new File("C:/Users/Asus/Music");
    private File[] prevFiles = null;
    private boolean dirChanged = false;

    private final UserPrefDAO userPrefDAO = new UserPrefDAO(DatabaseManager.getConnection());
    private final PlaylistsDAO playlistsDAO = new PlaylistsDAO(DatabaseManager.getConnection());
    @Getter
    private static LibraryController instance;
    public static boolean isPlaylistChanged = false;
    @Getter
    public int currentPlaylistId;

    @FXML
    public void initialize() throws SQLException {
        instance = this;
        //Load initial library from DB
        loadInitialDirectoryFromDatabase();
        PlaybackService.setLibraryController(this);
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
            try {
                loadSongsFromDirectory(selectedDir);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Setup search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTracks(newVal));
        // Sort ComboBox
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && UserPref.shuffle == 0) sortLibrary(newVal);
            else System.out.println("Shuffle Mode is active. Can't sort");
        });

        reverseButton.setOnAction(e -> {
            if (UserPref.shuffle == 0) sortLibrary("Reverse");
            else System.out.println("Shuffle Mode is active. Can't reverse");
        });

        // ListView Cell Factory (virtualized cards)
        songListView.setCellFactory(list -> new ListCell<>() {
            private final AnchorPane card = new AnchorPane();
            private final ImageView cover = new ImageView();
            private final Label nameLabel = new Label();
            private final Button favButton = new Button();

            {
                card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
                card.setStyle("-fx-border-color: gray; -fx-background-color: #f0f0f0;");

                cover.setFitWidth(CARD_WIDTH);
                cover.setFitHeight(CARD_WIDTH);
                cover.setPreserveRatio(true);

                nameLabel.setPrefWidth(CARD_WIDTH);
                nameLabel.setLayoutY(CARD_WIDTH + 5);
                nameLabel.setWrapText(true);

                // --- Favorite Button Styling ---
                favButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
                favButton.setLayoutX(CARD_WIDTH - 25); // bottom-right corner
                favButton.setLayoutY(CARD_HEIGHT - 25);
                favButton.setText("♡");

                card.getChildren().addAll(cover, nameLabel, favButton);
            }


            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setGraphic(null);
                } else {
                    Track track = trackDAO.getTrackCompressedArtworkAndTitleById(id);
                    if (track == null) {
                        System.out.println("No track found for id " + id + ". Removing...");
                        try {
                            playlistsDAO.deleteSongsFromPlaylist(UserPref.playlistId, Collections.singletonList(id));
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }
                    nameLabel.setText(track.getTitle());
                    cover.setImage(null);

                    boolean isFavorite = false;
                    try {
                        isFavorite = playlistsDAO.isSongInPlaylist(3, id);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    if (isFavorite) {
                        favButton.setText("♥");
                        favButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 16px;");
                    } else {
                        favButton.setText("♡");
                        favButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
                    }

                    // Async thumbnail load
                    CompletableFuture
                            .supplyAsync(() -> new Image(new ByteArrayInputStream(track.getCompressedArtworkData())))
                            .thenAccept(thumbnail -> {
                                if (thumbnail != null) {
                                    Platform.runLater(() -> cover.setImage(thumbnail));
                                }
                            });

                    // --- Queue Menu ---
                    MenuItem addToQueue = new MenuItem("Add to Queue");
                    MenuItem removeFromQueue = new MenuItem("Remove from Queue");
                    Menu queueMenu = new Menu("Queue");
                    queueMenu.getItems().addAll(addToQueue, removeFromQueue);

                    // --- Playlist Menu ---
                    MenuItem addToPlaylist = new MenuItem("Add to Playlist");
                    addToPlaylist.setOnAction((event) -> {
                        PlaylistService playlistService = new PlaylistService();
                        playlistService.openPlaylistSelectionWindow(id);
                    });

                    // --- Other Options ---
                    MenuItem viewDetails = new MenuItem("View Details");

                    // --- Attach to ContextMenu ---
                    ContextMenu contextMenu = new ContextMenu();
                    contextMenu.getItems().addAll(queueMenu, addToPlaylist, viewDetails);

                    card.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            if (isPlaylistChanged) {
                                isPlaylistChanged = false;
                                try {
                                    currentPlaylistId = UserPref.playlistId;
                                    int reverse = playlistsDAO.getReverse(currentPlaylistId);
                                    String sort = playlistsDAO.getSortingPref(currentPlaylistId);
                                    boolean ascending = reverse != 1;
                                    List<Integer> playlistSongs = trackDAO.getAllIdsSorted(currentPlaylistId, sort, ascending);
                                    playbackService.setPlaylist(playlistSongs, false);
                                } catch (SQLException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                            LibraryController.restartFromStart = false;
                            int index = PlaybackService.playlist.indexOf(id);
                            if (index != -1) {
                                try {
                                    playbackService.play(index);
                                } catch (SQLException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }
                            if (contextMenu.isShowing()) {
                                contextMenu.hide();
                            }
                            e.consume();
                        } else if (e.getButton() == MouseButton.SECONDARY) {
                            if (contextMenu.isShowing()) {
                                contextMenu.hide();
                            }
                            int idx = SongIdAndIndexUtility.getIndexFromSongId(id);
                            System.out.println("SongId: " + id + " Index: " + idx);
                            contextMenu.show(card, e.getScreenX(), e.getScreenY());
                            e.consume();
                        }
                    });

                    favButton.setOnAction(e -> {
                        if (favButton.getText().equals("♡")) {
                            favButton.setText("♥");
                            favButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 16px;");
                            List<Integer> songIds = new ArrayList<>();
                            songIds.add(id);
                            try {
                                playlistsDAO.insertSongsInPlaylist(3, songIds);
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        } else {
                            favButton.setText("♡");
                            favButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
                            List<Integer> songIds = new ArrayList<>();
                            songIds.add(id);
                            try {
                                playlistsDAO.deleteSongsFromPlaylist(3, songIds);
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });

                    setGraphic(card);
                }
            }
        });

    }


    // --- Sorting ---
    private void sortLibrary(String criteria) {
        System.out.println("sortLibrary() --> criteria: " + criteria);

        restartFromStart = true;
        new Thread(() -> {
            int songId;
            List<Integer> sortedIds;

            if (criteria.equals("Reverse")) {
                try {
                    sortedIds = new ArrayList<>(PlaybackService.getPlaylist());
                    songId = PlaybackService.getPlaylist().get(playbackService.getCurrentIndex());
                    Collections.reverse(sortedIds);
                    int reverse = playlistsDAO.getReverse(UserPref.playlistId);
                    reverse = reverse == 0 ? 1 : 0;
                    playlistsDAO.setPlaylistRev(UserPref.playlistId, reverse);
                    playlistsDAO.setPlaylistRev(1, reverse);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            } else {
                try {
                    songId = PlaybackService.getPlaylist().get(playbackService.getCurrentIndex());
                    boolean asc = getReverseStatusOfPlaylist(UserPref.playlistId);
                    sortedIds = trackDAO.getAllIdsSorted(UserPref.playlistId, criteria, asc);
                    playlistsDAO.setPlaylistSort(UserPref.playlistId, criteria);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }


            int finalSongId = songId; //safeguard mechanism of java so that multiple threads doesn't update the same variable
            // so either use temp or atomic when using variable in a thread.

            Platform.runLater(() -> {
                try {
                    songListView.getItems().setAll(sortedIds);
                    if(currentPlaylistId == UserPref.playlistId) {
                        playbackService.setPlaylist(sortedIds, false);
                        int idx = PlaybackService.getPlaylist().indexOf(finalSongId);
                        playbackService.setCurrentIndex(idx);
                        UserPref.playlistNo = idx;
                    }
                    else
                        System.out.println("Playlist not in focus");
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });
        }).start();
    }

    private void loadInitialDirectoryFromDatabase() throws SQLException {
        //User Pref Setter
        int playlistId = userPrefDAO.getPlaylistId();
        UserPref.playlistId = userPrefDAO.getPlaylistId();
        currentPlaylistId = playlistId;

        String sortingPref = playlistsDAO.getSortingPref(playlistId);
        int reverse = playlistsDAO.getReverse(playlistId);
        UserPref.repeat = userPrefDAO.getRepeat();
        UserPref.shuffle = userPrefDAO.getShuffle();
        UserPref.isRundown = userPrefDAO.getIsRundown();
        UserPref.volume = userPrefDAO.getVolume();
        System.out.println("Playlist Id: " + playlistId + " Sorting Pref: " + sortingPref + " Rev: " + reverse);
        //

        int idx = userPrefDAO.getPlaylistNo();
        String status = userPrefDAO.getUserStatus();
        long ts = userPrefDAO.getTimeStamp();
        List<Integer> idsToLoad;
        List<Integer> shuffleIdsToLoad = List.of();
        //


        boolean ascending = reverse != 1;

        if (sortingPref != null && !sortingPref.isEmpty()) {
            // Fetch from DB in sorted order directly
            idsToLoad = trackDAO.getAllIdsSorted(UserPref.playlistId, sortingPref, ascending);
            System.out.println("Loaded sorted order based on: " + sortingPref);
        } else {
            idsToLoad = trackDAO.getAllIdsSorted(UserPref.playlistId, "Title", ascending);
            System.out.println("Loaded songs based on default (Title)");
        }

        if (UserPref.shuffle == 1) {
            //Persistent Shuffling
            shuffleIdsToLoad = playlistsDAO.getSongsFromPlaylist(1);
            SongLocator.create(sortingPref, reverse);
            System.out.println("Loaded songs based on shuffling");
            toggleSort(true);
        }

        if (trackDAO.getTrackPath() != null) {
            File dir = new File(trackDAO.getTrackPath());
            prevFiles = dir.listFiles(this::isAudioFile);
        }

        List<Integer> finalIdsToLoad = idsToLoad;
        List<Integer> finalShuffleIdsToLoad = shuffleIdsToLoad;
        System.out.println("Ids to load: " + idsToLoad);
        System.out.println("Shuffle Ids to load: " + shuffleIdsToLoad);

        Platform.runLater(() -> {
            if (UserPref.shuffle == 0) {
                countSongs(finalIdsToLoad.size());
                songListView.getItems().setAll(finalIdsToLoad);
                try {
                    playbackService.setPlaylist(finalIdsToLoad, idx, status, ts);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                playbackService.initialTimePropertyBinding();
            } else {
                countSongs(idsToLoad.size());
                songListView.getItems().setAll(idsToLoad);
                try {
                    playbackService.setPlaylist(finalShuffleIdsToLoad, idx, status, ts);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                playbackService.initialTimePropertyBinding();
            }
            NowPlayingController npc = NowPlayingController.getInstance();
            if (npc != null) { //initialize Controller
                npc.setInitialVolumeSliderControllerValue(UserPref.volume);
                npc.updateShuffleButtonStyle();
                npc.updateRepeatButtonStyle();
            } else {
                System.out.println("NowPlayingController not initialized yet!");
            }
        });
    }


    // --- Directory Load ---
    private void loadSongsFromDirectory(File dir) throws SQLException {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        toggleSort(false);
        //Clear shuffled playlist
        playlistsDAO.deleteAllSongsFromPlaylist(1);
        playlistsDAO.deleteAllSongsFromPlaylist(2);
        playlistsDAO.createShuffledPlaylist();
        playlistsDAO.createNormalPlaylist();
        playlistsDAO.createFavPlaylist();

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
                    throw new RuntimeException(ex);
                }
            }

            List<Integer> allIds = trackDAO.getAllIdsSortByDefault();

            if (UserPref.volume == 0) UserPref.volume = 0.75;


            UserPref.setUserPref(0, 0, "Play", 0, 0, 1, UserPref.volume, 2);
            try {
                playlistsDAO.insertSongsInPlaylist(2, allIds);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            Platform.runLater(() -> {
                countSongs(allIds.size());
                try {
                    playbackService.setPlaylist(allIds, dirChanged);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                songListView.getItems().setAll(allIds);
                dirChanged = false;
                NowPlayingController npc = NowPlayingController.getInstance();
                if (npc != null) { //initialize Controller
                    npc.setInitialVolumeSliderControllerValue(UserPref.volume);
                    npc.updateShuffleButtonStyle();
                    npc.updateRepeatButtonStyle();
                } else {
                    System.out.println("NowPlayingController not initialized yet!");
                }
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
            List<Integer> filteredIds = trackDAO.searchTrackIds(UserPref.playlistId, query, finalSortBy, true);
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

    public void toggleSort(boolean disable) {
        if (disable) {
            sortComboBox.setDisable(true);
            reverseButton.setDisable(true);
        } else {
            sortComboBox.setDisable(false);
            reverseButton.setDisable(false);
        }
    }

    @FXML
    private void openPlaylistManager() {
        PlaylistService playlistService = new PlaylistService();
        playlistService.openPlaylistSelectionWindow(-1);
    }

    public void loadPlaylistView(int playlistId, String playlistName) throws SQLException {
        isPlaylistChanged = true;
        UserPref.playlistId = playlistId;
        boolean ascending = getReverseStatusOfPlaylist(playlistId);
        String sort = getSortStatusOfPlaylist(playlistId);
        /*UserPref.shuffle = 1;
        NowPlayingController npc = NowPlayingController.getInstance();
        if (npc != null) {
            npc.toggleShuffle();
        }*/
        CompletableFuture.runAsync(() -> {
            List<Integer> playlistSongs = trackDAO.getAllIdsSorted(playlistId, sort, ascending);
            System.out.println("Playlist Songs -> " + playlistSongs);

            Platform.runLater(() -> {
                songListView.getItems().setAll(playlistSongs);
                countSongs(playlistSongs.size());
                songCountLabel.setText(playlistName + " - " + playlistSongs.size() + " songs");
            });
        });
    }

    public boolean getReverseStatusOfPlaylist(int playlistId) throws SQLException {
        int reverse = playlistsDAO.getReverse(playlistId);
        return reverse != 1;
    }

    public String getSortStatusOfPlaylist(int playlistId) throws SQLException {
        return playlistsDAO.getSortingPref(playlistId);
    }


}
