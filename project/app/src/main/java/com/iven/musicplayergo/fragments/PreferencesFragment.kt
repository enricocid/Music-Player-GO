package com.iven.musicplayergo.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.AccentsAdapter
import com.iven.musicplayergo.adapters.ActiveTabsAdapter
import com.iven.musicplayergo.adapters.FiltersAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.ui.ItemTouchCallback
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface


class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private lateinit var mAccentsDialog: MaterialDialog
    private lateinit var mActiveFragmentsDialog: MaterialDialog
    private lateinit var mFiltersDialog: MaterialDialog

    private var mThemePreference: Preference? = null

    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        if (::mAccentsDialog.isInitialized && mAccentsDialog.isShowing) {
            mAccentsDialog.dismiss()
        }
        if (::mActiveFragmentsDialog.isInitialized && mActiveFragmentsDialog.isShowing) {
            mActiveFragmentsDialog.dismiss()
        }
        if (::mFiltersDialog.isInitialized && mFiltersDialog.isShowing) {
            mFiltersDialog.dismiss()
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
            mMediaControlInterface = activity as MediaControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mThemePreference = findPreference<Preference>(getString(R.string.theme_pref))?.apply {
            icon = ContextCompat.getDrawable(requireActivity(), ThemeHelper.resolveThemeIcon(requireActivity()))
        }

        findPreference<Preference>(getString(R.string.accent_pref))?.let { preference ->
            preference.summary = ThemeHelper.getAccentName(goPreferences.accent, requireActivity())
            preference.onPreferenceClickListener = this@PreferencesFragment
        }

        findPreference<Preference>(getString(R.string.filter_pref))?.let { preference ->
            preference.onPreferenceClickListener = this@PreferencesFragment
        }

        findPreference<Preference>(getString(R.string.active_tabs_pref))?.let { preference ->
            preference.summary = goPreferences.activeTabs.size.toString()
            preference.onPreferenceClickListener = this@PreferencesFragment
        }

        updateFiltersPreferences()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference?.key) {
            getString(R.string.accent_pref) -> showAccentsDialog()
            getString(R.string.filter_pref) -> if (!goPreferences.filters.isNullOrEmpty()) {
                showFiltersDialog()
            }
            getString(R.string.active_tabs_pref) -> showActiveFragmentsDialog()
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.precise_volume_pref) -> mMediaControlInterface.onPreciseVolumeToggled()
            getString(R.string.playback_speed_pref) -> mMediaControlInterface.onPlaybackSpeedToggled()
            getString(R.string.theme_pref) -> {
                mThemePreference?.icon = ContextCompat.getDrawable(requireActivity(), ThemeHelper.resolveThemeIcon(requireActivity()))
                mUIControlInterface.onAppearanceChanged(true)
            }
            getString(R.string.accent_pref) -> {
                mAccentsDialog.dismiss()
                mUIControlInterface.onAppearanceChanged(false)
            }
            getString(R.string.focus_pref) -> mMediaControlInterface.onHandleFocusPref()
            getString(R.string.covers_pref) -> {
                mMediaControlInterface.onHandleCoverOptionsUpdate()
                mMediaControlInterface.onHandleNotificationUpdate(false)
            }
            getString(R.string.fast_seeking_actions_pref) -> mMediaControlInterface.onHandleNotificationUpdate(
                true
            )
            getString(R.string.song_visual_pref) -> mUIControlInterface.onSongVisualizationChanged()
            getString(R.string.filter_pref) -> mUIControlInterface.onAppearanceChanged(false)
        }
    }

    private fun showAccentsDialog() {

        mAccentsDialog = MaterialDialog(requireActivity()).show {

            title(R.string.accent_pref_title)

            customListAdapter(AccentsAdapter(requireActivity()))

            val rv = getRecyclerView()
            rv.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.HORIZONTAL, false)
            rv.scrollToPosition(ThemeHelper.getAccentedTheme().second)
        }
    }

    private fun showActiveFragmentsDialog() {

        mActiveFragmentsDialog = MaterialDialog(requireActivity()).show {

            title(R.string.active_fragments_pref_title)

            val activeTabsAdapter = ActiveTabsAdapter(requireActivity())

            customListAdapter(activeTabsAdapter)

            val touchHelper = ItemTouchHelper(ItemTouchCallback(activeTabsAdapter.availableItems, true))
            touchHelper.attachToRecyclerView(getRecyclerView())

            positiveButton(android.R.string.ok) {
                goPreferences.activeTabs = activeTabsAdapter.getUpdatedItems().toMutableList()
                mUIControlInterface.onAppearanceChanged(false)
            }

            negativeButton(android.R.string.cancel)
        }
    }

    private fun showFiltersDialog() {

        MaterialDialog(requireActivity()).show {

            title(R.string.filter_pref_title)

            val filtersAdapter = FiltersAdapter(requireActivity())

            customListAdapter(filtersAdapter)

            positiveButton(android.R.string.ok) {
                if (goPreferences.filters != filtersAdapter.getUpdatedItems()) {
                    goPreferences.filters = filtersAdapter.getUpdatedItems()
                }
            }

            negativeButton(android.R.string.cancel)
        }
    }

    fun updateFiltersPreferences() {
        findPreference<Preference>(getString(R.string.filter_pref))?.let { preference ->
            goPreferences.filters?.let { ft ->
                preference.summary = ft.size.toString()
                preference.isEnabled = ft.isNotEmpty()
            }
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
