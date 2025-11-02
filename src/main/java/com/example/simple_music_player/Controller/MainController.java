package com.example.simple_music_player.Controller;


import com.example.simple_music_player.SimpleMusicPlayer;
import com.example.simple_music_player.Utility.WindowUtils;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.UserPrefRealtimeDAO;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;

import java.sql.SQLException;

public class MainController {
    @FXML
    private AnchorPane albumCoverPane;
    @FXML
    private AnchorPane libraryPane;
    @Setter
    @Getter
    Stage stage;
    UserPrefRealtimeDAO userPrefRealtimeDAO = new UserPrefRealtimeDAO(DatabaseManager.getConnection());
    NowPlayingController npc;
    private boolean isHiddenLibrary = false;
    private boolean isHiddenAlbum = false;

    @Getter
    private static MainController instance;

    public MainController() {
        instance = this;
    }

    @FXML
    private void initialize() {
        npc = NowPlayingController.getInstance();
        npc.setMainController(this);
        Platform.runLater(() -> {
            try {
                if (SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty()) {
                    isHiddenAlbum = !userPrefRealtimeDAO.getIsHiddenAlbum();
                    isHiddenLibrary = !userPrefRealtimeDAO.getIsHiddenLibrary();
                } else {
                    isHiddenAlbum = false;
                    isHiddenLibrary = false;
                }

                toggleSidePanels(true, 1);
                toggleSidePanels(false, 1);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            WindowUtils.makeDraggable(stage, npc.getTitleMainLabel());
        });
    }

    // Called by NowPlayingController
    public void toggleSidePanels(boolean isLibrary, int ini) throws SQLException {
        if (isLibrary) {
            isHiddenLibrary = !isHiddenLibrary;
            if (SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
                userPrefRealtimeDAO.setIsHiddenLibrary(isHiddenLibrary);
            libraryPane.setVisible(!isHiddenLibrary);
            libraryPane.setManaged(!isHiddenLibrary);
            npc.toggleLibraryButton(!isHiddenLibrary);
            System.out.println("LibraryView Pane Toggled");
        } else {
            isHiddenAlbum = !isHiddenAlbum;
            if (SimpleMusicPlayer.argument == null || SimpleMusicPlayer.argument.isEmpty())
                userPrefRealtimeDAO.setIsHiddenAlbum(isHiddenAlbum);
            albumCoverPane.setVisible(!isHiddenAlbum);
            albumCoverPane.setManaged(!isHiddenAlbum);
            npc.toggleAlbumWindowButton(!isHiddenAlbum);
            System.out.println("AlbumCoverPane Toggled");
        }

        if (stage != null) {
            double baseWidth = 465;
            double oldWidth = stage.getWidth();

            double newWidth = baseWidth;
            if (!isHiddenAlbum) newWidth += albumCoverPane.getWidth();
            if (!isHiddenLibrary) newWidth += libraryPane.getWidth();

            if (!isLibrary) {
                if (ini != 1) {
                    double delta = newWidth - oldWidth;
                    stage.setX(stage.getX() - delta);
                }
            }
            stage.setWidth(newWidth);
            if (ini == 1) centerStage(stage);
        }
    }

    private void centerStage(Stage stage) {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setX((screenBounds.getWidth() - stage.getWidth()) / 2);
        stage.setY((screenBounds.getHeight() - stage.getHeight()) / 2);
    }


}
