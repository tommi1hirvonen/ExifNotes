package com.tommihirvonen.exifnotes.utilities

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import com.tommihirvonen.exifnotes.R

class GeneralDialogBuilder(private val context: Context, private val title: String, private val message: String) {
    fun create(): AlertDialog {
        val generalDialogBuilder = AlertDialog.Builder(context)
        generalDialogBuilder.setTitle(title)
        val spannableString = SpannableString(message)
        Linkify.addLinks(spannableString, Linkify.WEB_URLS)
        generalDialogBuilder.setMessage(spannableString)
        generalDialogBuilder.setNegativeButton(R.string.Close) { _: DialogInterface?, _: Int -> }
        val generalDialog = generalDialogBuilder.create()
        generalDialog.setOnShowListener {
            val textView = generalDialog.findViewById<TextView>(android.R.id.message)
            textView.textSize = 14f
            textView.movementMethod = LinkMovementMethod.getInstance()
        }
        return generalDialog
    }
}