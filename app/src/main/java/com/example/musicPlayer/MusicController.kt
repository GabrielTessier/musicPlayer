package com.example.musicPlayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class MusicController (private var activity: ComponentActivity, private val onMusicServiceConnect: () -> Unit) {
    private lateinit var seekBar: SeekBar
    private var isUserSeeking = false
    private lateinit var playButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView
    private lateinit var textCardTitle: TextView

    var musicService: MusicService? = null
    private var isBound = false

    private var end = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            update()
            onMusicServiceConnect()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    companion object {
        var musicServiceIntent: Intent? = null
    }

    init {
        reloadVar()

        val intent = Intent(activity, MusicService::class.java)
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        activity.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                while (!end) {
                    update()
                    delay(500)
                }
            }
        }
    }

    fun reloadVar() {
        seekBar = activity.findViewById(R.id.seekBar)
        playButton = activity.findViewById(R.id.btnPlayPause)
        previousButton = activity.findViewById(R.id.btnPrevious)
        nextButton = activity.findViewById(R.id.btnNext)
        textCurrentTime = activity.findViewById(R.id.textCurrentTime)
        textTotalTime = activity.findViewById(R.id.textTotalDuration)
        textCardTitle = activity.findViewById(R.id.cardMusicTitle)

        previousButton.setOnClickListener {
            previousMusic()
        }
        nextButton.setOnClickListener {
            nextMusic()
        }
        playButton.setOnClickListener {
            if (musicService?.mediaPlayer?.isPlaying == true) pauseMusic()
            else startMusic()
        }

        // Déplacement manuel de la SeekBar par l’utilisateur
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isUserSeeking = false
                musicService?.mediaPlayer?.seekTo(seekBar.progress)
                updateTextDuration()
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Rien à faire ici
            }
        })
    }

    fun onStop() {
        if (isBound) {
            activity.unbindService(connection)
            isBound = false
        }
        end = true
    }


    private fun updateSeekBar() {
        seekBar.max = musicService?.mediaPlayer?.duration ?: 0
        if (!isUserSeeking && musicService?.mediaPlayer?.isPlaying == true) {
            seekBar.progress = musicService?.mediaPlayer?.currentPosition?:0
            textCurrentTime.text = Utils.formatTime(musicService?.mediaPlayer?.currentPosition?:0)
        }
    }

    private fun updatePlayButton() {
        if (musicService?.mediaPlayer?.isPlaying == true)
            playButton.setImageResource(android.R.drawable.ic_media_pause)
        else
            playButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun updateTextDuration() {
        textCurrentTime.text = Utils.formatTime(musicService?.mediaPlayer?.currentPosition?:0)
        textTotalTime.text = Utils.formatTime(musicService?.mediaPlayer?.duration?:0)
    }

    private fun updateTextTitle() {
        if (musicService?.currentAudioIndex != null) {
            val index = musicService?.currentAudioIndex!!
            val audioFiles = musicService?.audioFiles
            if (audioFiles != null && index >= 0 && index < audioFiles.size) {
                textCardTitle.text = audioFiles[index].title
            }
        }
    }

    fun update() {
        if (isBound) {
            updatePlayButton()
            updateSeekBar()
            updateTextDuration()
            updateTextTitle()
        }
    }

    private fun startMusic() {
        musicService?.startMusic()
        update()
    }

    private fun pauseMusic() {
        musicService?.pauseMusic()
        update()
    }

    private fun previousMusic() {
        musicService?.let {
            if ((it.mediaPlayer?.currentPosition ?: 0) < 3000) {
                if (it.currentAudioIndex != 0) it.currentAudioIndex--
                else it.currentAudioIndex = it.audioFiles.size - 1
            } else {
                it.mediaPlayer?.seekTo(0)
            }
            it.playCurrentAudio()
        }
    }

    private fun nextMusic() {
        musicService?.let {
            if (it.currentAudioIndex != it.audioFiles.size-1) it.currentAudioIndex++
            else it.currentAudioIndex = 0
            it.playCurrentAudio()
        }
    }

    fun playAudio(audioFiles: List<AudioFile>, initialIndex: Int) {
        if (musicServiceIntent != null) activity.stopService(musicServiceIntent)
        musicServiceIntent = Intent(activity, MusicService::class.java).apply {
            putParcelableArrayListExtra("audioFiles", ArrayList(audioFiles))
            putExtra("initialIndex", initialIndex)
        }
        activity.startService(musicServiceIntent)

        update()
    }
}