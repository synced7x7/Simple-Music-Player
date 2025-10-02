package com.example.simple_music_player.Controller;

import com.example.simple_music_player.Services.PlaybackService;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;

public class AlbumCoverController {
    @FXML
    private ImageView coverFront;
    @FXML
    private ImageView coverBack;

    PlaybackService playbackService = NowPlayingController.getPlaybackService();

    private final double screenAR = 0.608;

    @FXML
    private void initialize() {
        playbackService.currentTrackProperty().addListener((obs, oldT, newT) -> {
            //Image fill algorithm by preserving aspect ratio
            double coverAR = newT.getCoverWidth() / newT.getCoverHeight();
            if (coverAR < screenAR) {
                coverBack.setFitHeight(-1);
                coverBack.setFitWidth(456);
            } else {
                coverBack.setFitHeight(750);
                coverBack.setFitWidth(-1);
            }
            //

            if (newT.getCover() != null) {
                coverFront.setImage(newT.getCover());
                coverBack.setImage(newT.getCover());
            } else {
                coverFront.setImage(null);
                coverBack.setImage(null);
            }
        });
    }

}
