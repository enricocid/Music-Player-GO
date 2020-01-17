package com.iven.musicplayergo.fragments

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.AccentsAdapter
import com.iven.musicplayergo.adapters.ActiveTabsAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils
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

            findPreference<Preference>(getString(R.string.accent_pref))?.apply {
                summary =
                    ThemeHelper.getAccentName(goPreferences.accent, fragmentActivity)
                onPreferenceClickListener = this@PreferencesFragment
            }

            findPreference<Preference>(getString(R.string.active_fragments_pref))?.apply {
                summary = goPreferences.activeFragments?.size.toString()
                onPreferenceClickListener = this@PreferencesFragment
            }
        }
    }

    override fun onPreferenceClick(preference: Preference?): Boolean {

        activity?.let {
            when (preference?.key) {

                getString(R.string.open_git_pref) -> Utils.openCustomTab(
                    it,
                    getString(R.string.app_git)
                )
                getString(R.string.faq_pref) -> Utils.openCustomTab(
                    it,
                    getString(R.string.app_faq)
                )
                getString(R.string.accent_pref) -> showAccentsDialog(it)
                getString(R.string.active_fragments_pref) -> showActiveFragmentsDialog(it)
            }
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        activity?.let {

            when (key) {
                getString(R.string.theme_pref) -> AppCompatDelegate.setDefaultNightMode(
                    ThemeHelper.getDefaultNightMode(
                        it
                    )
                )
                getString(R.string.tabs_pref), getString(R.string.edge_pref) -> ThemeHelper.applyNewThemeSmoothly(
                    it
                )
                getString(R.string.accent_pref) -> mUIControlInterface.onAccentUpdated()
            }
        }
    }

    private fun showAccentsDialog(activity: Activity) {

        mAccentsDialog = MaterialDialog(activity).show {

            cornerRadius(res = R.dimen.md_corner_radius)
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

            cornerRadius(res = R.dimen.md_corner_radius)
            title(R.string.active_fragments_pref_title)

            val activeTabsAdapter = ActiveTabsAdapter(activity)

            customListAdapter(activeTabsAdapter)

            getRecyclerView().layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

            positiveButton(R.string.yes) {
                goPreferences.activeFragments = activeTabsAdapter.getUpdatedItems()
                ThemeHelper.applyNewThemeSmoothly(activity)
            }
            negativeButton(R.string.no)
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
