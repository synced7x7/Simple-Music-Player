package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Enum.MediaStatus;
import com.example.simple_music_player.Model.LyricLine;
import com.example.simple_music_player.Model.SongLocator;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Services.RealtimeVisualizerService;
import com.example.simple_music_player.Services.VisualizerService;
import com.example.simple_music_player.Utility.AnimationUtils;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.CubicCurve;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import lombok.Setter;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;

import java.awt.Desktop;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Random;

public class NowPlayingController {
    @FXML
    private Button playButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button prevButton;
    @FXML
    private ImageView albumCover;
    @FXML
    private Label nameLabel;
    @FXML
    private Label artistLabel;
    @FXML
    private Label albumLabel;
    @FXML
    private Label bitRateLabel;
    @FXML
    private Label formatLabel;
    @FXML
    private Label timeLabel;
    @FXML
    private Label sampleRateLabel;
    @FXML
    private AnchorPane visualizerHolder;
    @FXML
    private AnchorPane realtimeVisualizerHolder;
    @FXML
    private Button repeatButton;
    @FXML
    private Button shuffleButton;
    @FXML
    private Slider volumeSlider;
    @FXML
    private ImageView backAlbumCover;
    @FXML
    private ScrollPane lyricsScrollPane;
    @FXML
    private VBox lyricsFlow;
    @FXML
    private Button toggleLibraryButton;
    @FXML
    private Button toggleAlbumButton;
    @FXML
    private Label syncedXLyricsLabel;
    @FXML
    private CubicCurve curve;
    @FXML
    private ImageView playButtonImageView;
    @FXML
    private Button lyricsButton;

    private List<LyricLine> syncedLyricLines = new ArrayList<>();
    private int lastSyncedLabelIndex = -1;
    private final DoubleProperty progress = new SimpleDoubleProperty(0);

    public DoubleProperty progressProperty() {
        return progress;
    }

    private List<LyricLine> currentLyricLines = new ArrayList<>();
    public static VisualizerService visualizerController;
    @Getter
    private RealtimeVisualizerService realtimeVisualizerController;
    @Getter
    private static final PlaybackService playbackService = new PlaybackService(); //one instance to be shared among all
    @Getter
    private static NowPlayingController instance;
    private int lastHighlightedIndex = -1;
    @Getter
    private int lyricsToggleCount = 0;

    public NowPlayingController() {
        instance = this;
    }

    private boolean isLyricsActive = false;

    @FXML
    public void initialize() throws IOException {
        syncedXLyricsLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 4, 0.2, 2, 2)");

        playbackService.setNowPlayingController(this);
        lyricsScrollPane.setVisible(false);
        lyricsScrollPane.setManaged(false);

        // Bind slider to PlaybackService
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            playbackService.setVolume(newVal.doubleValue());
            UserPref.volume = newVal.doubleValue();
        });

        //Visualizer
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/simple_music_player/visualizer.fxml"));
        AnchorPane visualizer = loader.load();
        visualizerController = loader.getController();
        visualizerHolder.getChildren().add(visualizer);

        //Realtime Visualizer
        FXMLLoader loader2 = new FXMLLoader(getClass().getResource("/com/example/simple_music_player/realtimeVisualizer.fxml"));
        AnchorPane visualizer2 = loader2.load();
        realtimeVisualizerController = loader2.getController();
        realtimeVisualizerHolder.getChildren().add(visualizer2);

        // Anchor it to fill the holder
        AnchorPane.setTopAnchor(visualizer, 0.0);
        AnchorPane.setBottomAnchor(visualizer, 0.0);
        AnchorPane.setLeftAnchor(visualizer, 0.0);
        AnchorPane.setRightAnchor(visualizer, 0.0);

        playbackService.currentTrackProperty().addListener((obs, oldT, newT) -> {
            if (newT != null && visualizerController != null) {
                visualizerController.loadWaveform(new File(newT.getPath()));
            }
        });

        playButton.setOnAction(e -> {
            try {
                playbackService.togglePlayPause();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        nextButton.setOnAction(e -> {
            try {
                playbackService.next();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });
        prevButton.setOnAction(e -> {
            try {
                playbackService.previous();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        });

        //Song Metadata
        playbackService.currentTrackProperty().addListener((obs, oldT, newT) -> {
            //Name
            nameLabel.setText(newT.getTitle());
            //Artist
            if (newT.getArtist() != null) artistLabel.setText(newT.getArtist());
            else artistLabel.setText("Unknown");
            //Album
            if (newT.getAlbum() != null) albumLabel.setText(newT.getAlbum());
            else albumLabel.setText("Unknown");
            //Format
            formatLabel.setText(newT.getFormat());
            //bitRate
            bitRateLabel.setText(newT.getBitrate());
            //Sample Rate
            sampleRateLabel.setText(newT.getSampleRate());
            //Cover
            double coverAR = newT.getCoverWidth() / newT.getCoverHeight();
            double screenAR = 0.608;
            if (coverAR < screenAR) {
                backAlbumCover.setFitHeight(-1);
                backAlbumCover.setFitWidth(450);
            } else {
                backAlbumCover.setFitHeight(750);
                backAlbumCover.setFitWidth(-1);
            }
            if (newT.getCover() != null) {
                albumCover.setImage(newT.getCover());
                backAlbumCover.setImage(newT.getCover());
            } else {
                Random rand = new Random();
                int randomNumber = rand.nextInt(6) + 1;
                System.out.println("Random Number: " + randomNumber);
                Image img;
                switch (randomNumber) {
                    case 1 ->
                            img = new Image(Objects.requireNonNull(getClass().getResource("/icons/visualizerico1.png")).toExternalForm());
                    case 2 ->
                            img = new Image(Objects.requireNonNull(getClass().getResource("/icons/visualizerico2.jpg")).toExternalForm());
                    case 3 ->
                            img = new Image(Objects.requireNonNull(getClass().getResource("/icons/visualizerico3.jpg")).toExternalForm());
                    case 4 ->
                            img = new Image(Objects.requireNonNull(getClass().getResource("/icons/visualizerico4.jpg")).toExternalForm());
                    case 5 ->
                            img = new Image(Objects.requireNonNull(getClass().getResource("/icons/visualizerico5.jpg")).toExternalForm());
                    default ->
                            img = new Image(Objects.requireNonNull(getClass().getResource("/icons/visualizerico6.jpg")).toExternalForm());

                }
                albumCover.setImage(img);
                backAlbumCover.setImage(img);
                AlbumCoverController albumCoverController = playbackService.getAlbumCoverController();
                albumCoverController.setAlbumCover(img);
            }

            //Lyrics
            if (isLyricsActive) {
                try {
                    displayLyrics(newT.getLyrics());
                } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException |
                         IOException e) {
                    throw new RuntimeException(e);
                }
            }

            if (lyricsToggleCount == 2) {
                try {
                    displaySyncedLyricInLabel(newT.getLyrics());
                } catch (CannotReadException | TagException | InvalidAudioFrameException | IOException |
                         ReadOnlyFileException e) {
                    syncedXLyricsLabel.setText("SYNCED_X_");
                    throw new RuntimeException(e);
                }
            }

            //Curve ProgressBar
            animateCurveProgress();
        });

        lyricsScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            double deltaY = event.getDeltaY() * 3;
            double height = lyricsScrollPane.getContent().getBoundsInLocal().getHeight();
            double vValue = lyricsScrollPane.getVvalue();
            // invert delta to match normal direction
            lyricsScrollPane.setVvalue(vValue - deltaY / height);
            event.consume();
        });
    }

    private void animateCurveProgress() {
        double curveLength = 500;
        curve.getStrokeDashArray().setAll(curveLength, curveLength);

        // Bind strokeDashOffset to (1 - progress)
        curve.strokeDashOffsetProperty().bind(
                progress.multiply(-curveLength).add(curveLength)
        );
    }

    @FXML
    public void toggleCountdown() {
        if (UserPref.isRundown == 0) {
            timeLabel.textProperty().bind(playbackService.remainingTimeProperty());
            UserPref.isRundown = 1;
        } else {
            timeLabel.textProperty().bind(playbackService.elapsedTimeProperty());
            UserPref.isRundown = 0;
        }
        System.out.println("Countdown after toggling: " + UserPref.isRundown);
    }

    public void bindTextPropertyToTime() {
        if (UserPref.isRundown == 1) {
            timeLabel.textProperty().bind(playbackService.remainingTimeProperty());
            System.out.println("Remaining time property bound.");
        } else {
            timeLabel.textProperty().bind(playbackService.elapsedTimeProperty());
            System.out.println("Elapsed time property bound.");
        }
    }

    public void clearScreen() {
        nameLabel.setText("Title");
        artistLabel.setText("Artist");
        albumLabel.setText("Album");
        formatLabel.setText("Format");
        bitRateLabel.setText("Bitrate");
        sampleRateLabel.setText("Samplerate");
        albumCover.setImage(null);
        visualizerController.cleanup();
        System.out.println("Screen Cleared");
    }

    @FXML
    private void infoButtonHandler() {
        UserPref.userPrefChecker();
        Stage infoStage = new Stage();
        infoStage.setTitle("About This App");

        Label appName = new Label("🎵 SIMPLE MUSIC PLAYER");
        appName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label version = new Label("Version 0.5.5");
        Label author = new Label("Developed by: synced_x_");
        Label mail = new Label("synced7x7@gmail.com");
        Label credits = new Label("Releases");
        credits.setStyle("-fx-text-fill: blue; -fx-underline: true;");
        credits.setCursor(Cursor.HAND);

        VBox root = new VBox(10, appName, version, author, mail, credits);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(15));
        /**/
        Scene scene = new Scene(root, 300, 200);
        infoStage.setScene(scene);
        infoStage.initModality(Modality.APPLICATION_MODAL);
        infoStage.setResizable(false);
        infoStage.show();

        // Easter egg: clicking anywhere in the window updates author name
        root.setOnMouseClicked(event -> {
            author.setText("Developed by: Tasnif Emran");
            mail.setText("tasnifemran@gmail.com, Instagram: synced_x");
        });

        // Clicking "Updates" → open website
        credits.setOnMouseClicked(event -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/synced7x7/Simple-Music-Player/releases"));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    @FXML
    public void toggleShuffle() {
        if (UserPref.shuffle == 1) { //turn off
            UserPref.shuffle = 0;
            new Thread(() -> {
                try {
                    playbackService.songRelocator();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                SongLocator.delete();
            }).start();
        } else {
            UserPref.shuffle = 1;
            new Thread(() -> {
                try {
                    playbackService.shufflePlaylist();
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

        updateShuffleButtonStyle();
        System.out.println("User Shuffle status after toggling: " + UserPref.shuffle);
    }

    @FXML
    private void toggleRepeat() {
        if (UserPref.repeat == 1) { //turn OFF
            UserPref.repeat = 0;
        } else {
            UserPref.repeat = 1;
        }
        updateRepeatButtonStyle();
        System.out.println("User Repeat status after toggling: " + UserPref.repeat);
    }

    public void setInitialVolumeSliderControllerValue(double value) {
        volumeSlider.setValue(value);
        playbackService.setVolume(volumeSlider.getValue());
    }

    public void updateShuffleButtonStyle() {
        boolean isActive = (UserPref.shuffle == 1);
        shuffleButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), isActive);
    }

    public void updateRepeatButtonStyle() {
        boolean isActive = (UserPref.repeat == 1);
        repeatButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), isActive);
    }

    @FXML
    private void toggleLyrics() throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        lyricsToggleCount = (lyricsToggleCount + 1) % 3;

        Track currentTrack = playbackService.getCurrentTrack();

        switch (lyricsToggleCount) {
            // === CASE 0: Album cover only ===
            case 0 -> {
                syncedXLyricsLabel.setVisible(false);
                syncedXLyricsLabel.setText("");
                isLyricsActive = false;

                currentLyricLines.clear();
                lastHighlightedIndex = -1;

                albumCover.setVisible(true);
                albumCover.setManaged(true);

                lyricsScrollPane.setVisible(false);
                lyricsScrollPane.setManaged(false);
                realtimeVisualizerController.stopVisualizer();
                lyricsButton.setStyle("-fx-text-fill: white");
            }

            // === CASE 1: Full Lyrics View ===
            case 1 -> {
                isLyricsActive = true;

                albumCover.setVisible(false);
                albumCover.setManaged(false);

                lyricsScrollPane.setVisible(true);
                lyricsScrollPane.setManaged(true);

                syncedXLyricsLabel.setVisible(false);

                if (currentTrack != null) {
                    displayLyrics(currentTrack.getLyrics());
                } else {
                    currentLyricLines.clear();
                    lastHighlightedIndex = -1;
                }

                realtimeVisualizerController.stopVisualizer();
                lyricsButton.setStyle("-fx-text-fill: linear-gradient(to right, #6261bc, #ffffff)");
            }

            // === CASE 2: SyncedX Mode (Single Line + Visualizer) ===
            case 2 -> {
                isLyricsActive = false;
                currentLyricLines.clear();
                lastHighlightedIndex = -1;

                albumCover.setVisible(false);
                albumCover.setManaged(false);

                lyricsScrollPane.setVisible(false);
                lyricsScrollPane.setManaged(false);

                syncedXLyricsLabel.setVisible(true);

                // 🔹 Show synced lyric in label or fallback
                if (currentTrack != null && currentTrack.getLyrics() != null) {
                    displaySyncedLyricInLabel(currentTrack.getLyrics());
                } else {
                    syncedXLyricsLabel.setText("SYNCED_X_");
                }

                // 🔹 Start visualizer
                realtimeVisualizerController.startVisualizer();
                playbackService.setupVisualizerListener(playbackService.getMediaPlayer());
                lyricsButton.setStyle("-fx-text-fill:   linear-gradient(to top right, #a674fc, #ff003a)");

                System.out.println("SyncedXMode Activated");
            }
        }
    }


    public void displaySyncedLyricInLabel(String lyrics) {
        syncedXLyricsLabel.setText(""); // reset
        syncedLyricLines.clear();
        lastSyncedLabelIndex = -1;

        if (lyrics == null || lyrics.isEmpty()) {
            syncedXLyricsLabel.setText("SYNCED_X_");
            return;
        }

        List<LyricLine> parsed = parseLyrics(lyrics);
        if (parsed.isEmpty()) {
            // Not synced, plain text
            syncedXLyricsLabel.setText("SYNCED_X_");
            return;
        }

        syncedLyricLines = parsed;
        syncedXLyricsLabel.setText(parsed.getFirst().getText()); // initialize first line
    }

    public void updateSyncedLyricLabel(Duration currentTime) {
        if (syncedLyricLines.isEmpty()) return;

        int currentIndex = -1;

        for (int i = 0; i < syncedLyricLines.size(); i++) {
            Duration lineTime = syncedLyricLines.get(i).getTimestamp();

            if (currentTime.greaterThanOrEqualTo(lineTime)) {
                if (i < syncedLyricLines.size() - 1) {
                    Duration next = syncedLyricLines.get(i + 1).getTimestamp();
                    if (currentTime.lessThan(next)) {
                        currentIndex = i;
                        break;
                    }
                } else {
                    currentIndex = i;
                }
            }
        }

        if (currentIndex != -1 && currentIndex != lastSyncedLabelIndex) {
            syncedXLyricsLabel.setText(syncedLyricLines.get(currentIndex).getText());
            lastSyncedLabelIndex = currentIndex;
            //Animation
            AnimationUtils.animateSyncedLyricTransition(syncedXLyricsLabel);
        }
    }


    private void displayLyrics(String lyrics) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        lyricsFlow.getChildren().clear();
        currentLyricLines.clear();

        if (lyrics == null || lyrics.isEmpty()) {
            Text noLyrics = new Text("NO LYRICS AVAILABLE");
            noLyrics.setStyle("-fx-fill: #ff6666; -fx-font-size: 16px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 4, 0.2, 2, 2);");
            lyricsFlow.getChildren().add(noLyrics);
            return;
        }

        List<LyricLine> lyricLines = parseLyrics(lyrics);
        currentLyricLines = lyricLines;

        if (lyricLines.isEmpty()) {
            // Plain text lyrics (no timestamps)
            String[] lines = lyrics.split("\n");
            for (String line : lines) {
                Text text = new Text(line + "\n");
                text.setStyle("""
                            -fx-fill: linear-gradient(to top right, #a674fc, #ff003a);
                            -fx-font-size: 14px;
                            -fx-font-weight: bold;
                            -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 4, 0.2, 2, 2);
                        """);

                text.wrappingWidthProperty().bind(lyricsFlow.widthProperty().subtract(20));
                text.setTextAlignment(TextAlignment.CENTER);
                lyricsFlow.getChildren().add(text);
            }
        } else {
            // Synced lyrics with timestamps
            for (LyricLine lyricLine : lyricLines) {
                Text text = new Text(lyricLine.getText() + "\n");
                text.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                text.wrappingWidthProperty().bind(lyricsFlow.widthProperty().subtract(20));
                text.setTextAlignment(TextAlignment.CENTER);

                // Make clickable
                text.setOnMouseClicked(e -> {
                    if (!lyricLine.getTimestamp().equals(curLyricTime))
                        playbackService.seek(lyricLine.getTimestamp());
                });

                // Hover effect
                text.setOnMouseEntered(e -> {
                    if (!lyricLine.getTimestamp().equals(curLyricTime))
                        text.setStyle("-fx-fill: linear-gradient(to bottom right, #ff5f6d, #ffc371); -fx-font-size: 14px; -fx-cursor: hand;");
                });

                text.setOnMouseExited(e -> {
                    if (!lyricLine.getTimestamp().equals(curLyricTime))
                        text.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                });

                lyricsFlow.getChildren().add(text);
            }
        }

        lyricsScrollPane.setVvalue(0);
    }

    public List<LyricLine> parseLyrics(String lrc) {
        List<LyricLine> result = new ArrayList<>();
        String[] lines = lrc.split("\n");

        for (String line : lines) {
            line = line.trim(); //Removes leading and trailing whitespace from the line.
            if (line.isEmpty()) continue;

            if (!line.startsWith("[") || !line.contains("]")) continue;

            int closeBracket = line.indexOf("]");
            String timePart = line.substring(1, closeBracket);
            String text = line.substring(closeBracket + 1).trim();

            if (text.isEmpty()) continue; // Skip empty lines

            if (line.matches("\\[(length|re|ve|ar|ti|al|by):.*\\]")) {
                continue;
            }

            String[] minSec = timePart.split(":");
            if (minSec.length != 2) continue;

            try {
                int minutes = Integer.parseInt(minSec[0]);
                double seconds = Double.parseDouble(minSec[1]);
                Duration timestamp = Duration.minutes(minutes).add(Duration.seconds(seconds));
                result.add(new LyricLine(timestamp, text));
            } catch (NumberFormatException e) {
                // ignore invalid lines
            }
        }
        return result;
    }

    Duration curLyricTime;

    public void highlightCurrentLyric(Duration currentTime) {
        if (!isLyricsActive || currentLyricLines.isEmpty()) {
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < currentLyricLines.size(); i++) {
            Duration lineTime = currentLyricLines.get(i).getTimestamp();
            curLyricTime = lineTime;
            if (currentTime.greaterThanOrEqualTo(lineTime)) {
                if (i < currentLyricLines.size() - 1) {
                    Duration nextLineTime = currentLyricLines.get(i + 1).getTimestamp();
                    if (currentTime.lessThan(nextLineTime)) {
                        currentIndex = i;
                        break;
                    }
                } else {
                    currentIndex = i;
                }
            }
        }

        if (currentIndex != lastHighlightedIndex && currentIndex >= 0) {

            if (lastHighlightedIndex >= 0 && lastHighlightedIndex < lyricsFlow.getChildren().size()) {
                Text prevText = (Text) lyricsFlow.getChildren().get(lastHighlightedIndex);
                prevText.setStyle("-fx-fill: white; -fx-font-size: 14px;");
            }

            // Highlight current
            if (currentIndex < lyricsFlow.getChildren().size()) {
                Text currentText = (Text) lyricsFlow.getChildren().get(currentIndex);
                AnimationUtils.scaleUp(currentText, 0.4, 1.1);
                currentText.setStyle("-fx-fill: linear-gradient(to top right, #a674fc, #ff003a); -fx-font-size: 14px; -fx-font-weight: bold;  -fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.6), 4, 0.2, 2, 2);");
                // Scroll ScrollPane to make current lyric visible
                double contentHeight = lyricsFlow.getHeight();
                double viewportHeight = lyricsScrollPane.getViewportBounds().getHeight();
                double y = currentText.getBoundsInParent().getMinY();

                double vValue = (y + currentText.getBoundsInParent().getHeight() / 2 - viewportHeight / 2) / (contentHeight - viewportHeight);
                vValue = Math.max(0, Math.min(vValue, 1));
                AnimationUtils.smoothScrollTo(lyricsScrollPane, vValue, 0.4);
                lyricsScrollPane.setVvalue(vValue);
            }

            lastHighlightedIndex = currentIndex;
        }
    }

    @Setter
    private MainController mainController;

    @FXML
    private void toggleLibraryView() throws SQLException {
        if (mainController != null) {
            mainController.toggleSidePanels(true);
        } else System.out.println("Main Controller is null");
    }

    @FXML
    private void toggleAlbumView() throws SQLException {
        if (mainController != null) mainController.toggleSidePanels(false);
        else System.out.println("Main Controller is null");
    }

    public void showLibraryButton(boolean enable) {
        if (!enable) {
            toggleLibraryButton.setVisible(false);
            toggleLibraryButton.setManaged(false);
            toggleLibraryButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), false);
        } else {
            toggleLibraryButton.setVisible(true);
            toggleLibraryButton.setManaged(true);
            toggleLibraryButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), true);
        }
    }

    public void hideShuffleButton() {
        shuffleButton.setVisible(false);
        shuffleButton.setManaged(false);
    }

    public void togglePlayPause(MediaStatus status) {
        Image image;
        if (status == MediaStatus.PLAYING) {
            image = new Image(Objects.requireNonNull(getClass().getResource("/icons/pauseButton.png")).toExternalForm());
            AnimationUtils.scaleDown(albumCover, 1, 0.3f);
        } else {
            image = new Image(Objects.requireNonNull(getClass().getResource("/icons/playButton.png")).toExternalForm());
            AnimationUtils.scaleDown(albumCover, 0.95, 0.3f);
        }
        playButtonImageView.setImage(image);
        playButton.setGraphic(playButtonImageView);
    }

    public void toggleLibraryButton(boolean enable) {
        toggleLibraryButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), enable);
    }

    public void toggleAlbumWindowButton(boolean enable) {
        toggleAlbumButton.pseudoClassStateChanged(PseudoClass.getPseudoClass("selected"), enable);
    }

}
