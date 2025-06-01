package com.example.musicPlayer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MusicActivity : ComponentActivity() {

    private lateinit var audioFiles: ArrayList<AudioFile>
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

                if (audio.imageBytes != null) {
                    val bitmap = BitmapFactory.decodeByteArray(audio.imageBytes, 0, audio.imageBytes.size)
                    val imageView = findViewById<ImageView>(R.id.image)
                    imageView.setImageBitmap(bitmap)
                }
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

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }
}