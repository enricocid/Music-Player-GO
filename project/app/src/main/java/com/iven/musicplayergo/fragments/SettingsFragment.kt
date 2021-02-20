package com.iven.musicplayergo.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentSettingsBinding
import com.iven.musicplayergo.ui.UIControlInterface

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _fragmentSettingsBinding: FragmentSettingsBinding? = null

    private lateinit var mUIControlInterface: UIControlInterface

    private var mPreferencesFragment: PreferencesFragment? = null

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

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentSettingsBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _fragmentSettingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return _fragmentSettingsBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _fragmentSettingsBinding?.searchToolbar?.run {
            inflateMenu(R.menu.menu_settings)
            setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }
            setOnMenuItemClickListener {
                if (it.itemId == R.id.github_page) {
                    openGitHubPage()
                }
                return@setOnMenuItemClickListener true
            }
        }

        mPreferencesFragment = PreferencesFragment.newInstance()
        mPreferencesFragment?.let { fm ->
            childFragmentManager.commit {
                replace(R.id.fragment_layout, fm)
            }
        }
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun openGitHubPage() {
       val customTabsIntent = CustomTabsIntent.Builder()
               .setShareState(CustomTabsIntent.SHARE_STATE_ON)
               .setShowTitle(true)
               .build()

        val parsedUri = getString(R.string.app_git).toUri()
        val manager = requireActivity().packageManager
        val info = manager.queryIntentActivities(customTabsIntent.intent, 0)
        if (info.size > 0) {
            customTabsIntent.launchUrl(requireActivity(), parsedUri)
        } else {
            //from: https://github.com/immuni-app/immuni-app-android/blob/development/extensions/src/main/java/it/ministerodellasalute/immuni/extensions/utils/ExternalLinksHelper.kt
            val browserIntent = Intent(Intent.ACTION_VIEW, parsedUri)
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val fallbackInfo = manager.queryIntentActivities(browserIntent, 0)
            if (fallbackInfo.size > 0) {
                requireActivity().startActivity(browserIntent)
            } else {
                Toast.makeText(
                        requireActivity(),
                        requireActivity().getString(R.string.error_no_browser),
                        Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun onFiltersChanged() {
        mPreferencesFragment?.updateFiltersPreferences()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SettingsFragment.
         */
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
