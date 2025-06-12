package com.example.musicPlayer

import android.app.Activity
import android.content.ContentUris
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import java.util.Locale

object Utils {
    fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format(Locale.FRANCE, "%d:%02d", minutes, seconds)
    }
    fun formatTime(milliseconds: Int): String {
        return formatTime(milliseconds.toLong())
    }

    fun formatDuration(duration: Long): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format(Locale.FRANCE, "%02d:%02d", minutes, seconds)
    }

    fun loadAlbumArt(activity: Activity, albumArtUri: String?, imageView: ImageView, placeholder: Drawable) {
        albumArtUri?.let {
            Glide.with(activity)
                .load(it)
                .placeholder(placeholder) // Image de remplacement
                .into(imageView)
        }
    }
    fun loadAlbumArt(activity: Activity, albumArtUri: String?, imageView: ImageView, placeholder: Int) {
        albumArtUri?.let {
            Glide.with(activity)
                .load(it)
                .placeholder(placeholder) // Image de remplacement
                .into(imageView)
        }
    }
    fun getAlbumArtUri(albumId: Long): String {
        val albumArtUri = "content://media/external/audio/albumart".toUri()
        return ContentUris.withAppendedId(albumArtUri, albumId).toString()
    }

    fun itemToAudioFile(item: Item.RealItem): AudioFile {
        return AudioFile(
            id = item.id,
            title = item.title,
            artist = item.artist,
            duration = item.duration,
            albumArtUri = item.albumArtUri,
            data = item.data
        )
    }
    fun audioFileToItem(audioFile: AudioFile): Item.RealItem {
        return Item.RealItem(
            id = audioFile.id,
            title = audioFile.title,
            artist = audioFile.artist,
            duration = audioFile.duration,
            albumArtUri = audioFile.albumArtUri,
            data = audioFile.data
        )
    }

    fun itemToPlaylist(item: PlaylistItem.RealItem): Playlist {
        return Playlist(
            id = item.id,
            name = item.name,
            audios = item.audios
        )
    }
    fun playlistToItem(playlist: Playlist): PlaylistItem.RealItem {
        return PlaylistItem.RealItem(
            id = playlist.id,
            name = playlist.name,
            audios = playlist.audios
        )
    }
}