package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentErrorBinding
import com.iven.musicplayergo.ui.UIControlInterface

/**
 * A simple [Fragment] subclass.
 * Use the [ErrorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ErrorFragment : Fragment() {

    private var _errorFragmentBinding: FragmentErrorBinding? = null

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

    override fun onDestroyView() {
        super.onDestroyView()
        _errorFragmentBinding = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            // Handle the back button event
            handleOnBackPressed()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _errorFragmentBinding = FragmentErrorBinding.inflate(inflater, container, false)
        return _errorFragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _errorFragmentBinding?.run {
            errorMessage.text = getString(mErrorString)
            errorIcon.setImageResource(mErrorIcon)
            root.setOnClickListener { mUIControlInterface.onCloseActivity() }

            errorToolbar.setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
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
