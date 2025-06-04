package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SelectMusicActivity : ComponentActivity() {

    private lateinit var playlistManager: PlaylistManager

    private lateinit var recyclerView: RecyclerView
    private lateinit var musicAdapter: MusicAdapter
    private lateinit var items: MutableList<Item>

    private var audioListSelect: ArrayList<AudioFile> = arrayListOf()
    private var isValidate: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.select_music_layout)

        playlistManager = PlaylistManager(this) {}

        recyclerView = findViewById(R.id.musics)
        recyclerView.layoutManager = LinearLayoutManager(this)
        updateItemList(MainActivity.main?.audioFiles?: emptyList())
        musicAdapter = MusicAdapter(this, items.size, items) { audioItem ->
            val index = audioListSelect.indexOfFirst { it.id == audioItem.id }
            if (index == -1) {
                audioListSelect.add(Utils.itemToAudioFile(audioItem))
                musicAdapter.setSelectedAudioId(audioItem.id)
            } else {
                audioListSelect.removeAt(index)
                musicAdapter.unsetSelectedAudioId(audioItem.id)

            }
        }
        musicAdapter.setSelectedColor(Color.LTGRAY)
        musicAdapter.setLastSelectedColor(Color.GRAY)
        recyclerView.adapter = musicAdapter

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

    /*private fun addItem(audio: AudioFile) {
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
    }*/

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

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("validate", isValidate)
            putParcelableArrayListExtra("audioList", audioListSelect)
        })
        super.finish()
    }
}