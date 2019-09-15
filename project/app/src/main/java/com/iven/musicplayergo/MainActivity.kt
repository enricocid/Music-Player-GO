package com.iven.musicplayergo

import android.Manifest
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.iven.musicplayergo.music.MusicViewModel
import kotlinx.android.synthetic.main.main_activity.*


@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity() {

    //default
    private lateinit var mMusicFragment: Fragment

    private lateinit var mNowPlayingFragment: Fragment
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
            0 -> mActiveFragment = mMusicFragment
            1 -> mActiveFragment = mNowPlayingFragment
            2 -> mActiveFragment = mSettingsFragment
        }
        return mActiveFragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        mMusicFragment = MusicFragment.newInstance("", "")
        mActiveFragment = mMusicFragment

        mNowPlayingFragment = NowPlayingFragment.newInstance("", "")
        mSettingsFragment = SettingsFragment.newInstance("", "")

        mFragmentManager = supportFragmentManager

        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission() else setupUI()
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
        mPager = pager
        mTabLayout = tab_layout
        mTabLayout.setupWithViewPager(mPager)

        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        mPager.adapter = pagerAdapter
        mPager.setPageTransformer(true, ZoomOutPageTransformer())
        mTabLayout.getTabAt(0)?.setIcon(R.drawable.ic_music)
        mTabLayout.getTabAt(1)?.setIcon(R.drawable.ic_folder)
        mTabLayout.getTabAt(2)?.setIcon(R.drawable.ic_settings)

        mMusicViewModel.loadMusic(this).observe(this, Observer {
            if (it.first.isNotEmpty()) {
                it.first.iterator().forEach { itt -> Log.d(itt.title, itt.artist!!) }
            }
        })
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = 3

        override fun getItem(position: Int): Fragment {
            return handleOnNavigationItemSelected(position)
        }

    }
}
