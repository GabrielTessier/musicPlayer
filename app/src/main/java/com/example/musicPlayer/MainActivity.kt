package com.example.musicPlayer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.util.ArrayList
import android.media.MediaMetadataRetriever
import android.widget.LinearLayout
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    lateinit var musicAdapter: MusicAdapter
    private var uri : Uri? = null
    private var audioFiles : ArrayList<AudioFile> = ArrayList()
    private lateinit var musicButtonLinearLayout: LinearLayout

    private lateinit var musicController: MusicController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter("ACTION_FROM_SERVICE"))

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        musicButtonLinearLayout = findViewById(R.id.musicButtonLinearLayout)
        musicButtonLinearLayout.setOnClickListener {
            val intent = Intent(this, MusicActivity::class.java)
            intent.putParcelableArrayListExtra("audioFiles", audioFiles)
            startActivity(intent)
        }

        musicAdapter = MusicAdapter(audioFiles) { audioFile ->
            // Gérer la lecture de l'audio ici
            val index = audioFiles.indexOfFirst { it.id == audioFile.id }
            musicController.playAudio(audioFiles, index)
            // Mettre à jour l'ID de la musique actuellement jouée
            musicAdapter.setSelectedAudioId(audioFile.id, index)
        }
        recyclerView.adapter = musicAdapter

        if (MusicService.isServiceRunning) {
            musicController = MusicController(this) {
                for (audio in musicController.musicService?.audioFiles?:ArrayList()) {
                    audioFiles.add(audio)
                }
            }
        } else {
            musicController = MusicController(this) {}
            setURI { uri ->
                if (uri != null) {
                    this.uri = uri
                    // ⚡ Lancer l’analyse en arrière-plan
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            scanAudioFiles(this@MainActivity, uri) { audioFile ->
                                withContext(Dispatchers.Main) {
                                    val position = audioFiles.size
                                    audioFiles.add(audioFile)
                                    musicAdapter.notifyItemInserted(position)

                                    recyclerView.post {
                                        recyclerView.invalidateItemDecorations()
                                    }
                                }
                            }
                        }
                    }
                } else {
                    finishAffinity() // Fermer l'application s'il n'y a pas d'uri
                }
            }
        }
    }

    private fun setURI(onUriReady: (Uri?) -> Unit) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        )
        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }
                onUriReady(uri)
            } else {
                onUriReady(null)
            }
        }
        resultLauncher.launch(intent)
    }

    private suspend fun scanAudioFiles(context: Context, uri: Uri, onFileScanned: suspend (AudioFile) -> Unit) {
        val directory = DocumentFile.fromTreeUri(context, uri)
        if (directory != null && directory.isDirectory) {
            var idValue = 0L
            for (file in directory.listFiles()) {
                if (file.isFile && file.type?.startsWith("audio") == true) {
                    val retriever = MediaMetadataRetriever()
                    try {
                        context.contentResolver.openFileDescriptor(file.uri, "r")?.use { fd ->
                            retriever.setDataSource(fd.fileDescriptor)

                            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name ?: "Inconnu"
                            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Inconnu"
                            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            val duration = durationStr?.toLongOrNull() ?: 0L
                            val imageBytes = retriever.embeddedPicture

                            val audioFile = AudioFile(
                                id = idValue,
                                title = title,
                                artist = artist,
                                duration = duration,
                                imageBytes = imageBytes,
                                data = file.uri.toString()
                            )
                            onFileScanned(audioFile)
                            idValue++
                        }
                    } catch (e: Exception) {
                        Log.e("setAudioFiles", "Erreur de lecture : ${file.name}", e)
                    } finally {
                        retriever.release()
                    }
                }
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val audio = intent?.getParcelableExtra("audio", AudioFile::class.java)
            val audioIndex = intent?.getIntExtra("audioIndex", 0)
            if (audio != null) {
                musicAdapter.setSelectedAudioId(audio.id, audioIndex?:0)
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        musicController.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        if (musicController.musicServiceIntent != null && musicController.musicService?.mediaPlayer?.isPlaying == false) stopService(musicController.musicServiceIntent)
    }
}
