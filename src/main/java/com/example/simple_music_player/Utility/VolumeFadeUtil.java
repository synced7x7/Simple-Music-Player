package com.example.simple_music_player.Utility;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

public class VolumeFadeUtil {
    public static void fadeIn(MediaPlayer player, double targetVolume, double durationSeconds) {
        if (player == null) return;

        // Ensure valid bounds
        targetVolume = Math.max(0, Math.min(1, targetVolume));

        player.setVolume(0.0); // start from silence

        Timeline fadeIn = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(player.volumeProperty(), 0.0)),
                new KeyFrame(Duration.seconds(durationSeconds), new KeyValue(player.volumeProperty(), targetVolume))
        );

        fadeIn.play();
    }

    /**
     * Smoothly fades out the volume from current level to 0.
     *
     * @param player          The MediaPlayer instance.
     * @param durationSeconds Duration of the fade in seconds.
     */
    public static void fadeOut(MediaPlayer player, double durationSeconds) {
        if (player == null) return;

        double currentVolume = player.getVolume();

        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.seconds(0), new KeyValue(player.volumeProperty(), currentVolume)),
                new KeyFrame(Duration.seconds(durationSeconds), new KeyValue(player.volumeProperty(), 0.0))
        );

        fadeOut.play();
    }
}
