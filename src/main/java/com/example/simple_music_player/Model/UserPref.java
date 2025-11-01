package com.example.simple_music_player.Model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserPref {
    public static int playlistNo;
    public static long timestamp;
    public static String status;
    public static int repeat;
    public static int shuffle;
    public static int isRundown;
    public static double volume;
    public static int playlistId;

    public static void setUserPref(int playlistNo, long timestamp, String status, int repeat, int shuffle, int isRundown, double volume, int playlistId) {
        UserPref.playlistNo = playlistNo;
        UserPref.timestamp = timestamp;
        UserPref.status = status;
        UserPref.repeat = repeat;
        UserPref.shuffle = shuffle;
        UserPref.isRundown = isRundown;
        UserPref.volume = volume;
        UserPref.playlistId = playlistId;
    }
}


