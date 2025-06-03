package com.example.musicPlayer

interface View {
    fun open()
    fun onUpdateAddSong(position: Int)
    fun onUpdateAudioFiles()
    fun onDestroy() {}
}