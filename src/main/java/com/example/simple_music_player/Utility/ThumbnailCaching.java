package com.example.simple_music_player.Utility;

import com.example.simple_music_player.Model.Track;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.util.WeakHashMap;

import static com.example.simple_music_player.Controller.LibraryController.CARD_WIDTH;

public class ThumbnailCaching {
    private final WeakHashMap<Integer, Image> thumbnailCache = new WeakHashMap<>();

    public Image loadThumbnail(Integer id, Track track) {
        if (track == null) return null;
        return thumbnailCache.computeIfAbsent(id, k -> {
            try {
                byte[] artworkData = track.getCompressedArtworkData();
                if (artworkData != null) {
                    return new Image(
                            new ByteArrayInputStream(artworkData),
                            CARD_WIDTH, CARD_WIDTH,
                            true, true
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        });
    }
}
