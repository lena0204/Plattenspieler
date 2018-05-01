package com.lk.plattenspieler.fragments

import android.app.*
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import com.lk.plattenspieler.R
import com.lk.plattenspieler.main.MainActivity
import kotlinx.android.synthetic.main.dialog_lyrics_adding.*

/**
 * Erstellt von Lena am 23.04.18.
 */
class LyricsAddingDialog: DialogFragment(){

	lateinit var listener: OnSaveLyrics

	interface OnSaveLyrics{
		fun onSaveLyrics(lyrics: String)
	}

	override fun onAttach(context: Context?) {
		super.onAttach(context)
		listener = context as MainActivity
	}

	// PROBLEM_ Writing access for Library is missing (min für mp3Files)
	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		super.onCreateDialog(savedInstanceState)
		val li = activity.layoutInflater
		val view = li.inflate(R.layout.dialog_lyrics_adding, null)
		val et = view.findViewById(R.id.et_lyrics_add) as EditText
		val builder = AlertDialog.Builder(context)
		builder.setTitle(R.string.dialog_title)
		builder.setView(view)
		builder.setPositiveButton(R.string.dialog_yes, { dialog, which ->
			if(et.text != null && et.text.toString() != ""){
				// Liedtext speichern
				listener.onSaveLyrics(et.text.toString())
			}
		})
		builder.setNegativeButton(R.string.dialog_no, {dialog, which ->
			dismiss()
		})
		return builder.create()
	}
}