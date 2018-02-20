package com.iven.musicplayergo.utils;

public class AndroidVersion {

    public static boolean isMarshmallow() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
    }

    public static boolean isNougat() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N;
    }

    public static boolean isOreo() {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O;
    }
}
