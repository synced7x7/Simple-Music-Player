package com.example.simple_music_player.Services;

import com.example.simple_music_player.Controller.LibraryController;
import lombok.Getter;

import java.util.LinkedList;

public class QueueService {
    @Getter
    private final LinkedList<Integer> queueList = new LinkedList<>();

    public void addToQueue(int songId) {
            queueList.addLast(songId);
            System.out.println("Added to queue: " + songId);
            refreshSongListView();
    }

    public void removeFromQueue(int songId) {
        queueList.remove(Integer.valueOf(songId));
        System.out.println("Removed from queue: " + songId);
        refreshSongListView();
    }

    public void clearQueue() {
        queueList.clear();
        System.out.println("Queue cleared");
        refreshSongListView();
    }

    private void refreshSongListView () {
        LibraryController libraryController = LibraryController.getInstance();
        libraryController.getSongListView().refresh();
    }
}
