package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentErrorBinding
import com.iven.musicplayergo.extensions.afterMeasured
import com.iven.musicplayergo.extensions.createCircularReveal
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.UIControlInterface

/**
 * A simple [Fragment] subclass.
 * Use the [ErrorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ErrorFragment : Fragment(R.layout.fragment_error) {

    private lateinit var mErrorFragmentBinding: FragmentErrorBinding
    private lateinit var mUIControlInterface: UIControlInterface

    private var sNoMusic = false
    private var mErrorString = R.string.perm_rationale
    private var mErrorIcon = R.drawable.ic_folder

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ERROR)?.let { errorType ->

            when (errorType) {
                TAG_NO_MUSIC -> {
                    sNoMusic = true
                    mErrorIcon = R.drawable.ic_music_off
                    mErrorString = R.string.error_no_music
                }
                TAG_NO_MUSIC_INTENT -> {
                    sNoMusic = false
                    mErrorIcon = R.drawable.ic_sentiment_very_dissatisfied
                    mErrorString = R.string.error_unknown_unsupported
                }
                TAG_SD_NOT_READY -> {
                    sNoMusic = true
                    mErrorIcon = R.drawable.ic_sentiment_very_dissatisfied
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

        mErrorFragmentBinding.errorMessage.text = getString(mErrorString)
        mErrorFragmentBinding.errorIcon.setImageResource(mErrorIcon)
        mErrorFragmentBinding.root.setOnClickListener {
            if (sNoMusic) mUIControlInterface.onCloseActivity() else mUIControlInterface.onAppearanceChanged(
                isAccentChanged = false,
                restoreSettings = false
            )
        }

        mErrorFragmentBinding.errorToolbar.setNavigationOnClickListener {
            mUIControlInterface.onCloseActivity()
        }

        mErrorFragmentBinding.root.afterMeasured {
            createCircularReveal(isCentered = true, show = true).doOnEnd {
                if (!goPreferences.isEdgeToEdge) activity?.let { fa ->
                    fa.window.apply {
                        val red = ContextCompat.getColor(fa, R.color.red)
                        statusBarColor = red
                        navigationBarColor = red
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG_ERROR = "WE_HAVE_A_PROBLEM_HOUSTON"

        internal const val TAG_NO_PERMISSION = "NO_PERMISSION"
        internal const val TAG_NO_MUSIC = "NO_MUSIC"
        internal const val TAG_NO_MUSIC_INTENT = "NO_MUSIC_INTENT"
        internal const val TAG_SD_NOT_READY = "SD_NOT_READY"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ErrorFragment.
         */
        @JvmStatic
        fun newInstance(errorType: String) = ErrorFragment().apply {
            arguments = Bundle().apply {
                putString(TAG_ERROR, errorType)
            }
        }
    }
}
