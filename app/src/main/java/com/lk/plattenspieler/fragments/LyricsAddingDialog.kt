package com.lk.plattenspieler.fragments

import android.app.*
import android.content.Context
import android.os.Bundle
import com.lk.plattenspieler.R
import kotlinx.android.synthetic.main.dialog_lyrics_adding.*

/**
 * Erstellt von Lena am 23.04.18.
 */
class LyricsAddingDialog: DialogFragment(){


	override fun onAttach(context: Context?) {
		super.onAttach(context)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		super.onCreateDialog(savedInstanceState)
		val li = activity.layoutInflater
		val view = li.inflate(R.layout.dialog_lyrics_adding, null)
		val et = et_lyrics_add
		val builder = AlertDialog.Builder(context)
		builder.setTitle(R.string.dialog_title)
		builder.setView(view)
		builder.setPositiveButton(R.string.dialog_yes, { dialog, which ->
			if(et.text != null && et.text.toString() != ""){
				val liedtext = et.text.toString()
				// Liedtext speichern
			}
		})
		builder.setNegativeButton(R.string.dialog_no, {dialog, which ->
			dismiss()
		})
		return builder.create()
	}
}