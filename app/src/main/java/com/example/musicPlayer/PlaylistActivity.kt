package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore.Audio
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class PlaylistActivity : ComponentActivity() {

    private lateinit var playlistManager: PlaylistManager
    private lateinit var playlist: Playlist

    private lateinit var recyclerView: RecyclerView
    lateinit var musicAdapter: MusicAdapter
    private lateinit var items: MutableList<Item>

    private var isDelete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist_manage_layout)

        playlistManager = PlaylistManager(this) {}

        val playlistExtra = intent.getParcelableExtra("playlist", Playlist::class.java)
        if (playlistExtra == null) {
            finish()
        }
        playlist = playlistExtra!!

        recyclerView = findViewById(R.id.musics)
        recyclerView.layoutManager = LinearLayoutManager(this)
        updateItemList(playlist.audios)
        musicAdapter = MusicAdapter(this, items) { audioFile ->
        }
        recyclerView.adapter = musicAdapter

        val btnRetour = findViewById<Button>(R.id.btnRetour)
        btnRetour.setOnClickListener {
            finish()  // Termine MusicActivity et retourne à MainActivity
        }

        val textTitre = findViewById<TextView>(R.id.title)
        textTitre.text = playlist.name
        updateNbElem()

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            isDelete = true
            finish()
        }

        val btnAddMusic = findViewById<Button>(R.id.btnAddMusic)
        btnAddMusic.setOnClickListener {
            val main:MainActivity? = MainActivity.main
            if (main != null) {
                addMusic(main.audioFiles[0])
            }
        }
    }

    private fun addItem(audio: AudioFile) {
        val pos = items.size-1
        items.add(pos,
            Item.RealItem(
                id = audio.id,
                title = audio.title,
                artist = audio.artist,
                duration = audio.duration,
                albumArtUri = audio.albumArtUri,
                data = audio.data
            )
        )
        musicAdapter.notifyItemInserted(pos)
    }
    private fun updateItemList(audioFiles: List<AudioFile>) {
        items = MutableList(audioFiles.size+1) { index: Int ->
            if (index != audioFiles.size) {
                val audio = audioFiles[index]
                Item.RealItem(
                    id = audio.id,
                    title = audio.title,
                    artist = audio.artist,
                    duration = audio.duration,
                    albumArtUri = audio.albumArtUri,
                    data = audio.data
                )
            } else {
                Item.FakeItem(-1)
            }
        }
    }

    private fun updateNbElem() {
        val textNbElem = findViewById<TextView>(R.id.nbElem)
        textNbElem.text = String.format(Locale.FRANCE, "Nombre d'élément : %d", playlist.audios.size)
    }

    fun addMusic(audio: AudioFile) {
        playlistManager.addAudioToPlaylist(playlistId = playlist.id, audio = audio)
        playlist = playlistManager.getPlaylist(playlistId = playlist.id)!!
        addItem(audio)
        updateNbElem()
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("delete", isDelete)
            putExtra("id", playlist.id)
        })
        super.finish()
    }

    /*override fun onDestroy() {
        super.onDestroy()
    }*/
}