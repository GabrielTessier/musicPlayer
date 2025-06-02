package com.example.musicPlayer

interface View {
    fun open()
    fun onMusicServiceConnect()
    fun onUpdateAddSong(position: Int)
    fun onUpdateAudioFiles()
}