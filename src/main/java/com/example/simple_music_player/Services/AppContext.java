package com.example.simple_music_player.Services;

import lombok.Getter;

public class AppContext {
    @Getter
    private static final QueueService queueService = new QueueService();

}
