package com.iven.musicplayergo.fragments

import android.animation.Animator
import android.content.Context
import android.content.res.ColorStateList
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.AdapterView.OnItemSelectedListener
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.afollestad.recyclical.datasource.emptyDataSource
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
import com.iven.musicplayergo.ui.MediaControlInterface
import com.iven.musicplayergo.ui.UIControlInterface
import java.util.*
import kotlin.concurrent.schedule


/**
 * A simple [Fragment] subclass.
 * Use the [EqFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class EqFragment : Fragment(R.layout.fragment_equalizer) {

    private var _eqFragmentBinding: FragmentEqualizerBinding? = null

    private lateinit var mEqualizer: Triple<Equalizer?, BassBoost?, Virtualizer?>

    private lateinit var mEqAnimator: Animator

    private val mPresetsList = mutableListOf<String>()

    private val mDataSource = emptyDataSource()

    private var mSelectedPreset = 0

    private val mSliders = mutableMapOf<Slider?, TextView?>()

    private lateinit var mUIControlInterface: UIControlInterface
    private lateinit var mMediaControlInterface: MediaControlInterface

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

    fun onHandleBackPressed(): Animator {
        if (!mEqAnimator.isRunning) {
            _eqFragmentBinding?.root?.run {
                mEqAnimator = createCircularReveal(
                    isErrorFragment = false,
                    show = false
                )
            }
        }
        return mEqAnimator
    }

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

        if (!::mEqualizer.isInitialized) {
            mEqualizer = mMediaControlInterface.onGetEqualizer()
        }

        _eqFragmentBinding?.run {
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

    private fun saveEqSettings() {
        _eqFragmentBinding?.run {
            mMediaControlInterface.onSaveEqualizerSettings(
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
        }

        updateBandLevels(false)

        setupToolbar()

        if (goPreferences.isAnimations) {
            view.afterMeasured {
                _eqFragmentBinding?.root?.run {
                    mEqAnimator = createCircularReveal(
                        isErrorFragment = false,
                        show = true
                    )
                }
            }
        }
    }

    private fun setupToolbar() {
        _eqFragmentBinding?.eqToolbar?.run {

            setNavigationOnClickListener {
                requireActivity().onBackPressed()
            }

            inflateMenu(R.menu.menu_eq)

            val equalizerSwitchMaterial =
                menu.findItem(R.id.equalizerSwitch).actionView as SwitchMaterial

            mEqualizer.first?.let { equalizer ->
                equalizerSwitchMaterial.isChecked = equalizer.enabled
            }
            equalizerSwitchMaterial.setOnCheckedChangeListener { _, isChecked ->
                Timer().schedule(1000) {
                    mMediaControlInterface.onEnableEqualizer(isChecked)
                }
            }

            with(menu.findItem(R.id.miSpinner).actionView as Spinner) {

                // set Spinner Adapter
                val spinnerAdapter = ArrayAdapter(requireActivity(), android.R.layout.simple_spinner_dropdown_item, mPresetsList)
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                adapter = spinnerAdapter

                setSelection(mSelectedPreset)

                onItemSelectedListener = object : OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        mSelectedPreset = position
                        mEqualizer.first?.usePreset(mSelectedPreset.toShort())
                        updateBandLevels(isPresetChanged = true)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {
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
                _eqFragmentBinding?.run {
                    goPreferences.savedEqualizerSettings?.let { eqSettings ->
                        sliderBass.value = eqSettings.bassBoost.toFloat()
                    }
                    mEqualizer.third?.let { virtualizer ->
                        sliderVirt.value = virtualizer.roundedStrength.toFloat()
                    }
                }
            }

        } catch (e: UnsupportedOperationException) {
            e.printStackTrace()
            Toast.makeText(requireActivity(), R.string.error_eq, Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun formatMilliHzToK(milliHz: Int) = if (milliHz < 1000000) {
        (milliHz / 1000).toString()
    } else {
        getString(R.string.freq_k, milliHz / 1000000)
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
