package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Model.Track;
import com.example.simple_music_player.Services.PlaybackService;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;

public class NowPlayingController {

    private Boolean countdown = true;

    @FXML private Button playButton;
    @FXML private Button nextButton;
    @FXML private Button prevButton;
    @FXML private ProgressBar nowPlayingBar;
    @FXML private ImageView albumCover;
    @FXML private Label nameLabel;
    @FXML private Label artistLabel;
    @FXML private Label albumLabel;
    @FXML private Label bitRateLabel;
    @FXML private Label formatLabel;
    @FXML private Label timeLabel;
    @FXML private Label sampleRateLabel;

    private final PlaybackService playbackService = new PlaybackService();

    @FXML
    public void initialize() {
        playButton.setOnAction(e -> playbackService.togglePlayPause());
        nextButton.setOnAction(e -> playbackService.next());
        prevButton.setOnAction(e -> playbackService.previous());

        nowPlayingBar.progressProperty().bind(playbackService.progressProperty()); //bind updates nowPlayingBar base on progress property it is getting
        //Time
        timeLabel.textProperty().bind(playbackService.remainingTimeProperty());
        //Song Metadata
        playbackService.currentTrackProperty().addListener((obs, oldT, newT) -> {
            //Name
            nameLabel.setText(newT.getTitle());
            //Artist
            if(newT.getArtist() != null)    artistLabel.setText(newT.getArtist()); else artistLabel.setText("Unknown");
            //Album
            if(newT.getAlbum() != null)    albumLabel.setText(newT.getAlbum()); else albumLabel.setText("Unknown");
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

        playbackService.setPlaylist(
                java.util.List.of(
                        new Track("C:/music/song1.mp3"),
                        new Track("C:/music/song2.mp3"),
                        new Track("C:/music/song3.mp3"),
                        new Track("C:/music/song4.mp3")
                )
        );
    }

    @FXML
    public void toggleCountdown(){
        countdown = !countdown;
        System.out.println("countdown: " + countdown);
        if(countdown){
            timeLabel.textProperty().bind(playbackService.remainingTimeProperty());
        } else {
            timeLabel.textProperty().bind(playbackService.elapsedTimeProperty());
        }
    }
}
