package com.example.musicPlayer

interface MusicListInterface {
    fun onAddToPlaylist(audioFile: AudioFile)
    fun onInfoMusic(audioFile: AudioFile)
    fun onRemoveMusic(audioFile: AudioFile) {}
}