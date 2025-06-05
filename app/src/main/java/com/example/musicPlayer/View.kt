package com.example.musicPlayer

interface View {
    fun open()
    fun onUpdateAddSong(audio: AudioFile, position: Int)
    //fun onUpdateAudioFiles()
    fun onDestroy() {}
}