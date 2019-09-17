package com.iven.musicplayergo

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.tabs.TabLayout
import com.iven.musicplayergo.fragments.AllMusicFragment
import com.iven.musicplayergo.fragments.ArtistsFragment
import com.iven.musicplayergo.fragments.FoldersFragment
import com.iven.musicplayergo.fragments.SettingsFragment
import com.iven.musicplayergo.music.MusicViewModel
import com.iven.musicplayergo.ui.ThemeHelper
import com.iven.musicplayergo.ui.Utils
import com.iven.musicplayergo.ui.ZoomOutPageTransformer
import kotlinx.android.synthetic.main.main_activity.*

class MainActivity : AppCompatActivity() {

    //default
    private lateinit var mArtistsFragment: Fragment

    private lateinit var mAllMusicFragment: Fragment
    private lateinit var mFoldersFragment: Fragment
    private lateinit var mSettingsFragment: Fragment

    private lateinit var mFragmentManager: FragmentManager

    private lateinit var mActiveFragment: Fragment

    private lateinit var mPager: ViewPager
    private lateinit var mTabLayout: TabLayout

    // music shit related
    private val mMusicViewModel: MusicViewModel by lazy {
        ViewModelProviders.of(this).get(MusicViewModel::class.java)
    }

    private fun handleOnNavigationItemSelected(itemId: Int): Fragment {

        when (itemId) {
            0 -> mActiveFragment = mArtistsFragment
            1 -> mActiveFragment = mAllMusicFragment
            2 -> mActiveFragment = mFoldersFragment
            3 -> mActiveFragment = mSettingsFragment
        }
        return mActiveFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        ThemeHelper.applyTheme(this, musicPlayerGoExAppPreferences.theme)

        setTheme(ThemeHelper.getAccent(musicPlayerGoExAppPreferences.accent).first)

        mFragmentManager = supportFragmentManager

        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission() else setupUI()
        super.onCreate(savedInstanceState)
    }

    //manage request permission result, continue loading ui if permissions was granted
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) showPermissionRationale() else setupUI()
    }

    @TargetApi(23)
    private fun checkPermission() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        )
            showPermissionRationale() else setupUI()
    }

    private fun showPermissionRationale() {

        MaterialDialog(this).show {

            cornerRadius(res = R.dimen.md_corner_radius)
            title(R.string.app_name)
            message(R.string.perm_rationale)
            positiveButton {
                ActivityCompat.requestPermissions(
                    this@MainActivity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    2588
                )
            }
            negativeButton {
                Utils.makeUnknownErrorToast(this@MainActivity, R.string.perm_rationale)
                dismiss()
                finishAndRemoveTask()
            }
        }
    }

    private fun setupUI() {

        mMusicViewModel.loadMusic(this).observe(this, Observer { hasLoaded ->

            if (hasLoaded) {

                mArtistsFragment = ArtistsFragment.newInstance()
                mActiveFragment = mArtistsFragment

                mAllMusicFragment = AllMusicFragment.newInstance()
                mFoldersFragment = FoldersFragment.newInstance()
                mSettingsFragment = SettingsFragment.newInstance()

                mPager = pager
                mTabLayout = tab_layout
                mTabLayout.setupWithViewPager(mPager)

                val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
                mPager.adapter = pagerAdapter
                mPager.setPageTransformer(true, ZoomOutPageTransformer())
                mTabLayout.getTabAt(0)?.setIcon(R.drawable.ic_person)
                mTabLayout.getTabAt(1)?.setIcon(R.drawable.ic_music)
                mTabLayout.getTabAt(2)?.setIcon(R.drawable.ic_folder)
                mTabLayout.getTabAt(3)?.setIcon(R.drawable.ic_settings)
            }
        })
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = 4

        override fun getItem(position: Int): Fragment {
            return handleOnNavigationItemSelected(position)
        }

    }
}
