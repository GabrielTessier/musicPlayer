package com.example.musicPlayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.util.Locale
import androidx.core.net.toUri
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MusicService : Service() {
    var mediaPlayer: MediaPlayer? = null
    var audioFiles: List<AudioFile> = emptyList()
    var currentAudioIndex: Int = -1

    private lateinit var mediaSession: MediaSessionCompat

    private val handler = Handler(Looper.getMainLooper())
    private val updateProgress = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            }
            handler.postDelayed(this, 1000) // Mettre à jour toutes les secondes
        }
    }

    companion object {
        const val CHANNEL_ID = "MusicPlayerChannel"
        const val NOTIFICATION_ID = 1
        var isServiceRunning: Boolean = false
    }

    private val binder = LocalBinder()
    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        initMediaSession()
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

    fun updatePlaybackState(state: Int) {
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

    fun playCurrentAudio() {
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

            mediaPlayer = MediaPlayer()
            mediaPlayer?.setDataSource(this, currentAudio.data.toUri())
            mediaPlayer?.prepare()

            mediaPlayer?.setOnCompletionListener { nextMusic() }
            mediaPlayer?.isLooping = false
            startMusic()

            val intent = Intent("ACTION_FROM_SERVICE")
            intent.putExtra("audio", currentAudio)
            intent.putExtra("audioIndex", currentAudioIndex)
            //sendBroadcast(intent)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)

            // Affichage
            startForeground(NOTIFICATION_ID, createNotification(currentAudio.title, currentAudio.artist))
        } else {
            // Si nous avons atteint la fin de la liste, arrêter le service
            stopSelf()
        }
    }

    fun startMusic() {
        mediaPlayer?.start()
        handler.removeCallbacks(updateProgress)
        handler.post(updateProgress)
        updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
    }

    fun pauseMusic() {
        mediaPlayer?.pause()
        handler.removeCallbacks(updateProgress)
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    fun previousMusic() {
        if ((mediaPlayer?.currentPosition ?: 0) < 3000) {
            if (currentAudioIndex != 0) currentAudioIndex--
            else currentAudioIndex = audioFiles.size - 1
        } else {
            mediaPlayer?.seekTo(0)
        }
        playCurrentAudio()
    }

    fun nextMusic() {
        if (currentAudioIndex != audioFiles.size-1) currentAudioIndex++
        else currentAudioIndex = 0
        playCurrentAudio()
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
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_previous, "Précédent", null))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_pause, "Pause", null))
            .addAction(NotificationCompat.Action(android.R.drawable.ic_media_next, "Suivant", null))
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(updateProgress)
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
        mediaSession.release()
        isServiceRunning = false
        handler.removeCallbacksAndMessages(null)
    }
}
