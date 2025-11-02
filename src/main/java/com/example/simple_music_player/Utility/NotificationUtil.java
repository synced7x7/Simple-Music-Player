package com.example.simple_music_player.Utility;

import com.example.simple_music_player.Controller.NowPlayingController;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class NotificationUtil {

    public static void alert(String str) {
        NowPlayingController npc = NowPlayingController.getInstance();
        Label nf = npc.getNotificationLabel();

        nf.setVisible(true);
        nf.setDisable(false);
        str  = "☢" +  str;
        String finalStr = str;
        Platform.runLater(() -> {
            nf.setText(finalStr);

            AnimationUtils.slideInFromRight(nf, 100f, 0.25f);
            AnimationUtils.whitePulse(nf);

            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> {
                TranslateTransition slideOut = new TranslateTransition(Duration.seconds(0.25), nf);
                slideOut.setFromX(0);
                slideOut.setToX(100);
                slideOut.setInterpolator(Interpolator.EASE_IN);
                slideOut.setOnFinished(ev -> {
                    nf.setVisible(false);
                    nf.setDisable(true);
                    nf.setTranslateX(0);
                });
                slideOut.play();
            });

            pause.play();
        });
    }
}

