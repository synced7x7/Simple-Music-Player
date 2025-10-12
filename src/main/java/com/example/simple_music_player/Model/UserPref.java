package com.example.simple_music_player.Model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserPref {
    public static int playlistNo;
    public static long timestamp;
    public static String status;
    public static String sortingPref;
    public static int reverse;

    public static void setUserPref(int playlistNo, long timestamp, String status, String sortingPref, int reverse) {
        UserPref.playlistNo = playlistNo;
        UserPref.timestamp = timestamp;
        UserPref.status = status;
        UserPref.sortingPref = sortingPref;
        UserPref.reverse = reverse;
    }
}


