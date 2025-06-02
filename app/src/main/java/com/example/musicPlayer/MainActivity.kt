package com.example.musicPlayer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.ArrayList
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import android.content.res.ColorStateList

class MainActivity : ComponentActivity() {
    var audioFiles : ArrayList<AudioFile> = ArrayList()
    lateinit var musicController: MusicController

    private var view: Int = SONG_VIEW
    private lateinit var songView: SongView
    private lateinit var playlistView: PlaylistView

    companion object {
        const val SONG_VIEW = 0
        const val PLAYLIST_VIEW = 1
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audio = intent?.getParcelableExtra("audio", AudioFile::class.java)
            val audioIndex = intent?.getIntExtra("audioIndex", 0)
            if (audio != null) {
                if (view == SONG_VIEW) {
                    songView.musicAdapter.setSelectedAudioId(audio.id, audioIndex ?: 0)
                } else if (view == PLAYLIST_VIEW) {
                    // Rien pour l'instant
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("ACTION_FROM_SERVICE"))

        songView = SongView(this)
        playlistView = PlaylistView(this)

        Log.d("MyDebug", "test1")

        changeView(SONG_VIEW)

        checkPermission()
    }

    private fun changeView(view: Int) {
        this.view = view
        if (view == SONG_VIEW) {
            musicController =
                if (MusicService.isServiceRunning) {
                    MusicController(this) {
                        audioFiles.clear()
                        for (audio in musicController.musicService?.audioFiles?:ArrayList()) {
                            audioFiles.add(audio)
                        }
                        songView.onMusicServiceConnect()
                    }
                } else {
                    MusicController(this) {}
                }
            songView.open()
        } else if (view == PLAYLIST_VIEW) {
            playlistView.open()
        }
        setFooterOnClickListener()
        updateFooter()
    }

    private fun updateFooter() {
        val musique: Button = findViewById(R.id.musicButton)
        val playlist: Button = findViewById(R.id.playlistButton)
        val defaultColor = ContextCompat.getColor(this, R.color.footer_default_background)
        val activeColor = ContextCompat.getColor(this, R.color.footer_active_background)
        musique.backgroundTintList = ColorStateList.valueOf(if (view == SONG_VIEW) activeColor else defaultColor)
        playlist.backgroundTintList = ColorStateList.valueOf(if (view == PLAYLIST_VIEW) activeColor else defaultColor)
    }

    private fun setFooterOnClickListener() {
        val musique: Button = findViewById(R.id.musicButton)
        val playlist: Button = findViewById(R.id.playlistButton)
        musique.setOnClickListener {
            if (view != SONG_VIEW) {
                setContentView(R.layout.activity_main)
                changeView(SONG_VIEW)
            }
        }
        playlist.setOnClickListener {
            if (view != PLAYLIST_VIEW) {
                setContentView(R.layout.playlist_layout)
                changeView(PLAYLIST_VIEW)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission accordée
            updateAudioFiles { audioFile ->
                val position = audioFiles.size
                audioFiles.add(audioFile)
                songView.onUpdateAddSong(position)
                playlistView.onUpdateAddSong(position)
            }
        } else {
            // Permission refusée
            Toast.makeText(
                this,
                "Permission refusée pour lire les fichiers audio.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                // Permission déjà accordée
                updateAudioFiles { audioFile ->
                    val position = audioFiles.size
                    audioFiles.add(audioFile)
                    songView.onUpdateAddSong(position)
                    playlistView.onUpdateAddSong(position)
                }
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_MEDIA_AUDIO) -> {
                // Expliquer à l'utilisateur pourquoi l'application a besoin de la permission
                Toast.makeText(
                    this,
                    "L'application a besoin de cette permission pour lire les fichiers audio.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                // Demander la permission
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }
    }

    private fun updateAudioFiles(onFileScanned: (AudioFile) -> Unit) {
        clearAudioFiles()
        getAllAudioFiles(onFileScanned)
        songView.onUpdateAudioFiles()
        playlistView.onUpdateAudioFiles()
    }

    private fun clearAudioFiles() {
        audioFiles.clear()
    }

    private fun getAllAudioFiles(onFileScanned: (AudioFile) -> Unit) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val cursor = contentResolver.query(uri, projection, selection, null, null)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val path = it.getString(dataColumn)
                val duration = it.getLong(durationColumn)
                val albumId = it.getLong(albumIdColumn)

                if (duration != 0L) {
                    val albumArtUri = getAlbumArtUri(albumId)
                    val audioFile = AudioFile(
                        id = id,
                        title = title,
                        artist = artist,
                        duration = duration,
                        albumArtUri = albumArtUri,
                        data = path
                    )
                    onFileScanned(audioFile)
                }
            }
        }
    }

    private fun getAlbumArtUri(albumId: Long): String {
        val albumArtUri = "content://media/external/audio/albumart".toUri()
        return ContentUris.withAppendedId(albumArtUri, albumId).toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        musicController.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        if (musicController.musicServiceIntent != null && musicController.musicService?.mediaPlayer?.isPlaying == false) stopService(musicController.musicServiceIntent)
    }
}
