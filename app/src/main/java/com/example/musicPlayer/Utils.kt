package com.example.musicPlayer

import android.app.Activity
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide

object Utils {
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
}