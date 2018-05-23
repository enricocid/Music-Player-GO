package com.iven.musicplayergo.utils;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;

import com.iven.musicplayergo.R;

public class PermissionDialog extends DialogFragment {

    private static final String DIALOG_TAG = "com.iven.musicplayergo.permissions";

    public PermissionDialog() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    private static PermissionDialog newInstance() {
        return new PermissionDialog();
    }

    public static void show(@NonNull FragmentManager fm) {
        newInstance().show(fm, DIALOG_TAG);
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle(getString(R.string.app_name));
        builder.setMessage(getString(R.string.perm_rationale));

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                requestReadPermission();
            }
        });

        builder.setIcon(R.mipmap.ic_launcher);
        Dialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    @TargetApi(23)
    private void requestReadPermission() {
        final int READ_FILES_CODE = 2588;
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}
                , READ_FILES_CODE);
    }
}