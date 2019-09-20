package com.iven.musicplayergo.fragments

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.musicPlayerGoExAppPreferences
import com.iven.musicplayergo.ui.AccentsAdapter
import com.iven.musicplayergo.ui.ThemeHelper

class PreferencesFragment : PreferenceFragmentCompat() {

    private lateinit var mAccentsDialog: MaterialDialog

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        if (activity != null) {
            val themePreference = findPreference<ListPreference>("theme_pref")
            themePreference?.setOnPreferenceChangeListener { _, newValue ->
                val themeOption = newValue as String
                ThemeHelper.applyTheme(activity!!, themeOption)
                return@setOnPreferenceChangeListener true
            }

            val accentPreference = findPreference<Preference>("accent_pref")
            accentPreference?.summary = String.format(
                getString(R.string.hex),
                0xFFFFFF and musicPlayerGoExAppPreferences.accent
            )
            accentPreference?.setOnPreferenceClickListener {
                showAccentDialog(it)
                return@setOnPreferenceClickListener true
            }

            val searchBarPreference = findPreference<SwitchPreference>("search_bar_pref")
            searchBarPreference?.setOnPreferenceChangeListener { _, _ ->
                ThemeHelper.applyNewThemeSmoothly(activity!!)
                return@setOnPreferenceChangeListener true
            }
        }
    }

    private fun showAccentDialog(accentPreference: Preference) {
        if (activity != null) {
            mAccentsDialog = MaterialDialog(activity!!).show {
                cornerRadius(res = R.dimen.md_radius)
                title(text = accentPreference.title.toString())
                customListAdapter(AccentsAdapter(activity!!))
                getRecyclerView().scrollToPosition(
                    ThemeHelper.getAccent(
                        musicPlayerGoExAppPreferences.accent
                    ).second
                )
            }
        }
    }

    override fun onPause() {
        super.onPause()
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
