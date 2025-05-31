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

lateinit var mainActivity: MainActivity

class MainActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    lateinit var musicAdapter: MusicAdapter
    private var uri : Uri? = null
    private var audioFiles : ArrayList<AudioFile> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainActivity = this

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val context = this

        setURI { uri ->
            if (uri != null) {
                this.uri = uri

                setAudioFiles(context, uri)
                musicAdapter = MusicAdapter(audioFiles) { audioFile ->
                    // Gérer la lecture de l'audio ici
                    val index = audioFiles.indexOfFirst { it.id == audioFile.id }
                    playAudio(audioFiles, index)
                    // Mettre à jour l'ID de la musique actuellement jouée
                    musicAdapter.setSelectedAudioId(audioFile.id, index)
                }
                recyclerView.adapter = musicAdapter
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

    /*private fun setAudioFiles(context: Context) {
        audioFiles = ArrayList<AudioFile>()
        //val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        //val uri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI
        Log.d("MyDebug", "GetAudioFiles uri : ${uri.toString()}")
        uri?.let { uri ->
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )
            Log.d("MyDebug", "projection")
            val cursor = context.contentResolver.query(uri, projection, null, null, null)
            Log.d("MyDebug", "cursor")
            cursor?.use {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val title = it.getString(titleColumn)
                    val artist = it.getString(artistColumn)
                    val duration = it.getLong(durationColumn)
                    val data = it.getString(dataColumn)
                    Log.d("MyDebug", "add music")
                    audioFiles.add(AudioFile(id, title, artist, duration, data))
                }
            }
        }
    }*/

    private fun setAudioFiles(context: Context, uri: Uri) {
        audioFiles = ArrayList<AudioFile>()
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

                            audioFiles.add(
                                AudioFile(
                                    id = idValue,
                                    title = title,
                                    artist = artist,
                                    duration = duration,
                                    data = file.uri.toString()
                                )
                            )
                            idValue++
                        }
                    } catch (e: Exception) {
                        Log.e("setAudioFiles", "Erreur de lecture : ${file.name}", e)
                    } finally {
                        retriever.release()
                    }
                }
            }
        } else {
            Log.w("setAudioFiles", "L'URI n'est pas un dossier valide")
        }
    }

    private fun playAudio(audioFiles: List<AudioFile>, initialIndex: Int) {
        val intent = Intent(this, MusicService::class.java).apply {
            putParcelableArrayListExtra("audioFiles", ArrayList(audioFiles))
            putExtra("initialIndex", initialIndex)
        }
        startService(intent)
    }
}
