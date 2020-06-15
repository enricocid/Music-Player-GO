package com.iven.musicplayergo.viewpager

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.browser.customtabs.CustomTabsIntent
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.MusicRepository
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.AccentsAdapter
import com.iven.musicplayergo.adapters.ActiveTabsAdapter
import com.iven.musicplayergo.adapters.FiltersAdapter
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.interfaces.UIControlInterface
import com.iven.musicplayergo.player.MediaPlayerHolder

class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private lateinit var mAccentsDialog: MaterialDialog
    private lateinit var mActiveFragmentsDialog: MaterialDialog
    private lateinit var mFiltersDialog: MaterialDialog

    private lateinit var mUIControlInterface: UIControlInterface

    private var mThemePreference: Preference? = null

    override fun setDivider(divider: Drawable?) {
        context?.let { cxt ->
            super.setDivider(
                ColorDrawable(
                    ThemeHelper.getAlphaAccent(
                        cxt,
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
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findPreference<Preference>(getString(R.string.open_git_pref))?.onPreferenceClickListener =
            this

        findPreference<Preference>(getString(R.string.faq_pref))?.onPreferenceClickListener =
            this

        findPreference<Preference>(getString(R.string.found_songs_pref))?.apply {
            val musicRepository = MusicRepository.getInstance()
            title =
                getString(R.string.found_songs_pref_title, musicRepository.musicDatabaseSize)
        }

        mThemePreference = findPreference<Preference>(getString(R.string.theme_pref))?.apply {
            icon = AppCompatResources.getDrawable(
                requireContext(),
                ThemeHelper.resolveThemeIcon(requireContext())
            )
        }

        findPreference<Preference>(getString(R.string.accent_pref))?.apply {
            summary =
                ThemeHelper.getAccentName(goPreferences.accent, requireContext())
            onPreferenceClickListener = this@PreferencesFragment
        }

        findPreference<Preference>(getString(R.string.filter_pref))?.apply {
            onPreferenceClickListener = this@PreferencesFragment
        }

        findPreference<Preference>(getString(R.string.active_fragments_pref))?.apply {
            summary = goPreferences.activeFragments.size.toString()
            onPreferenceClickListener = this@PreferencesFragment
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {
        when (preference?.key) {
            getString(R.string.open_git_pref) -> openCustomTab(getString(R.string.app_git))
            getString(R.string.faq_pref) -> openCustomTab(getString(R.string.app_faq))
            getString(R.string.accent_pref) -> showAccentsDialog()
            getString(R.string.filter_pref) -> if (!goPreferences.filters.isNullOrEmpty()) {
                showFiltersDialog()
            } else {
                getString(
                    R.string.error_no_filter
                ).toToast(requireContext())
            }
            getString(R.string.active_fragments_pref) -> showActiveFragmentsDialog()
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.theme_pref) -> {
                mThemePreference?.icon =
                    AppCompatResources.getDrawable(
                        requireContext(),
                        ThemeHelper.resolveThemeIcon(requireContext())
                    )
                mUIControlInterface.onThemeChanged()
            }
            getString(R.string.edge_pref) -> mUIControlInterface.onAppearanceChanged(
                isAccentChanged = false,
                restoreSettings = true
            )
            getString(R.string.accent_pref) -> {
                mAccentsDialog.dismiss()
                mUIControlInterface.onAppearanceChanged(
                    isAccentChanged = true,
                    restoreSettings = true
                )
            }
            getString(R.string.focus_pref) -> MediaPlayerHolder.getInstance().mediaPlayerInterface?.onHandleFocusPref()
        }
    }

    private fun openCustomTab(link: String) {
        val customTabsIntent = CustomTabsIntent.Builder()
            .addDefaultShareMenuItem()
            .setShowTitle(true)
            .build()

        val parsedUri = Uri.parse(link)
        val manager = requireContext().packageManager
        val infos = manager.queryIntentActivities(customTabsIntent.intent, 0)
        if (infos.size > 0) {
            customTabsIntent.launchUrl(requireContext(), parsedUri)
        } else {

            //from: https://github.com/immuni-app/immuni-app-android/blob/development/extensions/src/main/java/it/ministerodellasalute/immuni/extensions/utils/ExternalLinksHelper.kt
            val browserIntent = Intent(Intent.ACTION_VIEW, parsedUri)
            browserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val fallbackInfos = manager.queryIntentActivities(browserIntent, 0)
            if (fallbackInfos.size > 0) {
                requireContext().startActivity(browserIntent)
            } else {
                Toast.makeText(
                    context,
                    requireContext().getString(R.string.error_no_browser),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showAccentsDialog() {

        mAccentsDialog = MaterialDialog(requireActivity()).show {

            title(R.string.accent_pref_title)

            customListAdapter(AccentsAdapter(requireActivity()))

            getRecyclerView().apply {
                layoutManager =
                    LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                scrollToPosition(ThemeHelper.getAccentedTheme().second)
            }
        }
    }

    private fun showActiveFragmentsDialog() {

        mActiveFragmentsDialog = MaterialDialog(requireActivity()).show {

            title(R.string.active_fragments_pref_title)

            val activeTabsAdapter = ActiveTabsAdapter(requireContext())

            customListAdapter(activeTabsAdapter)

            getRecyclerView().layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

            positiveButton(android.R.string.ok) {
                goPreferences.activeFragments = activeTabsAdapter.getUpdatedItems()
                mUIControlInterface.onAppearanceChanged(
                    isAccentChanged = false,
                    restoreSettings = true
                )
            }

            negativeButton(android.R.string.cancel)
        }
    }

    private fun showFiltersDialog() {

        MaterialDialog(requireActivity()).show {

            title(R.string.filter_pref_title)

            val filtersAdapter = FiltersAdapter()

            customListAdapter(filtersAdapter)

            positiveButton(android.R.string.ok) {
                goPreferences.filters = filtersAdapter.getUpdatedItems()
                requireActivity().recreate()
            }

            negativeButton(android.R.string.cancel)
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
