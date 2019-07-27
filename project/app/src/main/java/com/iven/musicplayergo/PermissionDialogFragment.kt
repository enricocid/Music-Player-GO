package com.iven.musicplayergo

import android.Manifest
import android.content.DialogInterface
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.permission_dialog.*

class PermissionDialogFragment : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.permission_dialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(Color.TRANSPARENT)
        dlg_one_button_btn_ok.setOnClickListener {
            dismiss()
        }
        isCancelable = false
    }

    override fun onDismiss(dialog: DialogInterface) {
        requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2588)
        super.onDismiss(dialog)
    }

    companion object {
        fun newInstance() = PermissionDialogFragment()
    }
}
