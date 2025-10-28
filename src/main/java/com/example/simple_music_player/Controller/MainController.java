package com.example.simple_music_player.Controller;


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

public class MainController {
    @FXML
    private SplitPane mainSplitPane;
    @FXML
    private AnchorPane albumCoverPane;
    @FXML
    private AnchorPane libraryPane;

    private boolean isHiddenLibrary = false;
    private boolean isHiddenAlbum = false;

    @Getter
    private static MainController instance;
    public MainController() { instance = this; }

    @FXML
    private void initialize() throws IOException {
        NowPlayingController npc = NowPlayingController.getInstance();
        npc.setMainController(this);
    }

    @Setter
    Stage stage;

    // Called by NowPlayingController
    public void toggleSidePanels(boolean isLibrary) {
        if (isLibrary) {
            isHiddenLibrary = !isHiddenLibrary;
            libraryPane.setVisible(!isHiddenLibrary);
            libraryPane.setManaged(!isHiddenLibrary);
            System.out.println("LibraryView Pane Toggled");
        } else {
            isHiddenAlbum = !isHiddenAlbum;
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
