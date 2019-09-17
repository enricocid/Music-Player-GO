package com.iven.musicplayergo.fragments

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.musicPlayerGoExAppPreferences
import com.iven.musicplayergo.ui.AccentsAdapter
import com.iven.musicplayergo.ui.ThemeHelper

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        if (context != null) {
            val themePreference = findPreference<ListPreference>("theme_pref")
            themePreference?.setOnPreferenceChangeListener { _, newValue ->
                val themeOption = newValue as String
                ThemeHelper.applyTheme(context!!, themeOption)
                return@setOnPreferenceChangeListener true
            }

            val accentPreference = findPreference<Preference>("accent_pref")
            accentPreference?.setOnPreferenceClickListener {
                showAccentDialog(it)
                return@setOnPreferenceClickListener true
            }
        }
    }

    private fun showAccentDialog(accentPreference: Preference) {
        if (context != null) {
            MaterialDialog(context!!).show {
                cornerRadius(res = R.dimen.md_corner_radius)
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
