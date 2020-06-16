package com.iven.musicplayergo.ui

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.navigation.navOptions
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.MainActivityBinding
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.extensions.handleTransparentSystemBars
import com.iven.musicplayergo.extensions.toToast
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.DialogHelper
import com.iven.musicplayergo.helpers.MusicOrgHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.helpers.VersioningHelper
import com.iven.musicplayergo.interfaces.UIControlInterface
import com.iven.musicplayergo.navigation.DetailsFragment
import com.iven.musicplayergo.navigation.MainFragment
import com.iven.musicplayergo.player.MediaPlayerHolder
import de.halfbit.edgetoedge.Edge
import de.halfbit.edgetoedge.edgeToEdge


class MainActivity : AppCompatActivity(R.layout.main_activity),
    UIControlInterface {

    private val mMainFragment: Pair<Boolean, MainFragment?>
        get() {
            val navHostFragment = supportFragmentManager.primaryNavigationFragment
            val fragment = navHostFragment?.childFragmentManager!!.fragments[0] as? MainFragment
            return Pair(fragment != null, fragment)
        }

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    private val mNavOptions = navOptions {
        anim {
            enter = android.R.anim.slide_in_left
            exit = android.R.anim.slide_out_right
            popEnter = android.R.anim.slide_in_left
            popExit = android.R.anim.slide_out_right
        }
        launchSingleTop = true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (sAppearanceChanged) {
            super.onSaveInstanceState(outState)
            outState.putBoolean(
                GoConstants.RESTORE_SETTINGS_FRAGMENT,
                true
            )
        }
    }

    override fun onBackPressed() {
        if (!isDetailsFragment()) {
            onCloseActivity()
        } else {
            super.onBackPressed()
        }
    }

    private fun isDetailsFragment(): Boolean {
        val navHostFragment = supportFragmentManager.primaryNavigationFragment
        val fragment = navHostFragment?.childFragmentManager!!.fragments[0]
        return fragment is DetailsFragment
    }

    private fun setupEdgeToEdgeSystemBars() {
        if (goPreferences.isEdgeToEdge) {
            window?.apply {
                if (!VersioningHelper.isQ()) {
                    handleTransparentSystemBars()
                }
                ThemeHelper.handleLightSystemBars(resources.configuration, decorView, false)
            }
            edgeToEdge {
                MainActivityBinding.inflate(layoutInflater).root.fit { Edge.Top + Edge.Bottom }
            }
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        setTheme(ThemeHelper.getAccentedTheme().first)
        return super.onCreateView(name, context, attrs)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MediaPlayerHolder.getInstance()
        if (goPreferences.isEdgeToEdge) {
            setupEdgeToEdgeSystemBars()
        }
    }

    override fun onAddToFilter(stringToFilter: String?) {
        if (mMainFragment.first) {
            mMainFragment.second?.updateFilter(stringToFilter)
        }
    }

    override fun onError(errorType: String?) {
        findNavController(R.id.nav_host_fragment_container).navigate(
            R.id.errorFragment,
            bundleOf(Pair(GoConstants.TAG_ERROR, errorType))
        )
    }

    override fun onAppearanceChanged(isAccentChanged: Boolean, restoreSettings: Boolean) {
        sAppearanceChanged = true
        synchronized(mMediaPlayerHolder.mediaPlayerInterface?.onSaveSongToPref()!!) { recreate() }
    }

    override fun onArtistOrFolderSelected(artistOrFolder: String, launchedBy: LaunchedBy) {

        val bundle = Bundle()

        bundle.putString(GoConstants.TAG_ARTIST_FOLDER, artistOrFolder)
        bundle.putInt(GoConstants.TAG_IS_FOLDER, launchedBy.ordinal)
        val selectedAlbumPosition = MusicOrgHelper.getPlayingAlbumPosition(
            artistOrFolder
        )
        bundle.putInt(GoConstants.TAG_SELECTED_ALBUM_POSITION, selectedAlbumPosition)

        findNavController(R.id.nav_host_fragment_container).navigate(
            R.id.detailsFragment,
            bundle,
            mNavOptions
        )
    }

    override fun onCloseActivity() {
        if (mMediaPlayerHolder.isPlaying) {
            DialogHelper.stopPlaybackDialog(
                this
            )
        } else {
            finishAndRemoveTask()
        }
    }

    override fun onOpenLovedSongsDialog() {
        if (!goPreferences.lovedSongs.isNullOrEmpty()) {
            DialogHelper.showLovedSongsDialog(this)
        } else {
            getString(R.string.error_no_loved_songs).toToast(this)
        }
    }

    private var sAppearanceChanged = false

    override fun onThemeChanged() {
        sAppearanceChanged = true
        synchronized(mMediaPlayerHolder.mediaPlayerInterface?.onSaveSongToPref()!!) {
            AppCompatDelegate.setDefaultNightMode(
                ThemeHelper.getDefaultNightMode(
                    this
                )
            )
        }
    }
}
