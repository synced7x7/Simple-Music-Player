package com.example.simple_music_player.Model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class SongLocator {
    private String lastSortBS;
    private int lastReverseBS;

    private static SongLocator currentInstance;

    public static void create(String sort, int reverse) {
        currentInstance = new SongLocator(sort, reverse);
    }

    public static void delete() {
        currentInstance = null;
    }

    public static SongLocator getCurrent() {
        return currentInstance;
    }
}
