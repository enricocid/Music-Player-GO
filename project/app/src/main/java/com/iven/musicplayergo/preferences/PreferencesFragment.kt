package com.iven.musicplayergo.preferences

import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.dialogs.RecyclerSheet
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import com.iven.musicplayergo.utils.Theming


class PreferencesFragment : PreferenceFragmentCompat(),
    SharedPreferences.OnSharedPreferenceChangeListener, Preference.OnPreferenceClickListener {

    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

    var onLiftOnScroll: ((Boolean) -> Unit)? = null

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
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

        findPreference<Preference>(getString(R.string.theme_pref))?.icon = ContextCompat.getDrawable(requireContext(), if (Theming.isThemeNight(resources)) {
            R.drawable.ic_night
        } else {
            R.drawable.ic_light
        })

        findPreference<Preference>(getString(R.string.theme_pref_black))?.isVisible = Theming.isThemeNight(resources)

        findPreference<Preference>(getString(R.string.accent_pref))?.run {
            summary = Theming.getAccentName(requireContext(), GoPreferences.getPrefsInstance().accent)
            onPreferenceClickListener = this@PreferencesFragment
        }

        findPreference<Preference>(getString(R.string.filter_pref))?.onPreferenceClickListener = this@PreferencesFragment

        findPreference<Preference>(getString(R.string.active_tabs_pref))?.run {
            summary = GoPreferences.getPrefsInstance().activeTabs.size.toString()
            onPreferenceClickListener = this@PreferencesFragment
        }

        findPreference<Preference>(getString(R.string.filter_pref))?.run {
            GoPreferences.getPrefsInstance().filters?.let { ft ->
                summary = ft.size.toString()
                isEnabled = ft.isNotEmpty()
            }
        }

        // fix liftOnScroll glitches
        listView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val lm = listView.layoutManager as LinearLayoutManager
                val isAtTop = lm.findFirstVisibleItemPosition() == 0
                        || listView.getChildAt(0).top == 0
                onLiftOnScroll?.invoke(isAtTop)
            }
        })
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        when (preference.key) {
            getString(R.string.accent_pref) -> RecyclerSheet.newInstance(GoConstants.ACCENT_TYPE)
                .show(requireActivity().supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
            getString(R.string.filter_pref) -> if (!GoPreferences.getPrefsInstance().filters.isNullOrEmpty()) {
                RecyclerSheet.newInstance(GoConstants.FILTERS_TYPE)
                    .show(requireActivity().supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
            }
            getString(R.string.active_tabs_pref) -> RecyclerSheet.newInstance(GoConstants.TABS_TYPE)
                .show(requireActivity().supportFragmentManager, RecyclerSheet.TAG_MODAL_RV)
        }
        return false
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            getString(R.string.precise_volume_pref) -> mMediaControlInterface.onGetMediaPlayerHolder()?.run {
                setPreciseVolume(if (!GoPreferences.getPrefsInstance().isPreciseVolumeEnabled) {
                    GoPreferences.getPrefsInstance().latestVolume = currentVolumeInPercent
                    100
                } else {
                    GoPreferences.getPrefsInstance().latestVolume
                })
            }
            getString(R.string.playback_vel_pref) -> mMediaControlInterface.onPlaybackSpeedToggled()
            getString(R.string.theme_pref) -> mUIControlInterface.onAppearanceChanged(isThemeChanged = true)
            getString(R.string.theme_pref_black) -> mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
            getString(R.string.focus_pref) -> mMediaControlInterface.onGetMediaPlayerHolder()?.run {
                if (GoPreferences.getPrefsInstance().isFocusEnabled) {
                    tryToGetAudioFocus()
                } else {
                    giveUpAudioFocus()
                }
            }
            getString(R.string.covers_pref) -> {
                mMediaControlInterface.onGetMediaPlayerHolder()?.onHandleNotificationUpdate(isAdditionalActionsChanged = false)
                mMediaControlInterface.onHandleCoverOptionsUpdate()
            }
            getString(R.string.notif_actions_pref) -> mMediaControlInterface.onGetMediaPlayerHolder()?.onHandleNotificationUpdate(isAdditionalActionsChanged = true)
            getString(R.string.song_visual_pref) -> mMediaControlInterface.onUpdatePlayingAlbumSongs(null)
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
