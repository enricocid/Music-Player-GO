package com.iven.musicplayergo.navigation

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentErrorBinding
import com.iven.musicplayergo.extensions.decodeColor
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.interfaces.UIControlInterface

class ErrorFragment : Fragment(R.layout.fragment_error) {

    private lateinit var mErrorFragmentBinding: FragmentErrorBinding
    private var mUIControlInterface: UIControlInterface? = null

    private var mErrorString = R.string.perm_rationale
    private var mErrorIcon = R.drawable.ic_folder

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(GoConstants.TAG_ERROR)?.let { errorType ->

            when (errorType) {
                GoConstants.TAG_NO_MUSIC -> {
                    mErrorIcon = R.drawable.ic_music_off
                    mErrorString = R.string.error_no_music
                }
                GoConstants.TAG_NO_MUSIC_INTENT -> {
                    mErrorIcon = R.drawable.ic_mood_bad
                    mErrorString = R.string.error_unknown_unsupported
                }
                GoConstants.TAG_SD_NOT_READY -> {
                    mErrorIcon = R.drawable.ic_mood_bad
                    mErrorString = R.string.error_not_ready
                }
            }
        }

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

        mErrorFragmentBinding = FragmentErrorBinding.bind(view)

        mErrorFragmentBinding.apply {
            errorMessage.text = getString(mErrorString)
            errorIcon.setImageResource(mErrorIcon)
            root.setOnClickListener { mUIControlInterface?.onRecreate() }
            errorToolbar.setNavigationOnClickListener {
                mUIControlInterface?.onCloseActivity(false)
            }
        }

        if (!goPreferences.isEdgeToEdge) {
            requireActivity().window.apply {
                val red = R.color.red.decodeColor(requireContext())
                statusBarColor = red
                navigationBarColor = red
            }
        }
    }
}
