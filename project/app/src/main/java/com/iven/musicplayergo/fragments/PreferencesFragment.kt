package com.iven.musicplayergo.fragments

import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.iven.musicplayergo.R
import com.iven.musicplayergo.ui.ThemeHelper

class PreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        val themePreference = findPreference<ListPreference>("themePref")
        themePreference?.setOnPreferenceChangeListener { _, newValue ->
            val themeOption = newValue as String
            ThemeHelper.applyTheme(themeOption)
            return@setOnPreferenceChangeListener true
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
