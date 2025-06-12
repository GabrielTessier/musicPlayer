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

class MusicActivity : ComponentActivity() {
    private var musicController: MusicController? = null

    private var bindToMusicPlaying: Boolean = true
    private var audio: AudioFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_desc)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("ACTION_FROM_SERVICE"))

        val audioExtra = intent.getParcelableExtra("audio", AudioFile::class.java)
        if (audioExtra != null) {
            bindToMusicPlaying = false
            audio = audioExtra
            setViewContent(audio!!)
        }

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
            if (bindToMusicPlaying) {
                musicController?.musicService?.let { service ->
                    val audio = service.audioFiles[service.currentAudioIndex]
                    setViewContent(audio)
                }
            }
        }
        musicController?.update()
    }

    private fun setViewContent(audio: AudioFile) {
        val textTitle = findViewById<TextView>(R.id.title)
        textTitle.text = audio.title
        val textArtist = findViewById<TextView>(R.id.artist)
        textArtist.text = audio.artist
        val textDuration = findViewById<TextView>(R.id.duration)
        textDuration.text = Utils.formatTime(audio.duration)

        val imageView = findViewById<ImageView>(R.id.image)
        Utils.loadAlbumArt(this, audio.albumArtUri, imageView, R.drawable.music_disk)
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

    override fun onDestroy() {
        super.onDestroy()
        musicController?.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
}