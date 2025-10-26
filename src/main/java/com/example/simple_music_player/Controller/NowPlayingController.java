package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.LyricLine;
import com.example.simple_music_player.Model.SongLocator;
import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Model.UserPref;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Services.VisualizerService;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
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
    private Button infoButton;
    @FXML
    private Button repeatButton;
    @FXML
    private Button shuffleButton;
    @FXML
    private Slider volumeSlider;
    @FXML
    private ImageView backAlbumCover;
    @FXML
    private Button lyricsButton;
    @FXML
    private ScrollPane lyricsScrollPane;
    @FXML
    private VBox lyricsFlow;

    private List<LyricLine> currentLyricLines = new ArrayList<>();
    public static VisualizerService visualizerController;
    @Getter
    private static final PlaybackService playbackService = new PlaybackService(); //one instance to be shared among all
    @Getter
    private static NowPlayingController instance;  // static reference
    private int lastHighlightedIndex = -1;

    public NowPlayingController() {
        instance = this;   // set when FXML is loaded
    }

    private boolean isLyricsActive = false;

    @FXML
    public void initialize() throws IOException {
        playbackService.setNowPlayingController(this);
        lyricsScrollPane.setVisible(false);
        lyricsScrollPane.setManaged(false);

        // Bind slider to PlaybackService
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            playbackService.setVolume(newVal.doubleValue());
            UserPref.volume = newVal.doubleValue();
        });
        //

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/simple_music_player/visualizer.fxml"));
        AnchorPane visualizer = loader.load();
        visualizerController = loader.getController();
        visualizerHolder.getChildren().add(visualizer);

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
                albumCover.setImage(null);
                backAlbumCover.setImage(null);
            }

            if (isLyricsActive) {
                try {
                    displayLyrics(newT.getLyrics());
                } catch (CannotReadException | TagException | InvalidAudioFrameException | ReadOnlyFileException |
                         IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
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

        Label version = new Label("Version 0.2.0");
        Label author = new Label("Developed by: synced_x_");
        Label mail = new Label("synced7x7@gmail.com");
        Label credits = new Label("Releases");
        credits.setStyle("-fx-text-fill: blue; -fx-underline: true;");
        credits.setCursor(Cursor.HAND);

        VBox root = new VBox(10, appName, version, author, mail, credits);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(15));

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
                Desktop.getDesktop().browse(new URI("https://github.com/synced7x7/Simple-Music-Player/tags"));
            } catch (Exception ex) {
                ex.printStackTrace();
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
        if (UserPref.shuffle == 1) {
            shuffleButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        } else {
            shuffleButton.setStyle("");
        }
    }

    public void updateRepeatButtonStyle() {
        if (UserPref.repeat == 1) {
            repeatButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        } else {
            repeatButton.setStyle("");
        }
    }

    @FXML
    private void toggleLyrics() throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        isLyricsActive = !isLyricsActive;
        albumCover.setVisible(!isLyricsActive);
        albumCover.setManaged(!isLyricsActive);
        lyricsScrollPane.setVisible(isLyricsActive);
        lyricsScrollPane.setManaged(isLyricsActive);

        if (isLyricsActive && playbackService.getCurrentTrack() != null) {
            Track currentTrack = playbackService.getCurrentTrack();
            displayLyrics(currentTrack.getLyrics());
           // System.out.println("Lyrics: " + currentTrack.getLyrics());
        } else {
            currentLyricLines.clear();
            lastHighlightedIndex = -1;
        }
    }

    private void displayLyrics(String lyrics) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        lyricsFlow.getChildren().clear();
        currentLyricLines.clear();

        if (lyrics == null || lyrics.isEmpty()) {
            Text noLyrics = new Text("No lyrics available");
            noLyrics.setStyle("-fx-fill: red; -fx-font-size: 14px;");
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
                text.setStyle("-fx-fill: white; -fx-font-size: 14px;");
                lyricsFlow.getChildren().add(text);
            }
        } else {
            // Synced lyrics with timestamps
            for (LyricLine lyricLine : lyricLines) {
                Text text = new Text(lyricLine.getText() + "\n");
                text.setStyle("-fx-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");

                // Make clickable
                text.setOnMouseClicked(e -> {
                    playbackService.seek(lyricLine.getTimestamp());
                });

                // Hover effect
                text.setOnMouseEntered(e -> {
                    text.setStyle("-fx-fill: #4CAF50; -fx-font-size: 14px; -fx-cursor: hand; -fx-underline: true;");
                });

                text.setOnMouseExited(e -> {
                    text.setStyle("-fx-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
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


    public void highlightCurrentLyric(Duration currentTime) {
        if (!isLyricsActive || currentLyricLines.isEmpty()) {
            return;
        }

        int currentIndex = -1;
        for (int i = 0; i < currentLyricLines.size(); i++) {
            Duration lineTime = currentLyricLines.get(i).getTimestamp();

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
                prevText.setStyle("-fx-fill: white; -fx-font-size: 14px; -fx-cursor: hand;");
            }

            // Highlight current
            if (currentIndex < lyricsFlow.getChildren().size()) {
                Text currentText = (Text) lyricsFlow.getChildren().get(currentIndex);
                currentText.setStyle("-fx-fill: #4CAF50; -fx-font-size: 16px; -fx-font-weight: bold; -fx-cursor: hand;");

                // Scroll ScrollPane to make current lyric visible
                double contentHeight = lyricsFlow.getHeight();
                double viewportHeight = lyricsScrollPane.getViewportBounds().getHeight();
                double y = currentText.getBoundsInParent().getMinY();

                double vValue = (y + currentText.getBoundsInParent().getHeight() / 2 - viewportHeight / 2) / (contentHeight - viewportHeight);
                vValue = Math.max(0, Math.min(vValue, 1));

                lyricsScrollPane.setVvalue(vValue);
            }

            lastHighlightedIndex = currentIndex;
        }
    }


}
