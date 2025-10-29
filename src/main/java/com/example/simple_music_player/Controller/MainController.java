package com.example.simple_music_player.Controller;


import com.example.simple_music_player.SimpleMusicPlayer;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.UserPrefRealtimeDAO;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class MainController {
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private AnchorPane albumCoverPane;
    @FXML
    private AnchorPane libraryPane;
    @Setter
    Stage stage;
    UserPrefRealtimeDAO userPrefRealtimeDAO = new UserPrefRealtimeDAO(DatabaseManager.getConnection());

    private boolean isHiddenLibrary = false;
    private boolean isHiddenAlbum = false;

    @Getter
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    @FXML
    private void initialize() {
        NowPlayingController npc = NowPlayingController.getInstance();
        npc.setMainController(this);
        Platform.runLater(() -> {
            try {
                if(SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty()) {
                    isHiddenAlbum = !userPrefRealtimeDAO.getIsHiddenAlbum();
                    isHiddenLibrary = !userPrefRealtimeDAO.getIsHiddenLibrary();
                } else {
                    isHiddenAlbum = false;
                    isHiddenLibrary = false;
                }

                toggleSidePanels(true);
                toggleSidePanels(false);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Called by NowPlayingController
    public void toggleSidePanels(boolean isLibrary) throws SQLException {
        if (isLibrary) {
            isHiddenLibrary = !isHiddenLibrary;
            if(SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
                userPrefRealtimeDAO.setIsHiddenLibrary(isHiddenLibrary);
            libraryPane.setVisible(!isHiddenLibrary);
            libraryPane.setManaged(!isHiddenLibrary);
            System.out.println("LibraryView Pane Toggled");
        } else {
            isHiddenAlbum = !isHiddenAlbum;
            if(SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
                userPrefRealtimeDAO.setIsHiddenAlbum(isHiddenAlbum);
            albumCoverPane.setVisible(!isHiddenAlbum);
            albumCoverPane.setManaged(!isHiddenAlbum);
            System.out.println("AlbumCoverPane Toggled");
        }

        if (stage != null) {
            double targetWidth = 465; // NowPlaying width
            if (!isHiddenLibrary || !isHiddenAlbum) {
                if (!isHiddenAlbum) targetWidth += albumCoverPane.getWidth();
                if (!isHiddenLibrary) targetWidth += libraryPane.getWidth();
            }
            stage.setWidth(targetWidth);
            centerStage(stage);
        } else {
            System.out.println("Stage is null");
        }
    }

    private void centerStage(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }

}
