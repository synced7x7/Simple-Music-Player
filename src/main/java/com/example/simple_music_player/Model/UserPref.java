package com.example.simple_music_player.Model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class UserPref {
    public static int playlistNo;
    public static long timestamp;
    public static String status;
    public static String sortingPref;
    public static int reverse;
    public static int repeat;
    public static int shuffle;
    public static int isRundown;

    public static void setUserPref(int playlistNo, long timestamp, String status, String sortingPref, int reverse , int repeat, int shuffle, int isRundown) {
        UserPref.playlistNo = playlistNo;
        UserPref.timestamp = timestamp;
        UserPref.status = status;
        UserPref.sortingPref = sortingPref;
        UserPref.reverse = reverse;
        UserPref.repeat = repeat;
        UserPref.shuffle = shuffle;
        UserPref.isRundown = isRundown;
    }

    public static void userPrefChecker () {
        System.out.println("UserPrefStatus::");
        System.out.println("playlistNo: " + UserPref.playlistNo);
        System.out.println("timestamp: " + UserPref.timestamp);
        System.out.println("status: " + UserPref.status);
        System.out.println("sortingPref: " + UserPref.sortingPref);
        System.out.println("reverse: " + UserPref.reverse);
        System.out.println("repeat: " + UserPref.repeat);
        System.out.println("shuffle: " + UserPref.shuffle);
        System.out.println("isRundown: " + UserPref.isRundown);
    }
}


