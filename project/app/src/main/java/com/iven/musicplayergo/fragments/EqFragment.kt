package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.content.Context
import android.content.res.ColorStateList
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.recyclical.datasource.emptyDataSource
import com.afollestad.recyclical.setup
import com.afollestad.recyclical.withItem
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.ShapeAppearanceModel
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.iven.musicplayergo.R
import com.iven.musicplayergo.databinding.FragmentEqualizerBinding
import com.iven.musicplayergo.extensions.afterMeasured
import com.iven.musicplayergo.extensions.createCircularReveal
import com.iven.musicplayergo.goPreferences
import com.iven.musicplayergo.helpers.ThemeHelper
import com.iven.musicplayergo.ui.PresetsViewHolder
import com.iven.musicplayergo.ui.UIControlInterface
import java.util.*
import kotlin.concurrent.schedule


/**
 * A simple [Fragment] subclass.
 * Use the [EqFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EqFragment : Fragment(R.layout.fragment_equalizer) {

    private lateinit var mEqualizer: Triple<Equalizer?, BassBoost?, Virtualizer?>

    private lateinit var mEqFragmentBinding: FragmentEqualizerBinding
    private lateinit var mUIControlInterface: UIControlInterface

    private lateinit var mEqAnimator: Animator

    private val mPresetsList = mutableListOf<String>()

    private val mDataSource = emptyDataSource()

    private var mSelectedPreset = 0

    private val mSliders = mutableMapOf<Slider?, TextView?>()

    override fun onAttach(context: Context) {
        super.onAttach(context)

        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    fun onHandleBackPressed(): Animator {
        if (!mEqAnimator.isRunning) {
            mEqAnimator =
                mEqFragmentBinding.root.createCircularReveal(
                    isErrorFragment = false,
                    show = false
                )
        }
        return mEqAnimator
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!::mEqualizer.isInitialized) {
            mEqualizer = mUIControlInterface.onGetEqualizer()
        }

        mEqFragmentBinding = FragmentEqualizerBinding.bind(view).apply {
            sliderBass.addOnChangeListener { _, value, fromUser ->
                // Responds to when slider's value is changed
                if (fromUser) {
                    mEqualizer.second?.setStrength(value.toInt().toShort())
                }
            }
            sliderVirt.addOnChangeListener { _, value, fromUser ->
                // Responds to when slider's value is changed
                if (fromUser) {
                    mEqualizer.third?.setStrength(value.toInt().toShort())
                }
            }
        }

        mEqualizer.first?.let { equalizer ->
            (0 until equalizer.numberOfPresets).mapTo(mPresetsList) { preset ->
                equalizer.getPresetName(preset.toShort())
            }
        }

        mDataSource.set(mPresetsList)

        finishSetupEqualizer(view)
    }

    override fun onDetach() {
        synchronized(saveEqSettings()) {
            super.onDetach()
        }
    }

    private fun saveEqSettings() {
        mUIControlInterface.onSaveEqualizerSettings(
            mSelectedPreset,
            mEqFragmentBinding.sliderBass.value.toInt().toShort(),
            mEqFragmentBinding.sliderVirt.value.toInt().toShort()
        )
    }

    private fun finishSetupEqualizer(view: View) {

        mEqFragmentBinding.run {
            mSliders[slider0] = freq0
            mSliders[slider1] = freq1
            mSliders[slider2] = freq2
            mSliders[slider3] = freq3
            mSliders[slider4] = freq4
        }

        goPreferences.savedEqualizerSettings?.let { savedEqualizerSettings ->
            mSelectedPreset = savedEqualizerSettings.preset
        }

        val shapeAppearanceModel = ShapeAppearanceModel()
            .toBuilder()
            .setAllCorners(CornerFamily.ROUNDED, resources.getDimension(R.dimen.md_corner_radius))
            .build()
        val roundedTextBackground = MaterialShapeDrawable(shapeAppearanceModel).apply {
            strokeColor = ColorStateList.valueOf(ThemeHelper.resolveThemeAccent(requireActivity()))
            strokeWidth = 0.50F
            fillColor =
                ColorStateList.valueOf(
                    ContextCompat.getColor(
                        requireActivity(),
                        R.color.windowBackground
                    )
                )
        }

        mEqualizer.first?.run {
            val bandLevelRange = bandLevelRange
            val minBandLevel = bandLevelRange[0]
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
                        textView.run {
                            text = formatMilliHzToK(getCenterFreq(item.index.toShort()))
                            background = roundedTextBackground
                        }
                    }
                }
            }

            mEqFragmentBinding.presets.run {
                setup {
                    withDataSource(mDataSource)
                    withItem<String, PresetsViewHolder>(R.layout.eq_preset_item) {
                        onBind(::PresetsViewHolder) { index, item ->
                            presetTitle.text = item
                            val textColor = if (mSelectedPreset == index) {
                                ThemeHelper.resolveThemeAccent(requireActivity())
                            } else {
                                ThemeHelper.resolveColorAttr(
                                    requireActivity(),
                                    android.R.attr.textColorPrimary
                                )
                            }
                            presetTitle.setTextColor(textColor)
                        }

                        onClick { index ->
                            adapter?.notifyItemChanged(mSelectedPreset)
                            mSelectedPreset = index
                            adapter?.notifyItemChanged(mSelectedPreset)
                            mEqualizer.first?.usePreset(mSelectedPreset.toShort())
                            updateBandLevels(true)
                        }
                    }
                }
                scrollToPosition(mSelectedPreset)
            }
        }

        updateBandLevels(false)

        setupToolbar()

        if (goPreferences.isAnimations) {
            view.afterMeasured {
                mEqAnimator =
                        mEqFragmentBinding.root.createCircularReveal(
                                isErrorFragment = false,
                                show = true
                        )
            }
        }
    }

    private fun setupToolbar() {
        mEqFragmentBinding.eqToolbar.run {

            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }

            inflateMenu(R.menu.menu_eq)
            menu.run {
                val equalizerSwitchMaterial =
                    findItem(R.id.equalizerSwitch).actionView as SwitchMaterial
                mEqualizer.first?.let { equalizer ->
                    equalizerSwitchMaterial.isChecked = equalizer.enabled
                }
                equalizerSwitchMaterial.setOnCheckedChangeListener { _, isChecked ->
                    Timer().schedule(1000) {
                        mUIControlInterface.onEnableEqualizer(isChecked)
                    }
                }
            }
        }
    }

    private fun updateBandLevels(isPresetChanged: Boolean) {
        try {
            val iterator = mSliders.iterator().withIndex()
            while (iterator.hasNext()) {
                val item = iterator.next()
                mEqualizer.first?.let { equalizer ->
                    item.value.key?.let { slider ->
                        slider.value = equalizer.getBandLevel(item.index.toShort()).toFloat()
                    }
                }
            }

            if (!isPresetChanged) {
                goPreferences.savedEqualizerSettings?.let { eqSettings ->
                    mEqFragmentBinding.sliderBass.value = eqSettings.bassBoost.toFloat()
                }
                mEqualizer.third?.let { virtualizer ->
                    mEqFragmentBinding.sliderVirt.value = virtualizer.roundedStrength.toFloat()
                }
            }

        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(),  getString(R.string.error_eq), Toast.LENGTH_LONG)
                    .show()
        }
    }

    private fun formatMilliHzToK(milliHz: Int): String {
        return if (milliHz < 1000000) {
            (milliHz / 1000).toString()
        } else {
            getString(R.string.freq_k, milliHz / 1000000)
        }
    }

    companion object {

        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment EqFragment.
         */
        @JvmStatic
        fun newInstance() = EqFragment()
    }
}
