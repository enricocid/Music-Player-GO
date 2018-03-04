package com.iven.musicplayergo.utils;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import com.iven.musicplayergo.R;


public class PermissionDialog extends DialogFragment {

    private static boolean mError;
    private TextView mRationale;

    public PermissionDialog() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static PermissionDialog newInstance() {
        return new PermissionDialog();
    }

    public static void showPermissionDialog(FragmentActivity activity, boolean error) {
        mError = error;
        FragmentManager fm = activity.getSupportFragmentManager();
        newInstance().show(fm, "permission");
    }

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        View dialogView = View.inflate(getActivity(), R.layout.permission_layout, null);
        builder.setView(dialogView);
        mRationale = dialogView.findViewById(R.id.rationale);

        mRationale.setText(getString(R.string.perm_rationale));

        if (mError) {
            notifyError();
        }


        builder.setTitle(getString(R.string.app_name));

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                requestReadPermission();
            }
        });

        builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
                showPermissionDialog(getActivity(), true);
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

    private void notifyError() {

        ObjectAnimator a = ObjectAnimator.ofInt(mRationale, "textColor", mRationale.getCurrentTextColor(), ContextCompat.getColor(getActivity(), R.color.red_A400));

        a.setInterpolator(new LinearInterpolator());
        a.setDuration(1000);
        a.setRepeatCount(ValueAnimator.RESTART);
        a.setRepeatMode(ValueAnimator.REVERSE);
        a.setEvaluator(new ArgbEvaluator());
        AnimatorSet t = new AnimatorSet();
        t.play(a);
        t.start();
    }
}