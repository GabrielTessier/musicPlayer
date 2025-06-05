package com.example.musicPlayer

//import androidx.appcompat.app.AppCompatActivity

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.widget.Button
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object {
        const val SONG_VIEW = 0
        const val PLAYLIST_VIEW = 1
    }

    private var view: Int = SONG_VIEW
    private lateinit var songView: SongView
    private lateinit var playlistView: PlaylistView

    private lateinit var playlistManager: PlaylistManager

    private var isReadPermissionGranted = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audio = intent?.getParcelableExtra("audio", AudioFile::class.java)
            //val audioIndex = intent?.getIntExtra("audioIndex", 0)
            if (audio != null) {
                if (view == SONG_VIEW) {
                    songView.musicAdapter.setSelectedAudioId(audio.id)
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

        changeView(SONG_VIEW)

        playlistManager = PlaylistManager(this) {
            checkPermission()
            if (isReadPermissionGranted) {
                val audioFiles = mutableListOf<AudioFile>()
                updateAudioFiles(audioFiles) { audioFile ->
                    val position = audioFiles.size
                    audioFiles.add(audioFile)
                    songView.onUpdateAddSong(audioFile, position)
                    playlistView.onUpdateAddSong(audioFile, position)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    val pm = PlaylistManager(this@MainActivity) {}
                    delay(200)
                    pm.syncAudioFiles(audioFiles)
                }
            }
        }
    }

    private fun changeView(view: Int) {
        this.view = view
        if (view == SONG_VIEW) {
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
            isReadPermissionGranted = true
            // Redémarre l'application
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
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
                isReadPermissionGranted = true
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

    private fun updateAudioFiles(audioFiles: MutableList<AudioFile>,onFileScanned: (AudioFile) -> Unit) {
        audioFiles.clear()
        getAllAudioFiles(onFileScanned)
        //songView.onUpdateAudioFiles()
        //playlistView.onUpdateAudioFiles()
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
                    val albumArtUri = Utils.getAlbumArtUri(albumId)
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

    override fun onDestroy() {
        super.onDestroy()
        songView.onDestroy()
        playlistView.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)

        if (MusicController.musicServiceIntent != null && !MusicService.isPlaying) {
            stopService(MusicController.musicServiceIntent)
        }
    }
}