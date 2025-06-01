package com.example.musicPlayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.SeekBar
import java.util.Locale
import androidx.core.net.toUri
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.TextView

class MusicService : Service() {
    private var mediaPlayer: MediaPlayer? = null
    private var audioFiles: List<AudioFile> = emptyList()
    private var currentAudioIndex: Int = -1

    private lateinit var mediaSession: MediaSessionCompat

    private lateinit var seekBar: SeekBar
    private lateinit var playButton: ImageButton
    private lateinit var previousButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var textCurrentTime: TextView
    private lateinit var textTotalTime: TextView

    private val handler = Handler(Looper.getMainLooper())
    private var isUserSeeking = false

    companion object {
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        initMediaSession()

        seekBar = mainActivity.findViewById(R.id.seekBar)
        playButton = mainActivity.findViewById(R.id.btnPlayPause)
        previousButton = mainActivity.findViewById(R.id.btnPrevious)
        nextButton = mainActivity.findViewById(R.id.btnNext)
        textCurrentTime = mainActivity.findViewById(R.id.textCurrentTime)
        textTotalTime = mainActivity.findViewById(R.id.textTotalDuration)

        previousButton.setOnClickListener {
            previousMusic()
        }
        nextButton.setOnClickListener {
            nextMusic()
        }

        playButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) pauseMusic()
            else startMusic()
        }
    }

    private fun initMediaSession() {
        mediaSession = MediaSessionCompat(this, "MyMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    startMusic()
                }

                override fun onPause() {
                    pauseMusic()
                }

                override fun onSkipToNext() {
                    nextMusic()
                }

                override fun onSkipToPrevious() {
                    previousMusic()
                }
            })
            //setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            isActive = true
        }

        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, mediaPlayer?.currentPosition?.toLong()?:0, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val audioFilesExtra = intent?.getParcelableArrayListExtra("audioFiles", AudioFile::class.java)
        val initialIndex = intent?.getIntExtra("initialIndex", -1) ?: -1

        if (!audioFilesExtra.isNullOrEmpty()) {
            audioFiles = audioFilesExtra
            currentAudioIndex = initialIndex
            playCurrentAudio()
        }
        return START_STICKY
    }

    private fun playCurrentAudio() {
        if (currentAudioIndex >= 0 && currentAudioIndex < audioFiles.size) {
            //mediaPlayer?.release() // Libérer le MediaPlayer actuel s'il existe
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }

            Log.d("playCurrentAudio", String.format(Locale.FRANCE, "Lecture musique : %d / %d", currentAudioIndex, audioFiles.size))

            val currentAudio = audioFiles[currentAudioIndex]
            mainActivity.musicAdapter.setSelectedAudioId(currentAudio.id, currentAudioIndex)
            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(mainActivity, currentAudio.data.toUri())
            mediaPlayer?.prepare()

            seekBar.max = mediaPlayer?.duration?:0

            // Déplacement manuel de la SeekBar par l’utilisateur
            seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    isUserSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    isUserSeeking = false
                    mediaPlayer?.seekTo(seekBar.progress)
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    // Rien à faire ici
                }
            })

            mediaPlayer?.setOnCompletionListener { nextMusic() }
            mediaPlayer?.isLooping = false
            startMusic()
            updateTextDuration()

            startForeground(NOTIFICATION_ID, createNotification(currentAudio.title, currentAudio.artist))
        } else {
            // Si nous avons atteint la fin de la liste, arrêter le service
            stopSelf()
        }
    }

    private fun startMusic() {
        mediaPlayer?.start()
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        updateSeekBar()
        updatePlayButton()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
        updateSeekBar()
        updatePlayButton()
    }

    private fun previousMusic() {
        if ((mediaPlayer?.currentPosition ?: 0) < 3000) {
            if (currentAudioIndex != 0) currentAudioIndex--
            else currentAudioIndex = audioFiles.size-1
        } else {
            mediaPlayer?.seekTo(0)
        }
        playCurrentAudio()
    }

    private fun nextMusic() {
        if (currentAudioIndex != audioFiles.size-1) currentAudioIndex++
        else currentAudioIndex = 0
        playCurrentAudio()
    }

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (!isUserSeeking && mediaPlayer?.isPlaying == true) {
                    seekBar.progress = mediaPlayer?.currentPosition?:0
                    textCurrentTime.text = formatTime(mediaPlayer?.currentPosition?:0)
                }
                handler.postDelayed(this, 500)
            }
        }, 0)
    }

    private fun updatePlayButton() {
        if (mediaPlayer?.isPlaying == true)
            playButton.setImageResource(android.R.drawable.ic_media_pause)
        else
            playButton.setImageResource(android.R.drawable.ic_media_play)
    }

    private fun updateTextDuration() {
        textCurrentTime.text = formatTime(mediaPlayer?.currentPosition?:0)
        textTotalTime.text = formatTime(mediaPlayer?.duration?:0)
    }

    private fun formatTime(milliseconds: Int): String {
        val minutes = (milliseconds / 1000) / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format(Locale.FRANCE, "%d:%02d", minutes, seconds)
    }

    private fun createNotification(title: String, artist: String): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lecture audio",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Précédent", null))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", null))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Suivant", null))
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        mediaSession.release()
        handler.removeCallbacksAndMessages(null)
    }
}
