package com.iven.musicplayergo.fragments

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.customListAdapter
import com.afollestad.materialdialogs.list.getRecyclerView
import com.iven.musicplayergo.R
import com.iven.musicplayergo.adapters.AccentsAdapter
import com.iven.musicplayergo.adapters.CheckableAdapter
import com.iven.musicplayergo.adapters.ThemesAdapter
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.ui.Utils

class PreferencesFragment : PreferenceFragmentCompat() {

    private lateinit var mThemesDialog: MaterialDialog
    private lateinit var mAccentsDialog: MaterialDialog
    private lateinit var mMultiListDialog: MaterialDialog

    private var mSelectedAccent = R.color.deep_purple

    private lateinit var mUIControlInterface: UIControlInterface

    override fun setDivider(divider: Drawable?) {
        val newDivider = if (context != null && goPreferences.isDividerEnabled) ColorDrawable(
            ThemeHelper.getAlphaAccent(
                context!!,
                50
            )
        )
        else ColorDrawable(Color.TRANSPARENT)
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

            val themePreference = findPreference<Preference>("theme_pref")
            themePreference?.summary =
                ThemeHelper.getAppliedThemeName(activity!!, goPreferences.theme)

            val accentPreference = findPreference<Preference>("accent_pref")
            accentPreference?.summary = String.format(
                getString(R.string.hex),
                0xFFFFFF and goPreferences.accent
            )

            accentPreference?.setOnPreferenceClickListener {
                showAccentDialog()
                return@setOnPreferenceClickListener true
            }

            val dividersPreference = findPreference<SwitchPreference>("divider_pref")
            dividersPreference?.setOnPreferenceChangeListener { _, _ ->
                ThemeHelper.applyNewThemeSmoothly(activity!!)
                return@setOnPreferenceChangeListener true
            }

            val searchBarPreference = findPreference<SwitchPreference>("search_bar_pref")
            searchBarPreference?.setOnPreferenceChangeListener { _, _ ->
                ThemeHelper.applyNewThemeSmoothly(activity!!)
                return@setOnPreferenceChangeListener true
            }

            val hiddenItemsPreference = findPreference<Preference>("hidden_items_pref")
            hiddenItemsPreference?.setOnPreferenceClickListener {
                if (goPreferences.hiddenItems?.isNotEmpty()!!) showHiddenItemsDialog()
                else Utils.makeToast(
                    activity!!,
                    getString(R.string.error_no_hidden_item)
                )
                return@setOnPreferenceClickListener true
            }

            val focusPreference = findPreference<SwitchPreference>("focus_pref")
            focusPreference?.setOnPreferenceChangeListener { _, _ ->

                return@setOnPreferenceChangeListener true
            }
        }
    }

    private fun showThemesDialog() {
        if (activity != null) {
            mThemesDialog = MaterialDialog(activity!!).show {

                cornerRadius(res = R.dimen.md_corner_radius)
                title(R.string.theme_pref_title)

                customListAdapter(
                    ThemesAdapter(
                        activity!!
                    )
                )
            }
        }
    }

    private fun showAccentDialog() {
        if (activity != null) {
            mAccentsDialog = MaterialDialog(activity!!).show {

                cornerRadius(res = R.dimen.md_corner_radius)
                title(R.string.accent_pref_title)

                customListAdapter(
                    AccentsAdapter(
                        activity!!
                    )
                )
                getRecyclerView().scrollToPosition(
                    ThemeHelper.getAccentedTheme().second
                )
            }
        }
    }

    private fun showHiddenItemsDialog() {
        if (activity != null) {
            mMultiListDialog = MaterialDialog(activity!!).show {
                cornerRadius(res = R.dimen.md_corner_radius)
                title(R.string.hidden_items_pref_title)
                val checkableAdapter =
                    CheckableAdapter(
                        goPreferences.hiddenItems!!.toMutableList()
                    )
                customListAdapter(checkableAdapter)
                positiveButton {
                    Utils.updateCheckableItems(checkableAdapter.getUpdatedItems())
                }
                negativeButton {}
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (::mThemesDialog.isInitialized && mThemesDialog.isShowing) mThemesDialog.dismiss()
        if (::mAccentsDialog.isInitialized && mAccentsDialog.isShowing) mAccentsDialog.dismiss()
        if (::mMultiListDialog.isInitialized && mMultiListDialog.isShowing) mMultiListDialog.dismiss()
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
