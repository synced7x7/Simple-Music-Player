package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Enum.DirectoryMode;
import com.example.simple_music_player.Enum.MediaStatus;
import com.example.simple_music_player.Model.SongLocator;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.Services.*;
import com.example.simple_music_player.SimpleMusicPlayer;
import com.example.simple_music_player.Utility.SongDetailsUtility;
import com.example.simple_music_player.Utility.SongIdAndIndexUtility;
import com.example.simple_music_player.db.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import lombok.Getter;
import lombok.Setter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LibraryController {

    @Getter
    @Setter
    @FXML
    private ListView<Integer> songListView;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> sortComboBox;
    @FXML
    private ComboBox<String> refreshComboBox;
    @FXML
    private ImageView backgroundImage;
    @FXML
    private Button reverseButton;
    @FXML
    private Button directoryButton;
    @FXML
    private Label songCountLabel;
    @FXML
    private ProgressBar loadingProgressBar;
    @FXML
    private Button removeDuplicatesButton;
    @FXML
    public AnchorPane root;

    private static final double CARD_WIDTH = 120;
    private static final double CARD_HEIGHT = 150;

    private final PlaybackService playbackService = NowPlayingController.getPlaybackService();

    private final TrackDAO trackDAO = new TrackDAO(DatabaseManager.getConnection());
    private final MiscDAO miscDAO = new MiscDAO(DatabaseManager.getConnection());
    private File selectedDir;
    private final File defaultMusicDirOpener = new File(System.getProperty("user.home"));

    private final UserPrefDAO userPrefDAO = new UserPrefDAO(DatabaseManager.getConnection());
    private final PlaylistsDAO playlistsDAO = new PlaylistsDAO(DatabaseManager.getConnection());
    @Getter
    private static LibraryController instance;
    public static boolean isPlaylistChanged = false;
    @Getter
    @Setter
    public int currentPlaylistId;

    private boolean isDescendant(Node parent, Node child) {
        while (child != null) {
            if (child == parent) return true;
            child = child.getParent();
        }
        return false;
    }

    @FXML
    public void initialize() throws SQLException {
        instance = this;
        songListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        //Load initial library from DB
        if (SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
            loadInitialDirectoryFromDatabase();
        else {
            loadFileFromCommandLineArgument();
        }

        PlaybackService.setLibraryController(this);
        if (trackDAO.getTrackPath() != null) {
            selectedDir = new File(trackDAO.getTrackPath());
        }

        root.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!isDescendant(songListView, (Node) e.getTarget())) {
                songListView.getSelectionModel().clearSelection();
            }
        });

        directoryButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Select Music Directory");

            File initialDir = selectedDir;
            if (initialDir == null || !initialDir.exists() || !initialDir.isDirectory()) {
                initialDir = defaultMusicDirOpener;
            }

            // Final safety fallback (just in case home folder is invalid)
            if (!initialDir.exists() || !initialDir.isDirectory()) {
                initialDir = new File("C:/");
            }

            directoryChooser.setInitialDirectory(initialDir);

            File newDir = directoryChooser.showDialog(directoryButton.getScene().getWindow());
            if (newDir == null) return;

            if (selectedDir == null || !selectedDir.equals(newDir)) {
                System.out.println("Directory changed: clearing old tracks");
                trackDAO.deleteAllTracks();
                playbackService.clearList();
            }

            selectedDir = newDir;
            try {
                loadSongsFromDirectory(selectedDir, DirectoryMode.DIR_CHANGE);
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Setup search
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            try {
                filterTracks(newVal);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        // Sort ComboBox
        sortComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && UserPref.shuffle == 0) sortLibrary(newVal);
            else System.out.println("Shuffle Mode is active. Can't sort");
        });

        refreshComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "⚒" : item);
            }
        });


        refreshComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            try {
                if (newVal == null) return;
                if (newVal.equals("Update Library")) {
                    loadSongsFromDirectory(new File(trackDAO.getTrackPath()), DirectoryMode.REFRESH_MODE);
                } else if (newVal.equals("Force Refresh")) {
                    loadSongsFromDirectory(new File(trackDAO.getTrackPath()), DirectoryMode.FORCE);
                }
                Platform.runLater(() -> refreshComboBox.getSelectionModel().clearSelection());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
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
            private final Label qLabel = new Label();

            {
                card.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
                cover.setFitWidth(CARD_WIDTH);
                cover.setFitHeight(CARD_WIDTH);
                cover.setPreserveRatio(true);

                nameLabel.setPrefWidth(CARD_WIDTH);
                nameLabel.setLayoutY(CARD_WIDTH + 5);
                nameLabel.setWrapText(true);

                qLabel.setLayoutX(CARD_WIDTH - 25);
                qLabel.setLayoutY(CARD_HEIGHT - 5);


                // --- Favorite Button Styling ---
                favButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
                favButton.setLayoutX(CARD_WIDTH - 25); // bottom-right corner
                favButton.setLayoutY(CARD_HEIGHT - 25);
                favButton.setText("♡");

                card.getChildren().addAll(cover, nameLabel, qLabel, favButton);
            }

            @Override
            protected void updateItem(Integer id, boolean empty) {
                super.updateItem(id, empty);
                if (empty || id == null) {
                    setGraphic(null);
                } else {
                    // --- Handles multiple selection ---
                    ListView<Integer> listView = getListView();
                    MultipleSelectionModel<Integer> selectionModel = listView.getSelectionModel();
                    //

                    Track track = trackDAO.getTrackCompressedArtworkAndTitleById(id);
                    if (track == null) {
                        System.out.println("No track found for id " + id + ". Removing...");
                        try {
                            playlistsDAO.deleteSongsFromPlaylist(UserPref.playlistId, Collections.singletonList(id));
                            trackDAO.removeFromLibrary(id);
                        } catch (SQLException e) {
                            throw new RuntimeException(e);
                        }
                        return;
                    }
                    final Integer songId = id;
                    nameLabel.setText(track.getTitle());
                    cover.setImage(null);

                    boolean isFavorite;
                    try {
                        isFavorite = playlistsDAO.isSongInPlaylist(3, id);
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }

                    if (isFavorite) {
                        favButton.setText("♥");
                        favButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 16px;");
                    } else {
                        favButton.setText("♡");
                        favButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
                    }

                    boolean isInQ = false;
                    QueueService queueService = AppContext.getQueueService();
                    int count = 0;
                    LinkedList<Integer> q = queueService.getQueueList();
                    if (!q.isEmpty()) {
                        count = Math.toIntExact(q.stream().filter(item -> Objects.equals(item, id)).count());
                        if (count > 0) {
                            isInQ = true;
                            System.out.println("ID " + id + " appears " + count + " times in the queue.");
                        }
                    }

                    if (isInQ) {
                        qLabel.setText(String.valueOf(count));
                    } else {
                        qLabel.setText("");
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
                    addToQueue.setOnAction(e -> {
                        List<Integer> selectedIds = new ArrayList<>(selectionModel.getSelectedItems());
                        for (Integer sid : selectedIds) queueService.addToQueue(sid);
                        System.out.println("QueueList: " + queueService.getQueueList());
                    });
                    MenuItem removeFromQueue = new MenuItem("Remove from Queue");
                    removeFromQueue.setOnAction(e -> {
                        List<Integer> selectedIds = new ArrayList<>(selectionModel.getSelectedItems());
                        for (Integer sid : selectedIds) queueService.removeFromQueue(sid);
                        System.out.println("QueueList: " + queueService.getQueueList());
                    });

                    Menu queueMenu = new Menu("Queue");
                    queueMenu.getItems().addAll(addToQueue, removeFromQueue);

                    // --- Playlist Menu ---
                    MenuItem addToPlaylist = new MenuItem("Add to Playlist");
                    addToPlaylist.setOnAction((event) -> {
                        PlaylistService playlistService = new PlaylistService();
                        List<Integer> selectedIds = new ArrayList<>(selectionModel.getSelectedItems());
                        playlistService.openPlaylistSelectionWindow(selectedIds);
                    });

                    // -- Open File Location --
                    MenuItem openFileLocation = new MenuItem("Open File Location");
                    openFileLocation.setOnAction((event) -> {
                        try {
                            String path = trackDAO.getFileLocationById(id);
                            if (path != null && !path.isEmpty()) {
                                File file = new File(path);
                                if (file.exists()) {
                                    // --- Open Explorer with file selected ---
                                    Runtime.getRuntime().exec("explorer /select,\"" + file.getAbsolutePath() + "\"");
                                } else {
                                    System.err.println("File does not exist: " + path);
                                }
                            } else {
                                System.err.println("Path not found in database for ID: " + id);
                            }
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });

                    // -- Song Removal --
                    MenuItem removeFromLib;
                    if (UserPref.playlistId <= 3) {
                        removeFromLib = new MenuItem("Remove From Library");
                        removeFromLib.setOnAction((event) -> {
                            try {
                                List<Integer> selectedIds = new ArrayList<>(selectionModel.getSelectedItems());
                                for (Integer sId : selectedIds) {
                                    trackDAO.removeFromLibrary(sId);
                                    playlistsDAO.deleteSongFromAllPlaylists(sId);
                                    songListView.getItems().remove(sId);
                                    songListView.getSelectionModel().clearSelection();
                                    PlaybackService.playlist.remove(sId);
                                    refreshSongCountLabel();
                                }
                                System.out.println("Songs successfully removed from library + " + selectedIds);
                            } catch (SQLException e) {
                                System.out.println("Could not remove from library: " + songId);
                            }
                        });
                    } else {
                        removeFromLib = new MenuItem("Remove From Playlist");
                        removeFromLib.setOnAction((event) -> {
                            try {
                                List<Integer> selectedIds = new ArrayList<>(selectionModel.getSelectedItems());
                                for (Integer sId : selectedIds) {
                                    playlistsDAO.deleteSongFromPlaylist(sId, currentPlaylistId);
                                    songListView.getItems().remove(sId);
                                    songListView.getSelectionModel().clearSelection();
                                    PlaybackService.playlist.remove(sId);
                                    refreshSongCountLabel();
                                }
                                System.out.println("Songs successfully removed from playlist: " + selectedIds);
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }

                    MenuItem physicallyDelete = new MenuItem("Physically Delete");
                    physicallyDelete.setOnAction((event) -> {
                        try {
                            List<Integer> selectedIds = new ArrayList<>(selectionModel.getSelectedItems());
                            for (Integer sId : selectedIds) {
                                String path = trackDAO.getFileLocationById(sId);
                                if (path != null && !path.isEmpty()) {
                                    File file = new File(path);
                                    if (file.exists()) {
                                        boolean deleted = file.delete();
                                        if (!deleted) {
                                            System.err.println("Could not delete file: " + path);
                                            return;
                                        }
                                    }
                                }
                                trackDAO.removeFromLibrary(sId);
                                playlistsDAO.deleteSongFromAllPlaylists(sId);
                                System.out.println("Removed from Library & Deleted File: " + sId);
                                songListView.getItems().remove(sId);
                                PlaybackService.playlist.remove(sId);
                                refreshSongCountLabel();
                            }
                        } catch (SQLException e) {
                            System.err.println("Error removing song from DB: " + id);
                            throw new RuntimeException(e);
                        }
                    });

                    Menu remove = new Menu("Remove");
                    if (UserPref.playlistId <= 2)
                        remove.getItems().addAll(removeFromLib, physicallyDelete);
                    else
                        remove.getItems().addAll(removeFromLib);

                    // --- Song Info ---
                    MenuItem viewDetails = new MenuItem("View Details");
                    viewDetails.setOnAction((event) -> {
                        SongDetailsUtility songDetailsUtiliy;
                        songDetailsUtiliy = new SongDetailsUtility();
                        songDetailsUtiliy.openSongDetails(songId);
                    });

                    // --- Attach to ContextMenu ---
                    ContextMenu contextMenu = new ContextMenu();
                    if (UserPref.playlistId != 3)
                        contextMenu.getItems().addAll(queueMenu, addToPlaylist, openFileLocation, remove, viewDetails);
                    else
                        contextMenu.getItems().addAll(queueMenu, addToPlaylist, openFileLocation, viewDetails);

                    card.setOnMouseClicked(e -> {
                        if (e.getButton() == MouseButton.PRIMARY) {
                            if (e.isControlDown()) {
                                songListView.getSelectionModel().select(songListView.getItems().indexOf(id));
                                e.consume();
                                return;
                            } else if (e.isShiftDown()) {
                                int last = songListView.getSelectionModel().getSelectedIndex();
                                int current = songListView.getItems().indexOf(id);
                                songListView.getSelectionModel().selectRange(Math.min(last, current), Math.max(last, current) + 1);
                                e.consume();
                                return;
                            } else {
                                // Single click → clear selection and play
                                songListView.getSelectionModel().clearSelection();
                                songListView.getSelectionModel().select(songListView.getItems().indexOf(id));
                            }

                            if (isPlaylistChanged) {
                                isPlaylistChanged = false;
                                try {
                                    //shifted to new playlist
                                    queueService.clearQueue();
                                    currentPlaylistId = UserPref.playlistId;
                                    toggleRemoveDuplicatesButton(currentPlaylistId > 3);
                                    int reverse = playlistsDAO.getReverse(currentPlaylistId);
                                    String sort = playlistsDAO.getSortingPref(currentPlaylistId);
                                    boolean ascending = reverse != 1;
                                    List<Integer> playlistSongs = trackDAO.getAllIdsSorted(currentPlaylistId, sort, ascending);
                                    playbackService.setPlaylist(playlistSongs, false);
                                    UserPref.shuffle = 0;
                                    UserPref.repeat = 0;
                                    toggleSort(false);
                                    NowPlayingController npc = NowPlayingController.getInstance();
                                    npc.updateRepeatButtonStyle();
                                    npc.updateShuffleButtonStyle();
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
                        } else if (e.getButton() == MouseButton.SECONDARY && UserPref.playlistId == currentPlaylistId) {
                            if (contextMenu.isShowing()) {
                                contextMenu.hide();
                            }
                            int idx = SongIdAndIndexUtility.getIndexFromSongId(id);
                            System.out.println("SongId: " + id + " Index: " + idx);
                            contextMenu.show(card, e.getScreenX(), e.getScreenY());
                            e.consume();
                        } else {
                            songListView.getSelectionModel().clearSelection();
                        }
                    });

                    favButton.setOnAction(e -> {
                        try {
                            if (favButton.getText().equals("♡")) {
                                favButton.setText("♥");
                                favButton.setStyle("-fx-background-color: transparent; -fx-text-fill: red; -fx-font-size: 16px;");
                                List<Integer> songIds = new ArrayList<>(selectionModel.getSelectedItems());
                                songIds.add(id);
                                System.out.println("songIds: " + songIds);
                                for (int sId : songIds) {
                                    if (playlistsDAO.isSongInPlaylist(3, sId)) {
                                        System.out.println("SongId: " + sId + " is in playlist");
                                        continue;
                                    }
                                    playlistsDAO.updateSongInPlaylist(3, sId);
                                }
                                songListView.refresh();
                            } else {
                                favButton.setText("♡");
                                favButton.setStyle("-fx-background-color: transparent; -fx-font-size: 16px;");
                                List<Integer> songIds = new ArrayList<>(selectionModel.getSelectedItems());
                                songIds.add(id);
                                playlistsDAO.deleteSongsFromPlaylist(3, songIds);
                                songListView.refresh();
                            }
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    });

                    setGraphic(card);
                }
            }
        });


        playbackService.currentTrackProperty().addListener((obs, oldT, newT) -> {
            if (newT.getCover() != null) {
                backgroundImage.setImage(newT.getCover());
            } else {
                backgroundImage.setImage(null);
            }
        });

    }

    // --- Sorting ---
    private void sortLibrary(String criteria) {
        System.out.println("sortLibrary() --> criteria: " + criteria);
        clearSearchField();
        restartFromStart = true;
        new Thread(() -> {
            int songId;
            List<Integer> sortedIds;

            if (criteria.equals("Reverse")) {
                try {
                    songId = PlaybackService.getPlaylist().get(playbackService.getCurrentIndex());

                    int reverse = playlistsDAO.getReverse(UserPref.playlistId);
                    reverse = reverse == 0 ? 1 : 0;
                    boolean asc = reverse != 1;

                    String currentSort = playlistsDAO.getSortingPref(UserPref.playlistId);
                    if (currentSort == null || currentSort.isEmpty()) {
                        currentSort = "Title";
                    }

                    sortedIds = trackDAO.getAllIdsSorted(UserPref.playlistId, currentSort, asc);

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
                    if (currentPlaylistId == UserPref.playlistId) {
                        playbackService.setPlaylist(sortedIds, false);
                        int idx = PlaybackService.getPlaylist().indexOf(finalSongId);
                        playbackService.setCurrentIndex(idx);
                        UserPref.playlistNo = idx;
                    } else
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
        UserPref.playlistId = playlistId;
        currentPlaylistId = playlistId;
        toggleRemoveDuplicatesButton(currentPlaylistId > 3);

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
        MediaStatus mediaStatus;
        //Play-pause handler
        if(status.equals("Play")) mediaStatus = MediaStatus.PLAYING;
        else mediaStatus = MediaStatus.PAUSED;
        NowPlayingController npc = NowPlayingController.getInstance();
        npc.togglePlayPause(mediaStatus);
        //
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
            String path = trackDAO.getTrackPath();
            File dir = new File(path);
            miscDAO.upsertFileTimestamp(dir.lastModified());
        }

        List<Integer> finalIdsToLoad = idsToLoad;
        List<Integer> finalShuffleIdsToLoad = shuffleIdsToLoad;
        System.out.println("Ids to load: " + idsToLoad);
        System.out.println("Shuffle Ids to load: " + shuffleIdsToLoad);

        Platform.runLater(() -> {
            if (UserPref.shuffle == 0) {
                try {
                    countSongs(finalIdsToLoad.size(), playlistId);
                    songListView.getItems().setAll(finalIdsToLoad);
                    playbackService.setPlaylist(finalIdsToLoad, idx, status, ts);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                playbackService.initialTimePropertyBinding();
            } else {
                try {
                    countSongs(idsToLoad.size(), playlistId);
                    songListView.getItems().setAll(idsToLoad);
                    playbackService.setPlaylist(finalShuffleIdsToLoad, idx, status, ts);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                playbackService.initialTimePropertyBinding();
            }

            //initialize Controller
            npc.setInitialVolumeSliderControllerValue(UserPref.volume);
            npc.updateShuffleButtonStyle();
            npc.updateRepeatButtonStyle();
        });
    }


    // --- Directory Load ---
    private void loadSongsFromDirectory(File dir, DirectoryMode directoryMode) throws SQLException {
        if (dir == null || !dir.exists() || !dir.isDirectory()) return;
        System.out.println(dir.getAbsolutePath() + " " + dir.lastModified());
        System.out.println("Is file modified : " + miscDAO.isFileModified(dir.lastModified()));

        if ((directoryMode == DirectoryMode.DIR_CHANGE || directoryMode == DirectoryMode.REFRESH_MODE) && !miscDAO.isFileModified(dir.lastModified())) {
            System.out.println("File not modified!");
            return;
        }
        miscDAO.upsertFileTimestamp(dir.lastModified());
        toggleSort(false);
        // --- queue service ---
        QueueService queueService = AppContext.getQueueService();
        queueService.clearQueue();
        //
        // --- Create playlist ---
        if(directoryMode == DirectoryMode.DIR_CHANGE) playlistsDAO.clearAllPlaylists();
        playlistsDAO.createShuffledPlaylist();
        playlistsDAO.createNormalPlaylist();
        playlistsDAO.createFavPlaylist();

        File[] files = dir.listFiles(this::isAudioFile);

        if (files == null || files.length == 0) {
            playbackService.setPlaylist(Collections.emptyList(), true);
            songListView.getItems().clear();
            return;
        }

        loadingProgressBar.setVisible(true);
        loadingProgressBar.setDisable(false);
        loadingProgressBar.setProgress(0);
        songListView.setDisable(true);
        CompletableFuture.runAsync(() -> {
            int total = files.length;
            int count = 0;

            for (File f : files) {
                try {
                    Track t = new Track(f.getAbsolutePath());
                    trackDAO.updateTracks(t);
                    count++;
                    double finalProgress = (double) count / total;
                    Platform.runLater(() -> loadingProgressBar.setProgress(finalProgress));
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }

            // hide bar when done
            Platform.runLater(() -> {
                loadingProgressBar.setProgress(1);
                loadingProgressBar.setVisible(false);
                loadingProgressBar.setDisable(true);
                songListView.setDisable(false);
            });

            List<Integer> allIds = trackDAO.getAllIdsSortByDefault();

            if (UserPref.volume == 0) UserPref.volume = 0.75;

            currentPlaylistId = 2;
            toggleRemoveDuplicatesButton(false);
            UserPref.setUserPref(0, 0, "Play", 0, 0, 1, UserPref.volume, 2);
            try {
                playlistsDAO.insertSongsInPlaylist(2, allIds);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }

            Platform.runLater(() -> {
                try {
                    countSongs(allIds.size(), 2);
                    playbackService.setPlaylist(allIds, 0, "Play", 0);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                songListView.getItems().setAll(allIds);
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

    private void countSongs(int count, int playlistId) throws SQLException {
        songCountLabel.setText(playlistsDAO.getPlaylistName(playlistId) + " - " + count + " songs");
    }

    // --- Search ---
    private void filterTracks(String query) throws SQLException {
        String sortBy = getSortStatusOfPlaylist(UserPref.playlistId);
        if (sortBy.equals("Date Added")) sortBy = "date_added";
        boolean rev = getReverseStatusOfPlaylist(UserPref.playlistId);
        String finalSortBy = sortBy;
        boolean isEmptyQuery = (query == null || query.trim().isEmpty());

        CompletableFuture.runAsync(() -> {
            List<Integer> filteredIds = trackDAO.searchTrackIds(UserPref.playlistId, query, finalSortBy, rev);

            Platform.runLater(() -> {
                songListView.getItems().setAll(filteredIds);

                // After updating items, restore focus if search is cleared
                if (isEmptyQuery && UserPref.playlistId == currentPlaylistId) {
                    // Get current playing song ID
                    int currentSongId = PlaybackService.playlist.get(playbackService.getCurrentIndex());

                    // Find its position in the filtered list
                    int indexInList = filteredIds.indexOf(currentSongId);

                    if (indexInList != -1) {
                        songListView.getSelectionModel().select(indexInList);
                        songListView.scrollTo(indexInList);
                        System.out.println("Focus restored to index: " + indexInList + " (Song ID: " + currentSongId + ")");
                    } else {
                        System.out.println("Current song not found in list");
                    }
                }
            });
        });
    }

    private boolean isAudioFile(File d, String name) {
        int dotIndex = name.lastIndexOf('.');
        if (dotIndex == -1) return false;
        String ext = name.substring(dotIndex + 1).toLowerCase();
        return ext.equals("mp3") || ext.equals("wav") || ext.equals("aac") || ext.equals("m4a") || ext.equals("flac");
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
        clearSearchField();
        PlaylistService playlistService = new PlaylistService();
        playlistService.openPlaylistSelectionWindow(Collections.singletonList(-1)); // -1 refers to opening playlistManager from library not from
        //right-clicking song card
    }

    public void loadPlaylistView(int playlistId) throws SQLException {
        isPlaylistChanged = true;
        UserPref.playlistId = playlistId;
        boolean ascending = getReverseStatusOfPlaylist(playlistId);
        String sort = getSortStatusOfPlaylist(playlistId);
        boolean shouldRestoreFocus = (playlistId == currentPlaylistId);

        CompletableFuture.runAsync(() -> {
            List<Integer> playlistSongs = trackDAO.getAllIdsSorted(playlistId, sort, ascending);
            System.out.println("Playlist Songs -> " + playlistSongs);

            Platform.runLater(() -> {
                songListView.getItems().setAll(playlistSongs);

                try {
                    countSongs(playlistSongs.size(), playlistId);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                toggleRemoveDuplicatesButton(UserPref.playlistId >= 3 && shouldRestoreFocus);
                // Restore focus AFTER items are loaded
                if (shouldRestoreFocus && !PlaybackService.playlist.isEmpty()) {
                    Platform.runLater(() -> {
                        try {
                            int currentSongId = PlaybackService.playlist.get(playbackService.getCurrentIndex());
                            int indexInList = playlistSongs.indexOf(currentSongId);

                            if (indexInList != -1) {
                                songListView.getSelectionModel().select(indexInList);
                                songListView.scrollTo(indexInList);
                                System.out.println("Focus restored to currently playing song at index: " + indexInList);
                            } else {
                                System.out.println("Current song not in this playlist view");
                            }
                        } catch (Exception e) {
                            System.out.println("Could not restore focus: " + e.getMessage());
                        }
                    });
                }
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

    private void clearSearchField() {
        searchField.clear();
    }

    @FXML
    private void removeDuplicatesFromPlaylists() throws SQLException {
        List<Integer> uniquePlaylist = new ArrayList<>(new LinkedHashSet<>(PlaybackService.playlist));
        PlaybackService.playlist.clear();
        PlaybackService.playlist.addAll(uniquePlaylist);

        playlistsDAO.removeDuplicates(currentPlaylistId);

        songListView.getItems().setAll(uniquePlaylist);
        songListView.getSelectionModel().clearSelection();
        refreshSongCountLabel();
        System.out.println("Duplicate songs successfully removed from playlist.");
    }

    private void toggleRemoveDuplicatesButton(boolean enable) {
        if (enable) {
            removeDuplicatesButton.setVisible(true);
            removeDuplicatesButton.setManaged(true);
            removeDuplicatesButton.setDisable(false);
        } else {
            removeDuplicatesButton.setVisible(false);
            removeDuplicatesButton.setManaged(false);
            removeDuplicatesButton.setDisable(true);
        }
    }

    private void refreshSongCountLabel() throws SQLException {
        songCountLabel.setText(playlistsDAO.getPlaylistName(currentPlaylistId) + " - " + PlaybackService.playlist.size() + " songs");
    }

    private void loadFileFromCommandLineArgument() throws SQLException {
        System.out.println("Loading songs by using command Line Argument");
        TempPlaylistService tempPlaylistService = new TempPlaylistService(playbackService);
        tempPlaylistService.loadTempPlaylist();
        NowPlayingController npc = NowPlayingController.getInstance();
        npc.showLibraryButton(false);
        npc.hideShuffleButton();
        refreshSongCountLabel();
    }

}
