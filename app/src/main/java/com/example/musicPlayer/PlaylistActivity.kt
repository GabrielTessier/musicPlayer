package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class PlaylistActivity : ComponentActivity() {

    private lateinit var playlistManager: PlaylistManager
    private lateinit var playlist: Playlist

    private lateinit var recyclerView: RecyclerView
    private lateinit var musicAdapter: MusicAdapter
    private lateinit var items: MutableList<Item>

    private lateinit var musicController: MusicController
    private lateinit var musicControlCardView: CardView
    private lateinit var musicControlLinearLayout: LinearLayout

    private var isDelete = false

    private val resultLauncherSelectAddMusic = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                val validate: Boolean = data.getBooleanExtra("validate", false)
                if (validate) {
                    val audioList: ArrayList<AudioFile> = data.getParcelableArrayListExtra("audioList", AudioFile::class.java)?: arrayListOf()
                    for (audio in audioList) {
                        playlistManager.addAudioToPlaylist(playlist.id, audio)
                        addItem(audio)
                    }
                    playlist = PlaylistManager.getPlaylistById(playlistId = playlist.id)!!
                    updateNbElem()
                }
            }
        }
    }

    private fun openSelectMusicToAddActivity() {
        val intent = Intent(this, SelectMusicActivity::class.java)
        resultLauncherSelectAddMusic.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.playlist_manage_layout)

        playlistManager = PlaylistManager(this) {}

        val playlistExtra = intent.getParcelableExtra("playlist", Playlist::class.java)
        if (playlistExtra == null) {
            finish()
        }
        playlist = playlistExtra!!

        musicController = MusicController(this) { onMusicServiceConnect() }
        musicController.reloadVar()
        musicControlCardView = findViewById(R.id.musicControlCardView)
        musicControlLinearLayout = findViewById(R.id.musicButtonLinearLayout)
        musicControlLinearLayout.setOnClickListener {
            val intent = Intent(this, MusicActivity::class.java)
            startActivity(intent)
        }
        musicController.update()
        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)


        recyclerView = findViewById(R.id.musics)
        recyclerView.layoutManager = LinearLayoutManager(this)
        updateItemList(playlist.audios)
        musicAdapter = MusicAdapter(this, 0, items) { audioFile ->
            // Gérer la lecture de l'audio ici
            if (musicAdapter.getLastSelectedAudioId() != audioFile.id) {
                //val audioFiles = PlaylistManager.getAudioFiles()
                val audioFiles = playlist.audios
                val index = audioFiles.indexOfFirst { it.id == audioFile.id }
                musicController.playAudio(audioFiles, index)
                // Mettre à jour l'ID de la musique actuellement jouée
                musicAdapter.setSelectedAudioId(audioFile.id)
                toggleCardViewVisibility(true)
            }
        }
        recyclerView.adapter = musicAdapter

        val btnRetour = findViewById<Button>(R.id.btnRetour)
        btnRetour.setOnClickListener {
            finish()  // Termine MusicActivity et retourne à MainActivity
        }

        val textTitre = findViewById<TextView>(R.id.title)
        textTitre.text = playlist.name
        updateNbElem()

        val btnDelete = findViewById<Button>(R.id.btnDelete)
        btnDelete.setOnClickListener {
            isDelete = true
            finish()
        }

        val btnAddMusic = findViewById<Button>(R.id.btnAddMusic)
        btnAddMusic.setOnClickListener {
            openSelectMusicToAddActivity()
        }
    }

    // Fonction pour afficher ou masquer le CardView
    private fun toggleCardViewVisibility(show: Boolean) {
        if (show) {
            musicControlCardView.visibility = View.VISIBLE  // Afficher le CardView
        } else {
            musicControlCardView.visibility = View.GONE  // Masquer le CardView
        }
    }

    private fun onMusicServiceConnect() {
        musicController.update()
        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)
    }

    private fun addItem(audio: AudioFile) {
        val pos = items.size-1
        items.add(pos, Utils.audioFileToItem(audio))
        musicAdapter.notifyItemInserted(pos)
    }
    private fun updateItemList(audioFiles: List<AudioFile>) {
        items = MutableList(audioFiles.size+1) { index: Int ->
            if (index != audioFiles.size) {
                val audio = audioFiles[index]
                Utils.audioFileToItem(audio)
            } else {
                Item.FakeItem(-1)
            }
        }
    }

    private fun updateNbElem() {
        val textNbElem = findViewById<TextView>(R.id.nbElem)
        textNbElem.text = String.format(Locale.FRANCE, "Nombre d'élément : %d", playlist.audios.size)
    }

    override fun finish() {
        setResult(Activity.RESULT_OK, Intent().apply {
            putExtra("delete", isDelete)
            putExtra("id", playlist.id)
        })
        super.finish()
    }

    /*override fun onDestroy() {
        super.onDestroy()
    }*/
}