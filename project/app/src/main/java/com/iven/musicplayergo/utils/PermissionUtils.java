package com.iven.musicplayergo.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.widget.Toast;

import com.iven.musicplayergo.R;

public class PermissionUtils {

    @TargetApi(23)
    public static void requestReadPermission(Activity activity) {
        final int READ_FILES_CODE = 2588;
        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                , READ_FILES_CODE);
    }

    public static void notifyFail(Activity activity) {

        Toast.makeText(activity, activity.getString(R.string.fail), Toast.LENGTH_SHORT).show();
        activity.finish();
    }
}
