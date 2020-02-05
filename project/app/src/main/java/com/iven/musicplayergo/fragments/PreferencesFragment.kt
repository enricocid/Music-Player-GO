package com.iven.musicplayergo.fragments

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.AccentsAdapter
import com.iven.musicplayergo.adapters.ActiveTabsAdapter
import com.iven.musicplayergo.adapters.FiltersAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.musicLibrary
import com.iven.musicplayergo.toToast
import com.iven.musicplayergo.utils.ThemeHelper
import com.iven.musicplayergo.utils.UIControlInterface
import com.iven.musicplayergo.utils.Utils
import kotlin.properties.Delegates

class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private lateinit var mAccentsDialog: MaterialDialog
    private lateinit var mActiveFragmentsDialog: MaterialDialog

    private var mSelectedAccent: Int by Delegates.notNull()

    private lateinit var mUIControlInterface: UIControlInterface

    override fun setDivider(divider: Drawable?) {
        context?.let {
            super.setDivider(
                ColorDrawable(
                    ThemeHelper.getAlphaAccent(
                        it,
                        85
                    )
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        if (::mAccentsDialog.isInitialized && mAccentsDialog.isShowing) mAccentsDialog.dismiss()
        if (::mActiveFragmentsDialog.isInitialized && mActiveFragmentsDialog.isShowing) mActiveFragmentsDialog.dismiss()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        mSelectedAccent = goPreferences.accent

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        activity?.let { fragmentActivity ->

            findPreference<Preference>(getString(R.string.open_git_pref))?.onPreferenceClickListener =
                this

            findPreference<Preference>(getString(R.string.faq_pref))?.onPreferenceClickListener =
                this

            findPreference<Preference>(getString(R.string.reset_pref_lib))?.apply {
                summary = getString(R.string.reset_pref_summary, musicLibrary.allSongs?.size)
                onPreferenceClickListener = this@PreferencesFragment
            }

            findPreference<Preference>(getString(R.string.accent_pref))?.apply {
                summary =
                    ThemeHelper.getAccentName(goPreferences.accent, fragmentActivity)
                onPreferenceClickListener = this@PreferencesFragment
            }

            findPreference<Preference>(getString(R.string.filter_pref))?.apply {
                onPreferenceClickListener = this@PreferencesFragment
            }

            findPreference<Preference>(getString(R.string.active_fragments_pref))?.apply {
                summary = goPreferences.activeFragments?.size.toString()
                onPreferenceClickListener = this@PreferencesFragment
            }
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {

        activity?.let { ac ->
            when (preference?.key) {

                getString(R.string.open_git_pref) -> Utils.openCustomTab(
                    ac,
                    getString(R.string.app_git)
                )
                getString(R.string.faq_pref) -> Utils.openCustomTab(
                    ac,
                    getString(R.string.app_faq)
                )
                getString(R.string.reset_pref_lib) -> showReloadDatabaseDialog(ac)
                getString(R.string.accent_pref) -> showAccentsDialog(ac)
                getString(R.string.filter_pref) -> {
                    if (!goPreferences.filters.isNullOrEmpty()) showFiltersDialog(ac) else getString(
                        R.string.error_no_filter
                    ).toToast(ac)
                }
                getString(R.string.active_fragments_pref) -> showActiveFragmentsDialog(ac)
            }
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        activity?.let {

            when (key) {
                getString(R.string.theme_pref) -> mUIControlInterface.onThemeChanged(false)
                getString(R.string.edge_pref) -> ThemeHelper.applyNewThemeSmoothly(
                    it,
                    true
                )
                getString(R.string.accent_pref) -> mUIControlInterface.onThemeChanged(true)
            }
        }
    }

    private fun showReloadDatabaseDialog(activity: Activity) {
        MaterialDialog(activity).show {
            title(R.string.app_name)
            message(R.string.reset_pref_message)
            positiveButton(R.string.yes) {
                goPreferences.apply {
                    reloadDB = true
                    latestPlayedSong = null
                }
                mUIControlInterface.onStopPlaybackFromReloadDB()
            }
            negativeButton(R.string.no)
        }
    }

    private fun showAccentsDialog(activity: Activity) {

        mAccentsDialog = MaterialDialog(activity).show {

            title(R.string.accent_pref_title)

            customListAdapter(
                AccentsAdapter(
                    activity
                )
            )

            getRecyclerView().apply {
                layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
                scrollToPosition(ThemeHelper.getAccentedTheme().second)
            }
        }
    }

    private fun showActiveFragmentsDialog(activity: Activity) {

        mActiveFragmentsDialog = MaterialDialog(activity).show {

            title(R.string.active_fragments_pref_title)

            val activeTabsAdapter = ActiveTabsAdapter(activity)

            customListAdapter(activeTabsAdapter)

            getRecyclerView().layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

            positiveButton(R.string.yes) {
                goPreferences.activeFragments = activeTabsAdapter.getUpdatedItems()
                ThemeHelper.applyNewThemeSmoothly(activity, true)
            }

            negativeButton(R.string.no)
        }
    }

    private fun showFiltersDialog(activity: Activity) {

        MaterialDialog(activity).show {

            title(R.string.filter_pref_title)

            val filtersAdapter = FiltersAdapter()

            customListAdapter(filtersAdapter)

            positiveButton(R.string.yes) {
                goPreferences.filters = filtersAdapter.getUpdatedItems()
                ThemeHelper.applyNewThemeSmoothly(activity, false)
            }

            negativeButton(R.string.no)
        }
    }

    companion object {
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
