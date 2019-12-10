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
import com.iven.musicplayergo.adapters.CheckableTabsAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils
import kotlin.properties.Delegates


class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mThemesDialog: MaterialDialog
    private lateinit var mAccentsDialog: MaterialDialog
    private lateinit var mMultiListDialog: MaterialDialog
    private lateinit var mSupportedFormatsDialog: MaterialDialog

    private var mSelectedAccent: Int by Delegates.notNull()

    private lateinit var mUIControlInterface: UIControlInterface

    override fun setDivider(divider: Drawable?) {
        val newDivider = ColorDrawable(
            ThemeHelper.getAlphaAccent(
                context!!,
                85
            )
        )
        super.setDivider(newDivider)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
        if (::mThemesDialog.isInitialized && mThemesDialog.isShowing) mThemesDialog.dismiss()
        if (::mAccentsDialog.isInitialized && mAccentsDialog.isShowing) mAccentsDialog.dismiss()
        if (::mMultiListDialog.isInitialized && mMultiListDialog.isShowing) mMultiListDialog.dismiss()
        if (::mSupportedFormatsDialog.isInitialized && mSupportedFormatsDialog.isShowing) mSupportedFormatsDialog.dismiss()
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
            val openGitPreference = findPreference<Preference>(getString(R.string.open_git_pref))

            openGitPreference?.setOnPreferenceClickListener {
                Utils.openCustomTab(fragmentActivity, getString(R.string.app_git))
                return@setOnPreferenceClickListener true
            }

            val supportedMediaPreference =
                findPreference<Preference>(getString(R.string.supported_formats_pref))

            supportedMediaPreference?.setOnPreferenceClickListener {
                mSupportedFormatsDialog = Utils.showSupportedFormatsDialog(fragmentActivity)
                return@setOnPreferenceClickListener true
            }

            val accentPreference = findPreference<Preference>(getString(R.string.accent_pref))
            accentPreference?.summary =
                ThemeHelper.getAccentName(goPreferences.accent, fragmentActivity)

            accentPreference?.setOnPreferenceClickListener {
                showAccentDialog(fragmentActivity)
                return@setOnPreferenceClickListener true
            }

            val activeFragmentsPreference =
                findPreference<Preference>(getString(R.string.active_fragments_pref))

            activeFragmentsPreference?.summary = goPreferences.activeFragments?.size.toString()
            activeFragmentsPreference?.setOnPreferenceClickListener {
                showActiveFragmentsDialog(fragmentActivity)
                return@setOnPreferenceClickListener true
            }
        }
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

    private fun showAccentDialog(activity: Activity) {

        mAccentsDialog = MaterialDialog(activity).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(res = R.string.accent_pref_title)

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

        mMultiListDialog = MaterialDialog(activity).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(res = R.string.active_fragments_pref_title)

            val checkableAdapter = CheckableTabsAdapter(
                activity,
                resources.getStringArray(R.array.activeFragmentsListArray).toMutableList()
            )
            customListAdapter(checkableAdapter)

            getRecyclerView().layoutManager =
                LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

            positiveButton {
                goPreferences.activeFragments = checkableAdapter.getUpdatedItems()
                ThemeHelper.applyNewThemeSmoothly(activity)
            }
            negativeButton {}
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
