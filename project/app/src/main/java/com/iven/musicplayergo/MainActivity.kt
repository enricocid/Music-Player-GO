package com.iven.musicplayergo

import android.Manifest
import android.annotation.TargetApi
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction

private const val TAG_PERMISSION_RATIONALE = "com.iven.musicplayergo.rationale"

class MainActivity : AppCompatActivity() {

    //preferences
    private var mAccent: Int? = R.color.blue
    private var sThemeInverted: Boolean = false
    private lateinit var mMainFragment: MainFragment

    //boolean
    //https://stackoverflow.com/a/42262467
    private var sRequestPermissionsWithResult = false

    override fun onBackPressed() {
        if (::mMainFragment.isInitialized && !mMainFragment.onBackPressed()
        ) else super.onBackPressed()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            showPermissionRationale() else
            sRequestPermissionsWithResult = true
    }

    //https://stackoverflow.com/a/42262467
    override fun onPostResume() {
        super.onPostResume()
        if (sRequestPermissionsWithResult) {
            initMainFragment()
        }
        sRequestPermissionsWithResult = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mAccent = mMusicPlayerGoPreferences.accent
        sThemeInverted = mMusicPlayerGoPreferences.isThemeInverted
        setTheme(mMusicPlayerGoPreferences.resolveTheme(sThemeInverted, mAccent))

        setContentView(R.layout.main_activity)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) checkPermission() else initMainFragment()
    }

    @TargetApi(23)
    private fun checkPermission() {

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            showPermissionRationale() else initMainFragment()
    }

    private fun showPermissionRationale() {
        PermissionDialogFragment.newInstance().show(supportFragmentManager, TAG_PERMISSION_RATIONALE)
    }

    private fun initMainFragment() {
        supportFragmentManager.inTransaction {
            mMainFragment = MainFragment.newInstance(sThemeInverted, mAccent!!)
            replace(R.id.fragment_container, mMainFragment)
        }
    }

    fun openGitPage(@Suppress("UNUSED_PARAMETER") view: View) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/enricocid/Music-Player-GO")))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, getString(R.string.no_browser), Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    private inline fun FragmentManager.inTransaction(func: FragmentTransaction.() -> Unit) {
        val fragmentTransaction = beginTransaction()
        fragmentTransaction.func()
        fragmentTransaction.commitNow()
    }
}
