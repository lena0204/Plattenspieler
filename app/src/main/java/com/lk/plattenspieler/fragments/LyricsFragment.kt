package com.lk.plattenspieler.fragments

import android.app.Fragment
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.TextView
import com.lk.plattenspieler.R
import com.lk.plattenspieler.utils.ThemeChanger
import kotlinx.android.synthetic.main.fragment_lyrics.*
import kotlinx.android.synthetic.main.fragment_lyrics.view.*

/**
 * Erstellt von Lena am 21.04.18.
 * Zeigt den Liedtext vom aktuell spielenden Lied an
 */
class LyricsFragment: Fragment() {

	private val TAG = "LyricsFragment"

	private var lyricsText : TextView? = null

	// FIXME von Lyrics über zurück-Button zum PlayingFragment führt dazu, dass er Lyrics vergisst -> SaveState
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
		super.onCreateView(inflater, container, savedInstanceState)
		val v = inflater.inflate(R.layout.fragment_lyrics, container, false)
		lyricsText = v.tv_lyrics_text
		return v
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setTitleInActionbar()
		val args = this.arguments
		ll_lyrics_frame.background = Drawable.createFromPath(args?.getString("C"))
		val text = args?.getString("L")
		setLyricsText(text)
	}

	private fun setTitleInActionbar(){
		if(ThemeChanger.themeIsLineage(activity))
			(activity?.actionBar?.customView as TextView).text = resources.getString(R.string.action_lyrics)
		activity?.actionBar?.title = getString(R.string.action_lyrics)
	}

	private fun setLyricsText(text: String?){
		when {
			lyricsText == null -> Log.d(TAG, "TV ist null")
			text != null -> lyricsText?.text = text
			else -> Log.e(TAG, "Args oder lyrics sind null")
		}
	}
}