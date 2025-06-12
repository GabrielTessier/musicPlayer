package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectPlaylistActivity: ComponentActivity() {
    private lateinit var playlistManager: PlaylistManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var playlistAdapter: PlaylistAdapter
    private lateinit var items: MutableList<PlaylistItem>

    private var playlistListSelect: ArrayList<Playlist> = arrayListOf()
    private var isValidate: Boolean = false

    private var audio: AudioFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_layout)

        audio = intent.getParcelableExtra("audio", AudioFile::class.java)

        playlistManager = PlaylistManager(this) {}

        recyclerView = findViewById(R.id.musics)
        recyclerView.layoutManager = LinearLayoutManager(this)
        updateItemList(PlaylistManager.getPlaylists())
        playlistAdapter = PlaylistAdapter(items.size, items) { playlistItem ->
            val index = playlistListSelect.indexOfFirst { it.id == playlistItem.id }
            if (index == -1) {
                playlistListSelect.add(Utils.itemToPlaylist(playlistItem))
                playlistAdapter.setSelectedAudioId(playlistItem.id)
            } else {
                playlistListSelect.removeAt(index)
                playlistAdapter.unsetSelectedAudioId(playlistItem.id)

            }
        }
        playlistAdapter.setSelectedColor(Color.LTGRAY)
        playlistAdapter.setLastSelectedColor(Color.GRAY)
        recyclerView.adapter = playlistAdapter

        val btnAnnule = findViewById<Button>(R.id.btnAnnule)
        btnAnnule.setOnClickListener {
            finish()
        }

        val btnValider = findViewById<Button>(R.id.btnValidate)
        btnValider.setOnClickListener {
            isValidate = true
            finish()
        }
    }

    private fun updateItemList(playlists: List<Playlist>) {
        items = MutableList(playlists.size+1) { index: Int ->
            if (index != playlists.size) {
                val playlist = playlists[index]
                Utils.playlistToItem(playlist)
            } else {
                PlaylistItem.FakeItem(-1)
            }
        }
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("validate", isValidate)
            putParcelableArrayListExtra("playlistList", playlistListSelect)
            putExtra("audio", audio)
        })
        super.finish()
    }
}