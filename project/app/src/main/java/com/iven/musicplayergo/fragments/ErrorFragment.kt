package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentErrorBinding
import com.iven.musicplayergo.extensions.afterMeasured
import com.iven.musicplayergo.extensions.createCircularReveal
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.ui.UIControlInterface

/**
 * A simple [Fragment] subclass.
 * Use the [ErrorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ErrorFragment : Fragment(R.layout.fragment_error) {

    private lateinit var mErrorFragmentBinding: FragmentErrorBinding
    private lateinit var mUIControlInterface: UIControlInterface

    private var mErrorString = R.string.perm_rationale
    private var mErrorIcon = R.drawable.ic_folder_music

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ERROR)?.let { errorType ->

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

        mErrorFragmentBinding = FragmentErrorBinding.bind(view).apply {
            errorMessage.text = getString(mErrorString)
            errorIcon.setImageResource(mErrorIcon)
            root.setOnClickListener { mUIControlInterface.onCloseActivity() }

            errorToolbar.setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }

            root.afterMeasured {
                createCircularReveal(isErrorFragment = true, show = true).doOnEnd {
                    if (!VersioningHelper.isOreoMR1()) {
                        requireActivity().window.run {
                            val red = ContextCompat.getColor(requireActivity(), R.color.red)
                            statusBarColor = red
                            navigationBarColor = red
                        }
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG_ERROR = "WE_HAVE_A_PROBLEM_HOUSTON"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment ErrorFragment.
         */
        @JvmStatic
        fun newInstance(errorType: String) = ErrorFragment().apply {
            arguments = bundleOf(TAG_ERROR to errorType)
        }
    }
}
