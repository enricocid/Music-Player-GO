package com.iven.musicplayergo.helpers

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.UIControlInterface

object PermissionsHelper {

    @JvmStatic
    fun hasToAskForReadStoragePermission(activity: Activity) =
        VersioningHelper.isMarshmallow() && ContextCompat.checkSelfPermission(
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

            MaterialDialog(activity).show {

                cancelOnTouchOutside(false)

                title(R.string.app_name)

                message(R.string.perm_rationale)
                positiveButton(android.R.string.ok) {
                    askForReadStoragePermission(
                        activity
                    )
                }
                negativeButton {
                    (activity as UIControlInterface).onDenyPermission()
                }
            }
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
