package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.LibraryController;
import com.example.simple_music_player.Model.Playlist;
import com.example.simple_music_player.Utility.NotificationUtil;
import com.example.simple_music_player.db.DatabaseManager;
import com.example.simple_music_player.db.PlaylistsDAO;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

public class PlaylistService {
    private final PlaylistsDAO playlistsDAO = new PlaylistsDAO(DatabaseManager.getConnection());
    LibraryController libraryController = LibraryController.getInstance();

    public void openPlaylistSelectionWindow(List<Integer> songIds) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Add to Playlist");
        if (songIds.size() == 1 && songIds.getFirst() == -1) {
            stage.setTitle("Playlist Manager");
        }


        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label title = new Label("Select a Playlist");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        // Container for playlist items
        VBox playlistContainer = new VBox(5);
        playlistContainer.setPadding(new Insets(5));

        // ScrollPane to make playlists scrollable
        ScrollPane scrollPane = new ScrollPane(playlistContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(300); // Max height before scrolling
        scrollPane.setStyle("-fx-background-color: transparent;");

        // Load existing playlists
        loadPlaylists(playlistContainer, songIds);

        Button createNewBtn = new Button("Create New Playlist");
        createNewBtn.setPrefWidth(360);
        createNewBtn.setOnAction(e -> openCreatePlaylistWindow(stage, playlistContainer, songIds));

        root.getChildren().addAll(title, scrollPane, createNewBtn);

        Scene scene = new Scene(root, 400, 450);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    private void loadPlaylists(VBox playlistContainer, List<Integer> songIds) {
        playlistContainer.getChildren().clear();

        try {
            List<Playlist> playlists = playlistsDAO.getAllPlaylistsAboveId(1);

            if (playlists.isEmpty()) {
                Label emptyLabel = new Label("No playlists yet. Create one!");
                emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                playlistContainer.getChildren().add(emptyLabel);
                return;
            }

            for (Playlist playlist : playlists) {
                HBox row = new HBox(10);
                row.setPadding(new Insets(5));
                row.setStyle("-fx-background-color: #f5f5f5; -fx-background-radius: 5;");

                Label name = new Label(playlist.getName());
                name.setPrefWidth(200);
                name.setWrapText(true);
                name.setStyle("-fx-font-size: 14px;");

                TextField editField = new TextField(playlist.getName());
                editField.setVisible(false);
                editField.setPrefWidth(200);

                Button addBtn = new Button("✚");
                addBtn.setMinWidth(35);
                addBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
                if (songIds.size() == 1 && songIds.getFirst() == -1) {
                    addBtn.setVisible(false);
                    addBtn.setDisable(true);
                }

                Button editBtn = new Button("✎");
                editBtn.setMinWidth(35);

                Button deleteBtn = new Button("🗑");
                deleteBtn.setMinWidth(35);
                deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");

                if (playlist.getId() < 4) {
                    addBtn.setVisible(false);
                    addBtn.setDisable(true);
                    editBtn.setVisible(false);
                    editBtn.setDisable(true);
                    deleteBtn.setVisible(false);
                    deleteBtn.setDisable(true);
                }

                name.setOnMouseClicked(e -> {
                    if (songIds.size() == 1 && songIds.getFirst() == -1) {
                        System.out.println("Loading playlist: " + name.getText());
                        if (libraryController != null) {
                            try {
                                libraryController.loadPlaylistView(playlist.getId());
                            } catch (SQLException ex) {
                                throw new RuntimeException(ex);
                            }
                            Stage currentStage = (Stage) name.getScene().getWindow();
                            currentStage.close();
                        }
                    }
                });

                addBtn.setOnAction(e -> {
                    try {
                        playlistsDAO.insertSongsInPlaylist(playlist.getId(), songIds);
                        System.out.println("Added song " + songIds + " to playlist: " + playlist.getName());
                        NotificationUtil.alert("Added songs to playlist. Duplicated songs will be ignored." );
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                });

                editBtn.setOnAction(e -> {
                    editField.setText(name.getText());
                    name.setVisible(false);
                    editField.setVisible(true);
                    editField.requestFocus();
                    editField.selectAll();
                });

                editField.setOnAction(ev -> {
                    String newName = editField.getText().trim();
                    if (!newName.isEmpty() && !newName.equals(playlist.getName())) {
                        try {
                            playlistsDAO.renamePlaylist(playlist.getId(), newName);
                            playlist.setName(newName);
                            name.setText(newName);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    editField.setVisible(false);
                    name.setVisible(true);
                });

                // Handle focus lost (cancel edit)
                editField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                    if (!isNowFocused && editField.isVisible()) {
                        editField.setVisible(false);
                        name.setVisible(true);
                    }
                });

                deleteBtn.setOnAction(e -> {
                    try {
                        playlistsDAO.deletePlaylist(playlist.getId());
                        playlistContainer.getChildren().remove(row);

                        // Show empty message if no playlists left
                        if (playlistContainer.getChildren().isEmpty()) {
                            Label emptyLabel = new Label("No playlists yet. Create one!");
                            emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-style: italic;");
                            playlistContainer.getChildren().add(emptyLabel);
                        }
                    } catch (SQLException ex) {
                        throw new RuntimeException();
                    }
                });

                row.getChildren().addAll(name, editField, addBtn, editBtn, deleteBtn);
                playlistContainer.getChildren().add(row);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void openCreatePlaylistWindow(Stage parentStage, VBox playlistContainer, List<Integer> songIds) {
        Stage stage = new Stage();
        stage.initOwner(parentStage);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Create Playlist");

        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label instruction = new Label("Enter playlist name:");
        instruction.setStyle("-fx-font-size: 14px;");

        TextField nameField = new TextField();
        nameField.setPromptText("Playlist name");
        nameField.setPrefWidth(260);

        HBox buttonBox = new HBox(10);
        Button createBtn = new Button("✔ Create");
        createBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");

        Button cancelBtn = new Button("✖ Cancel");

        createBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            if (!name.isEmpty()) {
                try {
                    playlistsDAO.createPlaylist(name);
                    loadPlaylists(playlistContainer, songIds);
                    NotificationUtil.alert("Created playlist with name: " + name);
                    stage.close();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        cancelBtn.setOnAction(e -> stage.close());
        nameField.setOnAction(e -> createBtn.fire());

        buttonBox.getChildren().addAll(createBtn, cancelBtn);
        root.getChildren().addAll(instruction, nameField, buttonBox);

        Scene scene = new Scene(root, 300, 150);
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();

        nameField.requestFocus();
    }


}