package com.lk.plattenspieler.utils

import android.util.Log
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.mp3.MP3File
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.id3.ID3v24FieldKey
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.mp4.Mp4FieldKey
import org.jaudiotagger.tag.mp4.Mp4Tag
import java.io.File

/**
 * Erstellt von Lena am 13.05.18.
 * Lesender und schreibender Zugriff auf die Liedtexte eines Liedes über eine externe Bibliothek
 */
object LyricsAccess {

    private val TAG = this.javaClass.simpleName
    private var currentLyrics = ""

    fun readLyrics(filepath: String): String{
        Log.d(TAG, filepath)
        currentLyrics = ""
        if(filepath.contains("mp3")){
            readMP3Lyrics(filepath)
        } else if(filepath.contains("m4a")) {
            readM4ALyrics(filepath)
        }
        return currentLyrics
    }

    private fun readMP3Lyrics(filepath: String) {
        val mp3File = AudioFileIO.read(File(filepath)) as MP3File
        // PROBLEM_ Fehler bei read() auf Bliss ROM 6.0 und RR 7.1
        if(mp3File.hasID3v2Tag()) {
            val lyrics = mp3File.iD3v2TagAsv24.getFirst(ID3v24FieldKey.LYRICS)
            if (!lyrics.isNullOrEmpty()) {
                currentLyrics = lyrics
            }
        }
    }

    private fun readM4ALyrics(filepath: String) {
        val m4aTag = AudioFileIO.read(File(filepath)).tag as Mp4Tag
        val lyrics = m4aTag.getFirst(Mp4FieldKey.LYRICS)
        if(!lyrics.isNullOrEmpty()){
            currentLyrics = lyrics
        }
    }

    // PROBLEM_ schreiben auf die SD-Karte ist nicht unbedingt ohne weiteres möglich ...
    // TODO Nutzer informieren, wenn schreiben der Lyrics fehlgeschlagen ist, geeignete Rückgabe !!
    fun writeLyrics(lyrics: String, datapath: String){
        if(datapath != ""){
            Log.i(TAG, datapath)
            if(datapath.contains("mp3")){
                writeLyricsForMP3File(datapath, lyrics)
            } else if(datapath.contains("m4a")) {
                writeLyricsForM4AFile(datapath, lyrics)
            }
        }
    }

    private fun writeLyricsForMP3File(path: String, lyrics: String){
        try {
            val mp3File = AudioFileIO.read(File(path))
            val mp3Tag = mp3File.tag
            if (!mp3Tag.isEmpty && mp3Tag is ID3v24Tag) {
                mp3Tag.setField(FieldKey.LYRICS, lyrics)
                mp3File.tag = mp3Tag
            } else {
                Log.w(TAG, "Kein ID3v2 Tag vorhanden, keine Lyrics geschrieben.")
            }
            AudioFileIO.write(mp3File)
        } catch(ex: Exception){
            Log.e(TAG, ex.message)
        }
    }

    private fun writeLyricsForM4AFile(path: String, lyrics: String){
        // m4a Datei
        // TESTING_ muss getestet werden -> auch Problem mit SD-Karte
        val m4aTag = AudioFileIO.read(File(path)).tag as Mp4Tag
        m4aTag.setField(Mp4FieldKey.LYRICS, lyrics)
    }

}
