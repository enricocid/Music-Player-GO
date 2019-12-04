package com.iven.musicplayergo.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import androidx.preference.ListPreference
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


class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var mThemesDialog: MaterialDialog
    private lateinit var mAccentsDialog: MaterialDialog
    private lateinit var mMultiListDialog: MaterialDialog

    private var mSelectedAccent = R.color.deep_purple

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

        if (activity != null) {

            val openGitPreference = findPreference<Preference>(getString(R.string.open_git_pref))

            openGitPreference?.setOnPreferenceClickListener {
                try {
                    Utils.openCustomTab(activity!!)
                } catch (e: Exception) {
                    Utils.makeToast(activity!!, getString(R.string.no_browser))
                    e.printStackTrace()
                }
                return@setOnPreferenceClickListener true
            }

            findPreference<ListPreference>(getString(R.string.theme_pref))?.setOnPreferenceChangeListener { _, _ ->
                ThemeHelper.applyNewThemeSmoothly(
                    activity!!
                )
                return@setOnPreferenceChangeListener true
            }

            val accentPreference = findPreference<Preference>(getString(R.string.accent_pref))
            accentPreference?.summary = String.format(
                getString(R.string.hex),
                0xFFFFFF and goPreferences.accent
            )

            accentPreference?.setOnPreferenceClickListener {
                showAccentDialog()
                return@setOnPreferenceClickListener true
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) preferenceScreen.removePreference(
                findPreference<Preference>(getString(R.string.edge_pref))
            )

            val activeFragmentsPreference =
                findPreference<Preference>(getString(R.string.active_fragments_pref))

            activeFragmentsPreference?.summary = goPreferences.activeFragments?.size.toString()
            activeFragmentsPreference?.setOnPreferenceClickListener {
                showActiveFragmentsDialog()
                return@setOnPreferenceClickListener true
            }
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

        if (activity != null && key == getString(R.string.theme_pref) || key == getString(R.string.tabs_pref) || key == getString(
                R.string.edge_pref
            )
        ) ThemeHelper.applyNewThemeSmoothly(
            activity!!
        ) else if (key == getString(R.string.accent_pref)) mUIControlInterface.onAccentUpdated()
    }

    private fun showAccentDialog() {
        if (activity != null) {
            mAccentsDialog = MaterialDialog(activity!!).show {

                title(res = R.string.accent_pref_title)

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

    private fun showActiveFragmentsDialog() {
        if (activity != null) {
            mMultiListDialog = MaterialDialog(activity!!).show {

                cornerRadius(res = R.dimen.md_corner_radius)
                title(res = R.string.active_fragments_pref_title)

                val checkableAdapter = CheckableTabsAdapter(
                    activity!!,
                    resources.getStringArray(R.array.activeFragmentsListArray).toMutableList()
                )
                customListAdapter(checkableAdapter)

                getRecyclerView().layoutManager =
                    LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)

                positiveButton {
                    goPreferences.activeFragments = checkableAdapter.getUpdatedItems()
                    ThemeHelper.applyNewThemeSmoothly(activity!!)
                }
                negativeButton {}
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
