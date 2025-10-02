package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.Services.VisualizerService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import lombok.Getter;

import java.io.IOException;

public class NowPlayingController {
    @FXML private Button playButton;
    @FXML private Button nextButton;
    @FXML private Button prevButton;
    @FXML private ImageView albumCover;
    @FXML private Label nameLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label bitRateLabel;
    @FXML private Label formatLabel;
    @FXML private Label timeLabel;
    @FXML private Label sampleRateLabel;
    @FXML private AnchorPane visualizerHolder;

    public static VisualizerService visualizerController;
    @Getter
    private static final PlaybackService playbackService = new PlaybackService(); //one instance to be shared among all
    private Boolean countdown = true;

    @FXML
    public void initialize() throws IOException {

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
                visualizerController.loadWaveform(new java.io.File(newT.getPath()));
            }
        });

        playButton.setOnAction(e -> playbackService.togglePlayPause());
        nextButton.setOnAction(e -> playbackService.next());
        prevButton.setOnAction(e -> playbackService.previous());

        //Time
        timeLabel.textProperty().bind(playbackService.remainingTimeProperty());
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
            if (newT.getCover() != null) {
                albumCover.setImage(newT.getCover());
            } else {
                albumCover.setImage(null);
            }

        });

    }


    @FXML
    public void toggleCountdown() {
        countdown = !countdown;
        System.out.println("countdown: " + countdown);
        if (countdown) {
            timeLabel.textProperty().bind(playbackService.remainingTimeProperty());
        } else {
            timeLabel.textProperty().bind(playbackService.elapsedTimeProperty());
        }
    }

}
