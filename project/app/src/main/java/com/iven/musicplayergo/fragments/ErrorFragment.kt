package com.iven.musicplayergo.fragments

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.iven.musicplayergo.R
import com.iven.musicplayergo.afterMeasured
import com.iven.musicplayergo.createCircularReveal
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import kotlinx.android.synthetic.main.fragment_error.*

/**
 * A simple [Fragment] subclass.
 * Use the [ErrorFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ErrorFragment : Fragment(R.layout.fragment_error) {

    private lateinit var mUIControlInterface: UIControlInterface

    private var sNoMusic = false
    private var mErrorString = R.string.perm_rationale
    private var mErrorIcon = R.drawable.ic_folder

    override fun onAttach(context: Context) {
        super.onAttach(context)

        arguments?.getString(TAG_ERROR)?.let { errorType ->

            sNoMusic = errorType == TAG_NO_MUSIC

            if (sNoMusic) {
                mErrorIcon = R.drawable.ic_music_off
                mErrorString = R.string.error_no_music
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

        error_message.text = getString(mErrorString)
        error_icon.setImageResource(mErrorIcon)
        view.setOnClickListener {
            if (sNoMusic) mUIControlInterface.onCloseActivity() else activity?.let { fa ->
                ThemeHelper.applyNewThemeSmoothly(fa)
            }
        }

        error_toolbar.setNavigationOnClickListener {
            mUIControlInterface.onCloseActivity()
        }

        view.afterMeasured {
            createCircularReveal(isCentered = true, show = true)
        }
    }

    companion object {

        private const val TAG_ERROR = "WE_HAVE_A_PROBLEM_HOUSTON"

        internal const val TAG_NO_PERMISSION = "NO_PERMISSION"
        internal const val TAG_NO_MUSIC = "NO_MUSIC"

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
