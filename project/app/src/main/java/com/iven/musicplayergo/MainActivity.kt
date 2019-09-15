package com.iven.musicplayergo

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.main_activity.*

@Suppress("UNUSED_PARAMETER")
class MainActivity : AppCompatActivity() {

    //default
    private lateinit var mMusicFragment: Fragment

    private lateinit var mNowPlayingFragment: Fragment
    private lateinit var mSettingsFragment: Fragment

    private lateinit var mFragmentManager: FragmentManager

    private lateinit var mActiveFragment: Fragment

    private fun handleOnNavigationItemSelected(itemId: Int) {

        when (itemId) {
            R.id.app_bar_artists -> {
                mFragmentManager.beginTransaction().hide(mActiveFragment).show(mMusicFragment)
                    .commit()
                mActiveFragment = mMusicFragment
            }
            R.id.app_bar_np -> {
                mFragmentManager.beginTransaction().hide(mActiveFragment).show(mNowPlayingFragment)
                    .commit()
                mActiveFragment = mNowPlayingFragment
            }
            R.id.app_bar_settings -> {
                mFragmentManager.beginTransaction().hide(mActiveFragment).show(mSettingsFragment)
                    .commit()
                mActiveFragment = mSettingsFragment
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mMusicFragment = MusicFragment.newInstance("", "")
        mActiveFragment  = mMusicFragment

        mNowPlayingFragment = NowPlayingFragment.newInstance("", "")
        mSettingsFragment = SettingsFragment.newInstance("", "")

        mFragmentManager = supportFragmentManager

        mFragmentManager.beginTransaction()
            .add(R.id.main_container, mMusicFragment, mMusicFragment.tag).commit()
        mFragmentManager.beginTransaction()
            .add(R.id.main_container, mNowPlayingFragment, mNowPlayingFragment.tag)
            .hide(mNowPlayingFragment).commit()
        mFragmentManager.beginTransaction()
            .add(R.id.main_container, mSettingsFragment, mSettingsFragment.tag)
            .hide(mSettingsFragment).commit()

        setContentView(R.layout.main_activity)

        bottom_navigation.setOnNavigationItemSelectedListener {
            handleOnNavigationItemSelected(it.itemId)
            return@setOnNavigationItemSelectedListener true
        }
    }

    //viewTreeObserver extension to measure layout params
    //https://antonioleiva.com/kotlin-ongloballayoutlistener/
    private inline fun <T : View> T.afterMeasured(crossinline f: T.() -> Unit) {
        viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                if (measuredWidth > 0 && measuredHeight > 0) {
                    viewTreeObserver.removeOnGlobalLayoutListener(this)
                    f()
                }
            }
        })
    }
}
