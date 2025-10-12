package com.example.simple_music_player.Controller;

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
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.IOException;

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


    public static VisualizerService visualizerController;
    @Getter
    private static final PlaybackService playbackService = new PlaybackService(); //one instance to be shared among all
    private Boolean countdown = true;

    @FXML
    public void initialize() throws IOException {

        playbackService.setNowPlayingController(this);


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
            /*if (newT != null && visualizerController != null) {
                visualizerController.loadWaveform(new java.io.File(newT.getPath()));
            }*/
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

    public void clearScreen() {
        nameLabel.setText("Title");
        artistLabel.setText("Artist");
        albumLabel.setText("Album");
        formatLabel.setText("Format");
        bitRateLabel.setText("Bitrate");
        sampleRateLabel.setText("Samplerate");
        albumCover.setImage(null);
        //timeLabel is resetting in PlaybackService Class in play function by using listener
        System.out.println("Screen Cleared");
    }

    @FXML
    private void infoButtonHandler() {
        Stage infoStage = new Stage();
        infoStage.setTitle("About This App");

        Label appName = new Label("🎵 SIMPLE MUSIC PLAYER");
        appName.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Label version = new Label("Version 0.1.0");
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
                java.awt.Desktop.getDesktop().browse(new java.net.URI("https://github.com/synced7x7/Simple-Music-Player/tags"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }


}
