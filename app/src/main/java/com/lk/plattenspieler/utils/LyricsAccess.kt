package com.lk.plattenspieler.utils

import android.util.Log
import android.widget.Toast
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.id3.ID3v24FieldKey
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import java.io.File

/**
 * Erstellt von Lena am 13.05.18.
 * Lesender und schreibender Zugriff auf die Liedtexte eines Liedes über eine externe Bibliothek
 */
object LyricsAccess{

    private val TAG = this.javaClass.simpleName

    fun readLyrics(filepath: String): String{
        Log.d(TAG, filepath)
        if(filepath.contains("mp3")){
            // Mp3 Datei
            val mp3File = AudioFileIO.read(File(filepath)) as MP3File
            // PROBLEM_ Fehler bei read() auf Bliss ROM 6.0 und RR 7.1
            if(mp3File.hasID3v2Tag()) {
                val lyrics = mp3File.iD3v2TagAsv24.getFirst(ID3v24FieldKey.LYRICS)
                if (lyrics != null && lyrics != "") {
                    return lyrics
                    /*
					if (lyrics.length > 60) {
						lyrics = lyrics.substring(0, 60)
					}
					Log.d(TAG, "MP3-Datei hat Lyrics abgespeichert: $lyrics")*/
                }
            }
        } else if(filepath.contains("m4a")) {
            // m4a Datei
            val m4aTag = AudioFileIO.read(File(filepath)).tag as Mp4Tag
            val lyrics = m4aTag.getFirst(Mp4FieldKey.LYRICS)
            if(lyrics != null  && lyrics != ""){
                return lyrics
                /*
				if (lyrics.length > 60) {
					lyrics = lyrics.substring(0, 60)
				}
				Log.d(TAG, "M4A-Datei hat Lyrics abgespeichert: $lyrics")*/
            }
        }
        return ""
    }


    fun writeLyrics(lyrics: String, datapath: String){
        // PROBLEM_ schreiben auf die SD-Karte ist nicht unbedingt ohne weiteres möglich ...
        if(datapath != ""){
            Log.i(TAG, datapath)
            if(datapath.contains("mp3")){
                // Mp3 Datei
                try {
                    val mp3File = AudioFileIO.read(File(datapath))
                    val tag: Tag = mp3File.tag
                    if (!tag.isEmpty && tag is ID3v24Tag) {
                        Log.d(TAG, "Tag ist ein ID3v24Tag")
                        tag.setField(FieldKey.LYRICS, lyrics)
                        mp3File.tag = tag
                    } else {
                        Log.i(TAG, "Kein ID3v2 Tag vorhanden, keine Lyrics geschrieben.")
                    }
                    AudioFileIO.write(mp3File)
                } catch(ex: Exception){
                    Log.d(TAG, ex.message)
                }
            } else {
                // m4a Datei
                // TESTING_ muss getestet werden -> auch Problem mit SD-Karte
                val m4aTag = AudioFileIO.read(File(datapath)).tag as Mp4Tag
                m4aTag.setField(Mp4FieldKey.LYRICS, lyrics)
            }
        }
    }

}
