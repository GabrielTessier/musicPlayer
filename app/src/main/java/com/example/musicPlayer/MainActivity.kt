package com.example.musicPlayer

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.ArrayList
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.cardview.widget.CardView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

class MainActivity : ComponentActivity(), ActivityInterface {
    private lateinit var recyclerView: RecyclerView
    lateinit var musicAdapter: MusicAdapter
    override var audioFiles : ArrayList<AudioFile> = ArrayList()
    private lateinit var musicControlCardView: CardView
    private lateinit var musicControlLinearLayout: LinearLayout

    private lateinit var musicController: MusicController
    //private lateinit var footerController: FooterController<MainActivity>

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audio = intent?.getParcelableExtra("audio", AudioFile::class.java)
            val audioIndex = intent?.getIntExtra("audioIndex", 0)
            if (audio != null) {
                musicAdapter.setSelectedAudioId(audio.id, audioIndex?:0)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("ACTION_FROM_SERVICE"))

        musicAdapter = MusicAdapter(audioFiles) { audioFile ->
            // Gérer la lecture de l'audio ici
            val index = audioFiles.indexOfFirst { it.id == audioFile.id }
            musicController.playAudio(audioFiles, index)
            // Mettre à jour l'ID de la musique actuellement jouée
            musicAdapter.setSelectedAudioId(audioFile.id, index)
            toggleCardViewVisibility(true)
        }


        setFooterOnClickListener()
        openMain()

        if (MusicService.isServiceRunning) {
            musicController = MusicController(this) {
                for (audio in musicController.musicService?.audioFiles?:ArrayList()) {
                    audioFiles.add(audio)
                }
                toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)
                if (musicController.musicService?.mediaPlayer?.isPlaying == true) {
                    val index = musicController.musicService?.currentAudioIndex?:0
                    musicAdapter.selectedAudioId = audioFiles[index].id
                }
            }
        } else {
            musicController = MusicController(this) {}
            toggleCardViewVisibility(false)
        }

        checkPermission()

        //footerController = FooterController(this)
    }

    private fun setFooterOnClickListener() {
        val mainImage: ImageView = findViewById(R.id.footerImageMain)
        val playlistImage: ImageView = findViewById(R.id.footerImagePlaylist)
        mainImage.setOnClickListener {
            setContentView(R.layout.activity_main)
            setFooterOnClickListener()
            openMain()
        }
        playlistImage.setOnClickListener {
            setContentView(R.layout.playlist_layout)
            setFooterOnClickListener()
        }
    }

    private fun openMain() {
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        musicControlCardView = findViewById(R.id.musicControlCardView)
        musicControlLinearLayout = findViewById(R.id.musicButtonLinearLayout)
        musicControlLinearLayout.setOnClickListener {
            val intent = Intent(this, MusicActivity::class.java)
            intent.putParcelableArrayListExtra("audioFiles", audioFiles)
            startActivity(intent)
        }

        recyclerView.adapter = musicAdapter
    }

    // Fonction pour afficher ou masquer le CardView
    private fun toggleCardViewVisibility(show: Boolean) {
        if (show) {
            musicControlCardView.visibility = View.VISIBLE  // Afficher le CardView
        } else {
            musicControlCardView.visibility = View.GONE  // Masquer le CardView
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
                musicAdapter.notifyItemInserted(position)
                recyclerView.post {
                    recyclerView.invalidateItemDecorations()
                }
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
                    musicAdapter.notifyItemInserted(position)
                    recyclerView.post {
                        recyclerView.invalidateItemDecorations()
                    }
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
        if (musicAdapter.selectedAudioId != null) {
            val index = audioFiles.indexOfFirst { it.id == musicAdapter.selectedAudioId }
            musicAdapter.setSelectedAudioId(musicAdapter.selectedAudioId?:0, index)
        }
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
