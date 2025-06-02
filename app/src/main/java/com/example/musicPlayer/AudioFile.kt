package com.example.musicPlayer

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val albumArtUri: String,
    val data: String
) : Parcelable

data class Playlist(
    val id: Long,
    val name: String,
    val audios: MutableList<AudioFile>
)