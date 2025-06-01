package com.example.musicPlayer

import android.app.Activity
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
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

lateinit var mainActivity: MainActivity

class MainActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    lateinit var musicAdapter: MusicAdapter
    private var uri : Uri? = null
    var audioFiles : ArrayList<AudioFile> = ArrayList()

    private var musicServiceIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivity = this

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        musicAdapter = MusicAdapter { audioFile ->
            // Gérer la lecture de l'audio ici
            val index = audioFiles.indexOfFirst { it.id == audioFile.id }
            playAudio(audioFiles, index)
            // Mettre à jour l'ID de la musique actuellement jouée
            musicAdapter.setSelectedAudioId(audioFile.id, index)
        }
        recyclerView.adapter = musicAdapter

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

                            val audioFile = AudioFile(
                                id = idValue,
                                title = title,
                                artist = artist,
                                duration = duration,
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

    private fun playAudio(audioFiles: List<AudioFile>, initialIndex: Int) {
        if (musicServiceIntent != null) stopService(musicServiceIntent)
        musicServiceIntent = Intent(this, MusicService::class.java).apply {
            putParcelableArrayListExtra("audioFiles", ArrayList(audioFiles))
            putExtra("initialIndex", initialIndex)
        }
        startService(musicServiceIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (musicServiceIntent != null) stopService(musicServiceIntent)
    }
}
