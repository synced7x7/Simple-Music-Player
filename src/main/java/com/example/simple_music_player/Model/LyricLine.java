package com.example.simple_music_player.Model;
import javafx.util.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class LyricLine {
    private final Duration timestamp; // JavaFX Duration
    private final String text;

}

