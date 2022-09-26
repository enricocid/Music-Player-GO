package com.iven.musicplayergo.equalizer

import android.animation.Animator
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.iven.musicplayergo.GoPreferences
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentEqualizerBinding
import com.iven.musicplayergo.extensions.afterMeasured
import com.iven.musicplayergo.extensions.createCircularReveal
import com.iven.musicplayergo.player.MediaPlayerHolder
import com.iven.musicplayergo.utils.Theming


class EqualizerFragment : Fragment() {

    private var _eqFragmentBinding: FragmentEqualizerBinding? = null
    private val mSliders = mutableMapOf<Slider?, TextView?>()
    private lateinit var mEqAnimator: Animator

    private var mEqualizer: Triple<Equalizer?, BassBoost?, Virtualizer?>? = null
    private val mPresetsList = mutableListOf<String>()
    private var mSelectedPreset = 0

    private val mMediaPlayerHolder get() = MediaPlayerHolder.getInstance()

    override fun onDestroyView() {
        super.onDestroyView()
        saveEqSettings()
        _eqFragmentBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _eqFragmentBinding = FragmentEqualizerBinding.inflate(inflater, container, false)
        return _eqFragmentBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mEqualizer = mMediaPlayerHolder.initOrGetBuiltInEqualizer()

        _eqFragmentBinding?.run {
            sliderBass.addOnChangeListener { _, value, fromUser ->
                // Responds to when slider's value is changed
                if (fromUser) {
                    mEqualizer?.second?.setStrength(value.toInt().toShort())
                }
            }
            sliderVirt.addOnChangeListener { _, value, fromUser ->
                // Responds to when slider's value is changed
                if (fromUser) {
                    mEqualizer?.third?.setStrength(value.toInt().toShort())
                }
            }
        }

        mEqualizer?.first?.let { equalizer ->
            (0 until equalizer.numberOfPresets).mapTo(mPresetsList) { preset ->
                equalizer.getPresetName(preset.toShort())
            }
        }

        if (mPresetsList.isNotEmpty()) {
            finishSetupEqualizer(view)
        } else {
            closeEqualizerOnError()
        }
    }

    private fun saveEqSettings() {
        _eqFragmentBinding?.run {
            mMediaPlayerHolder.onSaveEqualizerSettings(
                mSelectedPreset,
                sliderBass.value.toInt().toShort(),
                sliderVirt.value.toInt().toShort()
            )
        }
    }

    private fun finishSetupEqualizer(view: View) {

        _eqFragmentBinding?.run {
            mSliders[slider0] = freq0
            mSliders[slider1] = freq1
            mSliders[slider2] = freq2
            mSliders[slider3] = freq3
            mSliders[slider4] = freq4
        }

        GoPreferences.getPrefsInstance().savedEqualizerSettings?.let { savedEqualizerSettings ->
            mSelectedPreset = savedEqualizerSettings.preset
        }

        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, resources.getDimension(R.dimen.md_corner_radius))
            .build()
        val roundedTextBackground = MaterialShapeDrawable(shapeAppearanceModel).apply {
            strokeColor = ColorStateList.valueOf(Theming.resolveThemeColor(resources))
            strokeWidth = 2.5F
            fillColor =
                ColorStateList.valueOf(
                    Theming.resolveColorAttr(requireContext(), R.attr.main_bg)
                )
        }

        mEqualizer?.first?.run {
            val bandLevelRange = bandLevelRange
            val minBandLevel = bandLevelRange.first()
            val maxBandLevel = bandLevelRange[1]

            val iterator = mSliders.iterator().withIndex()

            while (iterator.hasNext()) {
                val item = iterator.next()
                item.value.key?.let { slider ->
                    slider.valueFrom = minBandLevel.toFloat()
                    slider.valueTo = maxBandLevel.toFloat()
                    slider.addOnChangeListener { selectedSlider, value, fromUser ->
                        if (fromUser) {
                            if (slider == selectedSlider) {
                                setBandLevel(
                                    item.index.toShort(),
                                    value.toInt().toShort()
                                )
                            }
                        }
                    }
                    mSliders[slider]?.let { textView ->
                        with(textView) {
                            text = formatMilliHzToK(getCenterFreq(item.index.toShort()))
                            background = roundedTextBackground
                        }
                    }
                }
            }
        }

        updateBandLevels(isPresetChanged = false)

        setupToolbar()

        // setup text field
        _eqFragmentBinding?.textField?.run {
            setSimpleItems(mPresetsList.toTypedArray())
            isSaveEnabled = false
            setText(mPresetsList[mSelectedPreset], false)
            setOnItemClickListener { _, _, newPreset, _ ->
                // Respond to item chosen
                mSelectedPreset = newPreset
                mEqualizer?.first?.usePreset(mSelectedPreset.toShort())
                updateBandLevels(isPresetChanged = true)
            }
        }

        if (GoPreferences.getPrefsInstance().isAnimations) {
            view.afterMeasured {
                _eqFragmentBinding?.root?.run {
                    mEqAnimator = createCircularReveal(show = true)
                }
            }
        }
    }

    private fun setupToolbar() {
        _eqFragmentBinding?.eqToolbar?.run {

            setNavigationOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            inflateMenu(R.menu.menu_eq)

            val equalizerSwitchMaterial =
                menu.findItem(R.id.equalizerSwitch).actionView as SwitchMaterial

            mEqualizer?.first?.let { equalizer ->
                equalizerSwitchMaterial.isChecked = equalizer.enabled
            }

            equalizerSwitchMaterial.setOnCheckedChangeListener { _, isChecked ->
                mMediaPlayerHolder.setEqualizerEnabled(isEnabled = isChecked)
            }
        }
    }

    private fun updateBandLevels(isPresetChanged: Boolean) {

        try {
            val iterator = mSliders.iterator().withIndex()
            while (iterator.hasNext()) {
                val item = iterator.next()
                mEqualizer?.first?.let { equalizer ->
                    item.value.key?.let { slider ->
                        slider.value = equalizer.getBandLevel(item.index.toShort()).toFloat()
                    }
                }
            }

            if (!isPresetChanged) {
                _eqFragmentBinding?.run {
                    GoPreferences.getPrefsInstance().savedEqualizerSettings?.let { eqSettings ->
                        sliderBass.value = eqSettings.bassBoost.toFloat()
                    }
                    mEqualizer?.third?.let { virtualizer ->
                        sliderVirt.value = virtualizer.roundedStrength.toFloat()
                    }
                }
            }
        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
            closeEqualizerOnError()
        }
    }

    private fun formatMilliHzToK(milliHz: Int) = if (milliHz < 1000000) {
        (milliHz / 1000).toString()
    } else {
        getString(R.string.freq_k, milliHz / 1000000)
    }

    private fun closeEqualizerOnError() {

        // disable equalizer component
        val pm = requireActivity().packageManager
        val cn = ComponentName(requireContext().applicationContext, EqualizerActivity::class.java)
        pm.setComponentEnabledSetting(cn, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)

        //release built in equalizer
        mMediaPlayerHolder.releaseBuiltInEqualizer()
        mMediaPlayerHolder.openEqualizer(requireActivity(), fallback = true)
        Toast.makeText(requireContext(), R.string.error_builtin_eq, Toast.LENGTH_SHORT).show()

        // bye, bye
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }
}
