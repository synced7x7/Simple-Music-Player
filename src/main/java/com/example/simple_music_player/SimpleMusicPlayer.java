package com.example.simple_music_player;

import com.example.simple_music_player.db.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SimpleMusicPlayer extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        DatabaseManager.initialize();

        FXMLLoader fxmlLoader = new FXMLLoader(SimpleMusicPlayer.class.getResource("main.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setResizable(false);
        stage.setTitle("Simple Music Player");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void stop() {
        DatabaseManager.close();
    }
}