package com.iven.musicplayergo

import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
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

        mPager = pager
        mTabLayout = tab_layout
        mTabLayout.setupWithViewPager(mPager)

        val pagerAdapter = ScreenSlidePagerAdapter(supportFragmentManager)
        mPager.adapter = pagerAdapter
        mPager.setPageTransformer(true, ZoomOutPageTransformer())
        mTabLayout.getTabAt(0)?.setIcon(R.drawable.ic_music)
        mTabLayout.getTabAt(1)?.setIcon(R.drawable.ic_np)
        mTabLayout.getTabAt(2)?.setIcon(R.drawable.ic_settings)
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private inner class ScreenSlidePagerAdapter(fm: FragmentManager) :
        FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
        override fun getCount(): Int = 3

        val icons = listOf(R.drawable.ic_music, R.drawable.ic_np, R.drawable.ic_settings)

        override fun getItem(position: Int): Fragment {
            return handleOnNavigationItemSelected(position)
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
