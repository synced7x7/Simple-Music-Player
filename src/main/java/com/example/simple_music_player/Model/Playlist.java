package com.example.simple_music_player.Model;

import lombok.Getter;
import lombok.Setter;

@Getter
public class Playlist {
    private final int id;
    @Setter
    private String name;

    public Playlist(int id, String name) {
        this.id = id;
        this.name = name;
    }
}

