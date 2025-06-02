package com.example.musicPlayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide

class MusicActivity : ComponentActivity() {

    lateinit var audioFiles: ArrayList<AudioFile>
    private var musicController: MusicController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_desc)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("ACTION_FROM_SERVICE"))

        audioFiles = intent.getParcelableArrayListExtra("audioFiles", AudioFile::class.java) ?: arrayListOf()

        updateMusicController()

        val btnRetour = findViewById<Button>(R.id.btnRetour)
        btnRetour.setOnClickListener {
            finish()  // Termine MusicActivity et retourne à MainActivity
        }

        val musicTextCard: TextView = findViewById(R.id.cardMusicTitle)
        musicTextCard.visibility = View.GONE
    }

    fun updateMusicController() {
        musicController?.onStop()
        musicController = MusicController(this) {
            musicController?.musicService?.let {
                val audio = audioFiles[it.currentAudioIndex]
                val textTitle = findViewById<TextView>(R.id.title)
                textTitle.text = audio.title
                val textArtist = findViewById<TextView>(R.id.artist)
                textArtist.text = audio.artist
                val textDuration = findViewById<TextView>(R.id.duration)
                textDuration.text = musicController?.formatTime(audio.duration)

                val imageView = findViewById<ImageView>(R.id.image)
                loadAlbumArt(audio.albumArtUri, imageView)
            }
        }
        musicController?.update()
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audio = intent?.getParcelableExtra("audio", AudioFile::class.java)
            //val audioIndex = intent?.getIntExtra("audioIndex", 0)
            if (audio != null) {
                updateMusicController()
            }
        }
    }

    private fun loadAlbumArt(albumArtUri: String?, imageView: ImageView) {
        albumArtUri?.let {
            Glide.with(this)
                .load(it)
                .placeholder(R.drawable.music_disk) // Image de remplacement
                .into(imageView)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
}