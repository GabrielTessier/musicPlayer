package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import java.util.Locale

class PlaylistActivity : ComponentActivity() {

    private lateinit var playlistManager: PlaylistManager
    private lateinit var playlist: Playlist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist_manage_layout)

        playlistManager = PlaylistManager(this) {}

        val playlistExtra = intent.getParcelableExtra("playlist", Playlist::class.java)
        if (playlistExtra == null) {
            finish()
        }
        playlist = playlistExtra!!

        val btnRetour = findViewById<Button>(R.id.btnRetour)
        btnRetour.setOnClickListener {
            finish()  // Termine MusicActivity et retourne à MainActivity
        }

        val textTitre = findViewById<TextView>(R.id.title)
        textTitre.text = playlist.name
        val textNbElem = findViewById<TextView>(R.id.nbElem)
        textNbElem.text = String.format(Locale.FRANCE, "Nombre d'élément : %d", playlist.audios.size)

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("delete", true)
                putExtra("id", playlist.id)
            })
            finish()
        }
    }

    /*override fun onDestroy() {
        super.onDestroy()
    }*/
}