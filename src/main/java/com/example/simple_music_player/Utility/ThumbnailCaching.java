package com.example.simple_music_player.Utility;

import com.example.simple_music_player.Model.Track;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.WeakHashMap;

import static com.example.simple_music_player.Controller.LibraryController.CARD_HEIGHT;
import static com.example.simple_music_player.Controller.LibraryController.CARD_WIDTH;

public class ThumbnailCaching {
    //compressed thumbnail loading
    private final WeakHashMap<String, Image> thumbnailCache = new WeakHashMap<>(); //does not need to explicitly mention to clean memory
    public Image loadThumbnail(Track track) {
        return thumbnailCache.computeIfAbsent(track.getPath(), k -> {
            try {
                byte[] artworkData = track.getArtworkData();
                if (artworkData != null) {
                    return new Image(
                            new ByteArrayInputStream(artworkData),
                            CARD_WIDTH,   // requested width
                            CARD_HEIGHT,   // requested height
                            true,  // preserve ratio
                            true   // smooth
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }

}
