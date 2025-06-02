package com.example.musicPlayer

import android.content.Intent
import android.widget.ImageView
import androidx.activity.ComponentActivity

class FooterController<T>(private var activity: T) where T : ComponentActivity, T : ActivityInterface {
    private val mainImage: ImageView = activity.findViewById(R.id.footerImageMain)
    private val playlistImage: ImageView = activity.findViewById(R.id.footerImagePlaylist)

    init {
        mainImage.setOnClickListener {
            if (activity::class.java != MainActivity::class.java) {
                val intent = Intent(activity, MainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                activity.startActivity(intent)
                activity.finish()
            }
        }
        playlistImage.setOnClickListener {
            if (activity::class.java != PlaylistActivity::class.java) {
                val intent = Intent(activity, PlaylistActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                intent.putParcelableArrayListExtra("audioFiles", activity.audioFiles)
                activity.startActivity(intent)
                activity.finish()
            }
        }
    }
}