package com.iven.musicplayergo.navigation

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.iven.musicplayergo.GoConstants
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentMainBinding
import com.iven.musicplayergo.enums.LaunchedBy
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ListsHelper
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.viewpager.AllMusicFragment
import com.iven.musicplayergo.viewpager.MusicContainersListFragment
import com.iven.musicplayergo.viewpager.SettingsFragment

class MainFragment : Fragment(R.layout.fragment_main) {

    // View binding classes
    private lateinit var mFragmentMainBinding: FragmentMainBinding

    // Colors
    private val mResolvedAccentColor get() = ThemeHelper.resolveThemeAccent(requireContext())
    private val mResolvedAlphaAccentColor
        get() = ThemeHelper.getAlphaAccent(
            requireContext(),
            ThemeHelper.getAlphaForAccent()
        )

    // Fragments
    private val mActiveFragments: List<Int> = goPreferences.activeFragments.toList()
    private var mArtistsFragment: MusicContainersListFragment? = null
    private var mAllMusicFragment: AllMusicFragment? = null
    private var mFoldersFragment: MusicContainersListFragment? = null
    private var mAlbumsFragment: MusicContainersListFragment? = null
    private var mSettingsFragment: SettingsFragment? = null

    // Booleans
    private var sRestoreSettingsFragment = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mFragmentMainBinding = FragmentMainBinding.bind(view)

        sRestoreSettingsFragment =
            savedInstanceState?.getBoolean(GoConstants.RESTORE_SETTINGS_FRAGMENT)
                ?: requireActivity().intent.getBooleanExtra(
                    GoConstants.RESTORE_SETTINGS_FRAGMENT,
                    false
                )

        initViewPager()
    }

    private fun initViewPager() {

        val pagerAdapter = ScreenSlidePagerAdapter()
        mFragmentMainBinding.viewPager2.offscreenPageLimit = mActiveFragments.size.minus(1)
        mFragmentMainBinding.viewPager2.adapter = pagerAdapter

        mFragmentMainBinding.tabLayout.apply {

            tabIconTint = ColorStateList.valueOf(mResolvedAlphaAccentColor)

            TabLayoutMediator(this, mFragmentMainBinding.viewPager2) { tab, position ->
                val fragmentIndex = mActiveFragments[position]
                tab.setIcon(ThemeHelper.getTabIcon(fragmentIndex))
                initFragmentAtPosition(fragmentIndex)
            }.attach()

            addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

                override fun onTabSelected(tab: TabLayout.Tab) {
                    tab.icon?.setTint(mResolvedAccentColor)
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    tab.icon?.setTint(mResolvedAlphaAccentColor)
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                }
            })

            getTabAt(
                if (sRestoreSettingsFragment) {
                    mFragmentMainBinding.viewPager2.offscreenPageLimit
                } else {
                    0
                }
            )?.icon?.setTint(
                mResolvedAccentColor
            )
        }

        if (sRestoreSettingsFragment) {
            mFragmentMainBinding.viewPager2.setCurrentItem(
                mFragmentMainBinding.viewPager2.offscreenPageLimit,
                false
            )
        }
    }

    private fun initFragmentAtPosition(fragmentIndex: Int) {
        when (fragmentIndex) {
            0 -> if (mArtistsFragment == null) {
                mArtistsFragment =
                    MusicContainersListFragment.newInstance(LaunchedBy.ArtistView)
            }
            1 -> if (mAlbumsFragment == null) {
                mAlbumsFragment =
                    MusicContainersListFragment.newInstance(LaunchedBy.AlbumView)
            }
            2 -> if (mAllMusicFragment == null) {
                mAllMusicFragment =
                    AllMusicFragment.newInstance()
            }
            3 -> if (mFoldersFragment == null) {
                mFoldersFragment =
                    MusicContainersListFragment.newInstance(LaunchedBy.FolderView)
            }
            else -> if (mSettingsFragment == null) {
                mSettingsFragment =
                    SettingsFragment.newInstance()
            }
        }
    }

    private fun handleOnNavigationItemSelected(itemId: Int) = when (itemId) {
        0 -> getFragmentForIndex(mActiveFragments[0])
        1 -> getFragmentForIndex(mActiveFragments[1])
        2 -> getFragmentForIndex(mActiveFragments[2])
        3 -> getFragmentForIndex(mActiveFragments[3])
        else -> getFragmentForIndex(mActiveFragments[4])
    }

    private fun getFragmentForIndex(index: Int) = when (index) {
        0 -> mArtistsFragment
        1 -> mAlbumsFragment
        2 -> mAllMusicFragment
        3 -> mFoldersFragment
        else -> mSettingsFragment
    }

    fun updateFilter(stringToFilter: String?) {
        stringToFilter?.let { string ->
            mArtistsFragment?.onListFiltered(string)
            mFoldersFragment?.onListFiltered(string)
            ListsHelper.addToHiddenItems(string)
        }
    }

    // ViewPager2 adapter class
    private inner class ScreenSlidePagerAdapter :
        FragmentStateAdapter(childFragmentManager, requireActivity().lifecycle) {
        override fun getItemCount(): Int = mActiveFragments.size

        override fun createFragment(position: Int): Fragment =
            handleOnNavigationItemSelected(position)!!
    }
}
