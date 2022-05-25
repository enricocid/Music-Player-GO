package com.iven.musicplayergo.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.UIControlInterface

object Permissions {

    @JvmStatic
    fun hasToAskForReadStoragePermission(activity: Activity) =
        Versioning.isMarshmallow() && ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED

    @JvmStatic
    fun manageAskForReadStoragePermission(
        activity: Activity
    ) {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        ) {

            MaterialAlertDialogBuilder(activity)
                .setCancelable(false)
                .setTitle(R.string.app_name)
                .setMessage(R.string.perm_rationale)
                .setPositiveButton(R.string.ok) { _, _ ->
                    askForReadStoragePermission(activity)
                }
                .setNegativeButton(R.string.no) { _, _ ->
                    (activity as UIControlInterface).onDenyPermission()
                }
                .show()
        } else {
            askForReadStoragePermission(
                activity
            )
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun askForReadStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            GoConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE
        )
    }
}
