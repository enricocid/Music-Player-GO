package com.iven.musicplayergo.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.AccentsAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils

class PreferencesFragment : PreferenceFragmentCompat() {

    private lateinit var mThemesDialog: MaterialDialog
    private lateinit var mAccentsDialog: MaterialDialog

    private var mSelectedAccent = R.color.deep_purple

    private lateinit var mUIControlInterface: UIControlInterface

    override fun setDivider(divider: Drawable?) {
        val newDivider = ColorDrawable(
            ThemeHelper.getAlphaAccent(
                context!!,
                50
            )
        )
        super.setDivider(newDivider)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mSelectedAccent = goPreferences.accent

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            throw ClassCastException(activity.toString() + " must implement MyInterface ")
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        if (activity != null) {

            val openGitPreference = findPreference<Preference>("open_git_pref")

            openGitPreference?.setOnPreferenceClickListener {
                try {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/enricocid/Music-Player-GO")
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    Utils.makeToast(activity!!, getString(R.string.no_browser))
                    e.printStackTrace()
                }
                return@setOnPreferenceClickListener true
            }

            val themePreference = findPreference<ListPreference>("theme_pref")

            themePreference?.setOnPreferenceChangeListener { _, _ ->
                ThemeHelper.applyNewThemeSmoothly(
                    activity!!
                )
                return@setOnPreferenceChangeListener true
            }

            val accentPreference = findPreference<Preference>("accent_pref")
            accentPreference?.summary = String.format(
                getString(R.string.hex),
                0xFFFFFF and goPreferences.accent
            )

            accentPreference?.setOnPreferenceClickListener {
                showAccentDialog()
                return@setOnPreferenceClickListener true
            }

            val focusPreference = findPreference<SwitchPreference>("focus_pref")
            focusPreference?.setOnPreferenceChangeListener { _, _ ->

                return@setOnPreferenceChangeListener true
            }
        }
    }

    private fun showAccentDialog() {
        if (activity != null) {
            mAccentsDialog = MaterialDialog(activity!!).show {

                title(R.string.accent_pref_title)

                customListAdapter(
                    AccentsAdapter(
                        activity!!
                    )
                )
                getRecyclerView().layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

                getRecyclerView().scrollToPosition(
                    ThemeHelper.getAccentedTheme().second
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mThemesDialog.isInitialized && mThemesDialog.isShowing) mThemesDialog.dismiss()
        if (::mAccentsDialog.isInitialized && mAccentsDialog.isShowing) mAccentsDialog.dismiss()
    }

    companion object {

        internal const val TAG = "PreferencesFragmentTag"

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment PreferencesFragment.
         */
        @JvmStatic
        fun newInstance() = PreferencesFragment()
    }
}
