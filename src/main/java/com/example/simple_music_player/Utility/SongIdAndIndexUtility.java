package com.example.simple_music_player.Utility;

import com.example.simple_music_player.Services.PlaybackService;

public class SongIdAndIndexUtility {
    public static int getSongIdFromIndex(int index) {
        return PlaybackService.getPlaylist().get(index);
    }

    public static int getIndexFromSongId(int idx) {
        return PlaybackService.playlist.indexOf(idx);
    }
}
