package com.example.musicPlayer

import android.annotation.SuppressLint
import android.content.Intent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class SongView(private val main: MainActivity): com.example.musicPlayer.View {
    private lateinit var recyclerView: RecyclerView
    private var scrollY: Int = 0
    lateinit var musicAdapter: MusicAdapter
    private var musicController: MusicController

    private lateinit var musicControlCardView: CardView
    private lateinit var musicControlLinearLayout: LinearLayout

    private var items: MutableList<Item> = mutableListOf()

    init {
        musicController =
            if (MusicService.isServiceRunning) {
                MusicController(main) {
                    /*main.audioFiles.clear()
                    for (audio in musicController.musicService?.audioFiles?: ArrayList()) {
                        main.audioFiles.add(audio)
                    }*/
                    onMusicServiceConnect()
                }
            } else {
                MusicController(main) {
                    onMusicServiceConnect()
                }
            }
        updateItemList(emptyList())
        musicAdapter = MusicAdapter(main, R.menu.overflow_menu_music, 1, items) { audioFile ->
            // Gérer la lecture de l'audio ici
            if (musicAdapter.getLastSelectedAudioId() != audioFile.id) {
                val audioFiles = PlaylistManager.getAudioFiles()
                val index = audioFiles.indexOfFirst { it.id == audioFile.id }
                musicController.playAudio(audioFiles, index)
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
        setupCardMusicControl()

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
            musicAdapter.setSelectedAudioId(PlaylistManager.getAudioFiles()[index].id)
        }
    }

    override fun onUpdateAddSong(audio: AudioFile, position: Int) {
        val item = Utils.audioFileToItem(audio)
        items.add(position, item)
        musicAdapter.notifyItemChanged(position)
        musicAdapter.notifyItemInserted(position+1)

        recyclerView.post {
            recyclerView.invalidateItemDecorations()
        }
    }

    override fun onDestroy() {
        musicController.onStop()
    }

    private fun setupCardMusicControl() {
        val gestureDetector = GestureDetector(main, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val diffX = e2.x - e1.x
                if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    // Swipe horizontal détecté
                    musicControlCardView.animate()
                        .translationX(if (diffX > 0) 1000f else -1000f)
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction {
                            musicControlCardView.visibility = View.GONE
                        }
                        .start()
                    musicController.pauseMusic()
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                val intent = Intent(main, MusicActivity::class.java)
                main.startActivity(intent)
                return true
            }
        })

        musicControlLinearLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }
}

/*
class Test: GestureDetector.OnGestureListener {
    override fun onDown(e: MotionEvent): Boolean {
        TODO("Not yet implemented")
    }

    override fun onShowPress(e: MotionEvent) {
        TODO("Not yet implemented")
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        TODO("Not yet implemented")
    }

    override fun onScroll(
        e1: MotionEvent?,
        e2: MotionEvent,
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun onLongPress(e: MotionEvent) {
        TODO("Not yet implemented")
    }

    override fun onFling(
        e1: MotionEvent?,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        TODO("Not yet implemented")
    }

}*/