package com.example.musicPlayer

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.collections.ArrayList

class SongView(private val main: MainActivity): com.example.musicPlayer.View {
    private lateinit var recyclerView: RecyclerView
    private var scrollY: Int = 0
    lateinit var musicAdapter: MusicAdapter
    private lateinit var musicController: MusicController

    private lateinit var musicControlCardView: CardView
    private lateinit var musicControlLinearLayout: LinearLayout

    private lateinit var items: MutableList<Item>

    init {
        musicController =
            if (MusicService.isServiceRunning) {
                MusicController(main) {
                    main.audioFiles.clear()
                    for (audio in musicController.musicService?.audioFiles?: ArrayList()) {
                        main.audioFiles.add(audio)
                    }
                    onMusicServiceConnect()
                }
            } else {
                MusicController(main) {
                    onMusicServiceConnect()
                }
            }
        updateItemList(main.audioFiles)
        musicAdapter = MusicAdapter(main, 1, items) { audioFile ->
            // Gérer la lecture de l'audio ici
            if (musicAdapter.getLastSelectedAudioId() != audioFile.id) {
                val index = main.audioFiles.indexOfFirst { it.id == audioFile.id }
                musicController.playAudio(main.audioFiles, index)
                // Mettre à jour l'ID de la musique actuellement jouée
                musicAdapter.setSelectedAudioId(audioFile.id)
                toggleCardViewVisibility(true)
            }
        }
    }

    fun updateItemList(audioFiles: List<AudioFile>) {
        items = MutableList(audioFiles.size+1) { index: Int ->
            if (index != audioFiles.size) {
                val audio = audioFiles[index]
                Utils.audioFileToItem(audio)
            } else {
                Item.FakeItem(-1)
            }
        }
    }

    override fun open() {
        musicController.reloadVar()

        recyclerView = main.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(main)
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        layoutManager.scrollToPosition(scrollY)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                scrollY = firstVisibleItemPosition
            }
        })

        musicControlCardView = main.findViewById(R.id.musicControlCardView)
        musicControlLinearLayout = main.findViewById(R.id.musicButtonLinearLayout)
        musicControlLinearLayout.setOnClickListener {
            val intent = Intent(main, MusicActivity::class.java)
            intent.putParcelableArrayListExtra("audioFiles", main.audioFiles)
            main.startActivity(intent)
        }

        recyclerView.adapter = musicAdapter

        if (MusicService.isPlaying) {
            val audioFiles = musicController.musicService?.audioFiles
            if (audioFiles != null) {
                val index = musicController.musicService?.currentAudioIndex ?: 0
                val audio = audioFiles[index]
                musicAdapter.setSelectedAudioId(audio.id)
            }
        }
        musicController.update()

        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)
    }

    // Fonction pour afficher ou masquer le CardView
    private fun toggleCardViewVisibility(show: Boolean) {
        if (show) {
            musicControlCardView.visibility = View.VISIBLE  // Afficher le CardView
        } else {
            musicControlCardView.visibility = View.GONE  // Masquer le CardView
        }
    }

    private fun onMusicServiceConnect() {
        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)
        if (musicController.musicService?.mediaPlayer?.isPlaying == true) {
            val index = musicController.musicService?.currentAudioIndex?:0
            musicAdapter.setSelectedAudioId(main.audioFiles[index].id)
        }
    }

    override fun onUpdateAddSong(position: Int) {
        val audio = main.audioFiles[position]
        val item = Utils.audioFileToItem(audio)
        items.add(position, item)

        musicAdapter.notifyItemInserted(position)
        recyclerView.post {
            recyclerView.invalidateItemDecorations()
        }
    }

    override fun onUpdateAudioFiles() {
        /*if (musicAdapter.lastSelectedAudioId != null) {
            val index = main.audioFiles.indexOfFirst { it.id == musicAdapter.lastSelectedAudioId }
            musicAdapter.setSelectedAudioId(musicAdapter.lastSelectedAudioId?:0, index)
        }*/
    }

    override fun onDestroy() {
        musicController.onStop()
    }
}