package com.example.musicPlayer

import android.os.Bundle
import androidx.activity.ComponentActivity

class PlaylistActivity : ComponentActivity(), ActivityInterface {

    override lateinit var audioFiles: ArrayList<AudioFile>
    private lateinit var footerController: FooterController<PlaylistActivity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist_layout)

        audioFiles = intent.getParcelableArrayListExtra("audioFiles", AudioFile::class.java) ?: arrayListOf()

        footerController = FooterController(this)
    }

}