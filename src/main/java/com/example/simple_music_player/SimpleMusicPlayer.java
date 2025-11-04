package com.example.simple_music_player;

import com.example.simple_music_player.Controller.MainController;
import com.example.simple_music_player.Controller.NowPlayingController;
import com.example.simple_music_player.Services.PlaybackService;
import com.example.simple_music_player.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SimpleMusicPlayer extends Application {

    PlaybackService playbackService = NowPlayingController.getPlaybackService();
    public static List<String> argument = new ArrayList<>();

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.initialize();
        stage.initStyle(StageStyle.TRANSPARENT);
        FXMLLoader fxmlLoader = new FXMLLoader(SimpleMusicPlayer.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        //css loader
        getStyleSheets(scene);
        loadFont();
        //
        scene.setFill(Color.TRANSPARENT);
        stage.setResizable(false);
        stage.setTitle("Simple Music Player");
        stage.setScene(scene);
        stage.show();

        //Call any stage using mainController if needed. Don't forger
        MainController mainController = MainController.getInstance();
        mainController.setStage(stage);
    }

    private void loadFont() {
        Font.loadFont(getClass().getResourceAsStream("/fonts/chiffon-trial-semibold.ttf"), 14);
    }

    private void getStyleSheets(Scene scene) {
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/base.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/components.css")).toExternalForm());
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/styles/LibraryComponents.css")).toExternalForm());
    }

    public static void main(String[] args) {
        argument = List.of(args);
        launch();
    }

    @Override
    public void stop() throws SQLException {
        if (playbackService != null) {
            playbackService.closePlaybackService();
        }
        DatabaseManager.close();
    }
}