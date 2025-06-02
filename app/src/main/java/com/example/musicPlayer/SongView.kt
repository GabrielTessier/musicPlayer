package com.example.musicPlayer

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class SongView(private val main: MainActivity): com.example.musicPlayer.View {
    private lateinit var recyclerView: RecyclerView
    private var scrollY: Int = 0
    lateinit var musicAdapter: MusicAdapter
    private lateinit var musicController: MusicController

    private lateinit var musicControlCardView: CardView
    private lateinit var musicControlLinearLayout: LinearLayout

    init {
        musicAdapter = MusicAdapter(main.audioFiles) { audioFile ->
            // Gérer la lecture de l'audio ici
            val index = main.audioFiles.indexOfFirst { it.id == audioFile.id }
            musicController.playAudio(main.audioFiles, index)
            // Mettre à jour l'ID de la musique actuellement jouée
            musicAdapter.setSelectedAudioId(audioFile.id, index)
            toggleCardViewVisibility(true)
        }

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
                MusicController(main) {}
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

    override fun onMusicServiceConnect() {
        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)
        if (musicController.musicService?.mediaPlayer?.isPlaying == true) {
            val index = musicController.musicService?.currentAudioIndex?:0
            musicAdapter.selectedAudioId = main.audioFiles[index].id
        }
    }

    override fun onUpdateAddSong(position: Int) {
        musicAdapter.notifyItemInserted(position)
        recyclerView.post {
            recyclerView.invalidateItemDecorations()
        }
    }

    override fun onUpdateAudioFiles() {
        if (musicAdapter.selectedAudioId != null) {
            val index = main.audioFiles.indexOfFirst { it.id == musicAdapter.selectedAudioId }
            musicAdapter.setSelectedAudioId(musicAdapter.selectedAudioId?:0, index)
        }
    }
}