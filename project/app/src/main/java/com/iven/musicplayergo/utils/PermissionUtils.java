package com.iven.musicplayergo.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.support.annotation.NonNull;

import com.iven.musicplayergo.R;

public class PermissionUtils {

    public static void show(@NonNull final Activity activity) {
        AlertDialog builder = new AlertDialog.Builder(activity).create();

        builder.setIcon(R.drawable.ic_sd_card);
        builder.setTitle(activity.getString(R.string.app_name));
        builder.setMessage(activity.getString(R.string.perm_rationale));

        builder.setButton(AlertDialog.BUTTON_POSITIVE, activity.getString(android.R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        requestReadPermission(activity);
                    }
                });
        builder.setCanceledOnTouchOutside(false);
        try {
            builder.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @TargetApi(23)
    private static void requestReadPermission(Activity activity) {
        final int READ_FILES_CODE = 2588;
        activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                , READ_FILES_CODE);
    }
}