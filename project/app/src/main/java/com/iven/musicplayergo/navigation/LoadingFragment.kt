package com.iven.musicplayergo.navigation

import android.Manifest
import android.annotation.TargetApi
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.MusicViewModel
import com.iven.musicplayergo.R
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.interfaces.UIControlInterface

class LoadingFragment : Fragment(R.layout.fragment_loading_fragment) {

    private var mUIControlInterface: UIControlInterface? = null

    // View model
    private val mMusicViewModel: MusicViewModel by viewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (hasToAskForReadStoragePermission()) {
            manageAskForReadStoragePermission()
        } else {
            getMusic()
        }
    }

    private fun getMusic() {
        mMusicViewModel.deviceMusic.observe(requireActivity(), Observer { returnedMusic ->
            if (!returnedMusic.isNullOrEmpty()) {
                findNavController().navigate(R.id.mainFragment)
            } else {
                mUIControlInterface?.onError(GoConstants.TAG_NO_MUSIC)
            }
        })
        mMusicViewModel.getDeviceMusic()
    }

    // Manage request permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            GoConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Permission was granted, yay! Do bind service
                    getMusic()
                } else {
                    // Permission denied, boo! Error!
                    mUIControlInterface?.onError(GoConstants.TAG_NO_PERMISSION)
                }
            }
        }
    }

    private fun hasToAskForReadStoragePermission() =
        VersioningHelper.isMarshMallow() && ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED

    private fun manageAskForReadStoragePermission() {

        if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {

            MaterialDialog(requireContext()).show {

                cancelOnTouchOutside(false)

                title(R.string.app_name)

                message(R.string.perm_rationale)
                positiveButton(android.R.string.ok) {
                    askForReadStoragePermission()
                }
                negativeButton {
                    mUIControlInterface?.onError(GoConstants.TAG_NO_PERMISSION)
                }
            }
        } else {
            askForReadStoragePermission()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    private fun askForReadStoragePermission() {
        requestPermissions(
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
            GoConstants.PERMISSION_REQUEST_READ_EXTERNAL_STORAGE
        )
    }
}
