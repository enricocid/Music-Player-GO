package com.iven.musicplayergo.player

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.RelativeSizeSpan
import android.text.style.TextAppearanceSpan
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.iven.musicplayergo.R


class SleeptimerDialog : DialogFragment() {
    private var numberString: String = "------"
    private var setTimeTextView: TextView? = null

    override fun onCreateDialog(
        savedInstanceState: Bundle?
    ): Dialog {

        val activity = requireActivity()
        val dialogBuilder = AlertDialog.Builder(activity)
        val rootView = activity.layoutInflater.inflate(R.layout.sleeptimer_dialog, null)
        setupOnClickListener(rootView)
        setTimeTextView = rootView.findViewById(R.id.setTime) as TextView
        changeTextView()
        dialogBuilder.setTitle(R.string.sleeptimer)
        dialogBuilder.setView(rootView)
        return dialogBuilder.create()
    }

    private fun setupOnClickListener(rootView: View){

        (rootView.findViewById(R.id.button1) as Button).setOnClickListener({ appendNumber('1') })
        (rootView.findViewById(R.id.button2) as Button).setOnClickListener({ appendNumber('2') })
        (rootView.findViewById(R.id.button3) as Button).setOnClickListener({ appendNumber('3') })
        (rootView.findViewById(R.id.button4) as Button).setOnClickListener({ appendNumber('4') })
        (rootView.findViewById(R.id.button5) as Button).setOnClickListener({ appendNumber('5') })
        (rootView.findViewById(R.id.button6) as Button).setOnClickListener({ appendNumber('6') })
        (rootView.findViewById(R.id.button7) as Button).setOnClickListener({ appendNumber('7') })
        (rootView.findViewById(R.id.button8) as Button).setOnClickListener({ appendNumber('8') })
        (rootView.findViewById(R.id.button9) as Button).setOnClickListener({ appendNumber('9') })
        (rootView.findViewById(R.id.button0) as Button).setOnClickListener({ appendNumber('0') })
        (rootView.findViewById(R.id.button00) as Button).setOnClickListener({
            appendNumber('0')
            appendNumber('0')
        })
        (rootView.findViewById(R.id.buttonBack) as Button).setOnClickListener({ deleteNumber() })
        (rootView.findViewById(R.id.buttonCancel) as Button).setOnClickListener({ dismiss() })
        (rootView.findViewById(R.id.buttonEnter) as Button).setOnClickListener({

        })
    }
    private fun appendNumber(number: Char){

        val emptyIndex = numberString.lastIndexOf('-')
        if (emptyIndex == -1) return

        numberString = numberString.removeRange(0..0) + number

        changeTextView()
    }

    private fun deleteNumber(){

        numberString = '-' + numberString.dropLast(1)

        changeTextView()
    }

    private fun numberString2timeString(numberString: String): SpannableString{

        val hoursNumber = numberString.slice(0..1)
        val hoursString = getText(R.string.sleeptimer_hours)
        val minutesNumber = numberString.slice(2..3)
        val minutesString = getText(R.string.sleeptimer_minutes)
        val secondsNumber = numberString.slice(4..5)
        val secondsString = getText(R.string.sleeptimer_seconds)
        val timeString = SpannableString(
            hoursNumber   + hoursString +
                    minutesNumber + minutesString+
                    secondsNumber + secondsString)
        var begin = 0
        val spans = arrayOf(
            Pair(2                   , 1f   ),
            Pair(hoursString.length  , 0.25f),
            Pair(2                   , 1f   ),
            Pair(minutesString.length, 0.25f),
            Pair(2                   , 1f   ),
            Pair(secondsString.length, 0.25f),
        )
        for ((len, textSize) in spans) {
            timeString.setSpan(
                RelativeSizeSpan(textSize), begin, begin+len,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            begin += len
        }

        return timeString
    }
    private fun changeTextView(){

        setTimeTextView!!.setText(numberString2timeString(numberString), TextView.BufferType.SPANNABLE)
    }
}
