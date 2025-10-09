package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
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

    private static final ObservableList<Track> allTracks = FXCollections.observableArrayList(); //Special kind of list that notifies listeners when its content changes (add, remove, update, sort).
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
    private  File[] prevFiles = null;

    @FXML
    public void initialize() {
        // Load default directory from DB (sorted by title)
        loadInitialDirectoryFromDatabase();
        if(trackDAO.getTrackPath()!=null) {
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
            }

            selectedDir = newDir;
            loadSongsFromDirectory(selectedDir);
            prevDir = selectedDir;
        });

        /*searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTracks(newVal));
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
        });*/
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
                //refreshGrid(allTracks);
                return;
            case "Reverse":
                FXCollections.reverse(allTracks);
                //refreshGrid(allTracks);
                return;
            default:
                return;
        }
        FXCollections.sort(allTracks, comparator);
        //refreshGrid(allTracks);
    }

    private void loadInitialDirectoryFromDatabase() {
        //For Playlist
        List<Integer> allIds = trackDAO.getAllIds();
        //List <Track> t = trackDAO.getAllTracksArtworkAndTitle();

        if(trackDAO.getTrackPath()!=null) {
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
        if(prevFiles!= null && Arrays.equals(prevFiles, files)) {
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
            //List<Track> firstPage = trackDAO.getAllTracksArtworkAndTitle();
            System.out.println("Directory changed = " + dirChanged);
            Platform.runLater(() -> {
                if (dirChanged) {
                    playbackService.setPlaylist(allIds, true);
                    dirChanged = false;
                }
                else
                    playbackService.setPlaylist(allIds, false);
                // refreshGrid(firstPage); // if needed
            });
        });
    }

    private void filterTracks(String query) {
        restartFromStart = true;
        if (query == null || query.isEmpty()) {
            //refreshGrid(allTracks);
            return;
        }

        String lowerQuery = query.toLowerCase();

        List<Track> filtered = allTracks.stream()
                .filter(t -> t.getTitle().toLowerCase().contains(lowerQuery)
                        || (t.getArtist() != null && t.getArtist().toLowerCase().contains(lowerQuery))
                        || (t.getAlbum() != null && t.getAlbum().toLowerCase().contains(lowerQuery)))
                .collect(Collectors.toList());

        //refreshGrid(filtered);
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
        byte[] artworkData = track.getCompressedArtworkData();
        Image thumbnail = null;
        if(artworkData != null) {
            thumbnail = new Image(new ByteArrayInputStream(artworkData));
        }
        //Image thumbnail = thumbnailCaching.loadThumbnail(track);
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
        /*card.setOnMouseClicked(e -> {
            restartFromStart = false;
            if (index != -1) {
                playbackService.play(index);
            }
        });*/

        return card;
    }

}
