package com.example.musicPlayer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
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
import java.util.Locale

class MusicController (private var activity: ComponentActivity, private val onMusicServiceConnect: () -> Unit) {
    private var seekBar: SeekBar = activity.findViewById(R.id.seekBar)
    private var isUserSeeking = false
    private var playButton: ImageButton = activity.findViewById(R.id.btnPlayPause)
    private var previousButton: ImageButton = activity.findViewById(R.id.btnPrevious)
    private var nextButton: ImageButton = activity.findViewById(R.id.btnNext)
    private var textCurrentTime: TextView = activity.findViewById(R.id.textCurrentTime)
    private var textTotalTime: TextView = activity.findViewById(R.id.textTotalDuration)
    private var textCardTitle: TextView = activity.findViewById(R.id.cardMusicTitle)

    var musicServiceIntent: Intent? = null
    var musicService: MusicService? = null
    private var isBound = false

    private var end = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            onMusicServiceConnect()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    init {
        val intent = Intent(activity, MusicService::class.java)
        activity.bindService(intent, connection, Context.BIND_AUTO_CREATE)

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

        activity.lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                while (!end) {
                    update()
                    delay(500)
                }
            }
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
            textCurrentTime.text = formatTime(musicService?.mediaPlayer?.currentPosition?:0)
        }
    }

    private fun updatePlayButton() {
        if (musicService?.mediaPlayer?.isPlaying == true)
            playButton.setImageResource(android.R.drawable.ic_media_pause)
        else
            playButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun updateTextDuration() {
        textCurrentTime.text = formatTime(musicService?.mediaPlayer?.currentPosition?:0)
        textTotalTime.text = formatTime(musicService?.mediaPlayer?.duration?:0)
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
        updatePlayButton()
        updateSeekBar()
        updateTextDuration()
        updateTextTitle()
    }

    private fun startMusic() {
        musicService?.mediaPlayer?.start()
        musicService?.updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        updateSeekBar()
        updatePlayButton()
    }

    private fun pauseMusic() {
        musicService?.mediaPlayer?.pause()
        musicService?.updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        updateSeekBar()
        updatePlayButton()
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

    fun formatTime(milliseconds: Long): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format(Locale.FRANCE, "%d:%02d", minutes, seconds)
    }
    fun formatTime(milliseconds: Int): String {
        return formatTime(milliseconds.toLong())
    }

    fun playAudio(audioFiles: List<AudioFile>, initialIndex: Int) {
        if (musicServiceIntent != null) activity.stopService(musicServiceIntent)
        musicServiceIntent = Intent(activity, MusicService::class.java).apply {
            putParcelableArrayListExtra("audioFiles", ArrayList(audioFiles))
            putExtra("initialIndex", initialIndex)
        }
        activity.startService(musicServiceIntent)

        updatePlayButton()
        updateTextDuration()
        updateSeekBar()
    }
}