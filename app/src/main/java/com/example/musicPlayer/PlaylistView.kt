package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistView(private val main: MainActivity): com.example.musicPlayer.View {

    private lateinit var recyclerView: RecyclerView
    private var scrollY: Int = 0

    private lateinit var playlistManager: PlaylistManager
    private lateinit var playlistAdapter: PlaylistAdapter

    private lateinit var items: MutableList<PlaylistItem>

    private lateinit var musicController: MusicController
    private lateinit var musicControlCardView: CardView
    private lateinit var musicControlLinearLayout: LinearLayout

    private lateinit var buttonPlus: Button

    private val resultLauncher = main.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                val id: Long = data.getLongExtra("id", 0)
                val index = PlaylistManager.getPlaylists().indexOfFirst { it.id == id }
                val delete: Boolean = data.getBooleanExtra("delete", false)
                if (delete) {
                    playlistManager.deletePlaylistById(id)
                    items.removeAt(index)
                    playlistAdapter.notifyItemRemoved(index)
                } else {
                    updateItem(index)
                }
            }
        }
    }

    private fun openPlaylistActivity(playlist: Playlist) {
        val intent = Intent(main, PlaylistActivity::class.java).apply {
            putExtra("playlist", playlist)
        }
        resultLauncher.launch(intent)
    }

    init {
        playlistManager = PlaylistManager(main) {
            val playlists = PlaylistManager.getPlaylists()
            updateItemList(playlists)
            playlistAdapter = PlaylistAdapter(1, items) { playlistItem ->
                val index = playlists.indexOfFirst { it.id == playlistItem.id }
                val playlist = playlists[index]
                // Mettre à jour l'ID de la playlist
                playlistAdapter.setSelectedAudioId(playlist.id)

                openPlaylistActivity(playlist)
            }
        }
        //musicController = MusicController(main) { onMusicServiceConnect() }
        musicController = MusicController(main) {}
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
        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)
    }

    private fun updateItem(index: Int) {
        val playlist = PlaylistManager.getPlaylistById(items[index].id)!!
        items[index] = Utils.playlistToItem(playlist)
        playlistAdapter.notifyItemChanged(index)
    }

    private fun updateItemList(playlists: List<Playlist>) {
        items = MutableList(playlists.size+1) { index: Int ->
            if (index != playlists.size) {
                val playlist = playlists[index]
                Utils.playlistToItem(playlist)
            } else {
                PlaylistItem.FakeItem(-1)
            }
        }
    }

    private fun addItemList(playlist: Playlist) {
        items.add(items.size-1, Utils.playlistToItem(playlist))
    }

    override fun open() {
        musicController.reloadVar()

        recyclerView = main.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(main)
        val layoutManager = recyclerView.layoutManager as LinearLayoutManager
        layoutManager.scrollToPosition(scrollY)

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                scrollY = firstVisibleItemPosition
            }
        })

        recyclerView.adapter = playlistAdapter

        musicControlCardView = main.findViewById(R.id.musicControlCardView)
        musicControlLinearLayout = main.findViewById(R.id.musicButtonLinearLayout)
        musicControlLinearLayout.setOnClickListener {
            val intent = Intent(main, MusicActivity::class.java)
            main.startActivity(intent)
        }

        musicController.update()
        toggleCardViewVisibility(musicController.musicService?.mediaPlayer?.isPlaying?:false)

        buttonPlus = main.findViewById(R.id.button_plus)
        buttonPlus.setOnClickListener {
            val playlist = playlistManager.createPlaylist("Test ${items.size}")
            addItemList(playlist)
            val pos = PlaylistManager.getPlaylists().size-1
            playlistAdapter.notifyItemChanged(pos)
            playlistAdapter.notifyItemInserted(pos+1)
        }
    }

    override fun onUpdateAddSong(audio: AudioFile, position: Int) {
        //TODO("Not yet implemented")
    }
}



sealed class PlaylistItem (open val id: Long) {
    data class RealItem(
        override val id: Long,
        val name: String,
        val audios: MutableList<AudioFile>
    ) : PlaylistItem(id)

    data class FakeItem(
        override val id: Long,
    ) : PlaylistItem(id)
}

class PlaylistAdapter(private val maxSelected: Int, private val items: MutableList<PlaylistItem>, private val onItemClick: (PlaylistItem.RealItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedAudioIdList: ArrayList<Long> = arrayListOf()
    private var lastSelectedAudioId: Long? = null

    private var selectedColor: Int = Color.LTGRAY
    private var lastSelectedColor: Int = Color.LTGRAY

    companion object {
        private const val VIEW_TYPE_REAL = 1
        private const val VIEW_TYPE_FAKE = 2
    }

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.title)
        private val size: TextView = itemView.findViewById(R.id.size)

        fun bind(item: PlaylistItem.RealItem, playlistAdapter: PlaylistAdapter) {
            name.text = item.name
            size.text = item.audios.size.toString()

            // Changer l'apparence de l'élément sélectionné
            if (item.id == playlistAdapter.lastSelectedAudioId) {
                itemView.setBackgroundColor(playlistAdapter.lastSelectedColor)
            } else if (playlistAdapter.selectedAudioIdList.any { it == item.id }) {
                itemView.setBackgroundColor(playlistAdapter.selectedColor)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                playlistAdapter.onItemClick(item)
            }
        }
    }
    class FakePlaylistItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: PlaylistItem.FakeItem, playlistAdapter: PlaylistAdapter) {}
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is PlaylistItem.RealItem -> VIEW_TYPE_REAL
            is PlaylistItem.FakeItem -> VIEW_TYPE_FAKE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_REAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false)
                PlaylistViewHolder(view)
            }
            VIEW_TYPE_FAKE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.fake_item, parent, false)
                FakePlaylistItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is PlaylistItem.RealItem -> (holder as PlaylistViewHolder).bind(item, this@PlaylistAdapter)
            is PlaylistItem.FakeItem -> (holder as FakePlaylistItemViewHolder).bind(item, this@PlaylistAdapter)
        }
    }

    override fun getItemCount(): Int = items.size

    fun getLastSelectedAudioId(): Long? {
        return lastSelectedAudioId
    }

    fun setSelectedAudioId(audioId: Long?) {
        val previousIndex = items.indexOfFirst { it.id == lastSelectedAudioId } // indice à passer de lastSelected à selected
        lastSelectedAudioId = audioId
        notifyItemChanged(previousIndex)
        if (audioId != null) {
            if (maxSelected > 0 && !selectedAudioIdList.any { it == audioId }) {
                selectedAudioIdList.add(audioId)
                val index = items.indexOfFirst { it.id == audioId } // indice à sélectionner
                notifyItemChanged(index)
                if (selectedAudioIdList.size == maxSelected + 1) {
                    val indexSup = items.indexOfFirst { it.id == selectedAudioIdList[0] }  // indice à désélectionner
                    selectedAudioIdList.removeAt(0)
                    notifyItemChanged(indexSup)
                }
            }
        }
    }

    fun unsetSelectedAudioId(audioId: Long) {
        val indexLast = items.indexOfFirst { it.id == lastSelectedAudioId }
        val indexInBuffer = selectedAudioIdList.indexOfFirst { it == audioId }
        lastSelectedAudioId = null
        selectedAudioIdList.removeAt(indexInBuffer)
        val indexInItemList = items.indexOfFirst { it.id == audioId }
        notifyItemChanged(indexInItemList)
        if (indexLast != indexInItemList) notifyItemChanged(indexLast)
    }

    fun setSelectedColor(color: Int) {
        selectedColor = color
    }
    fun setLastSelectedColor(color: Int) {
        lastSelectedColor = color
    }

    fun getItems(): MutableList<PlaylistItem> {
        return items
    }

    fun clearItems() {
        items.clear()
        items.add(PlaylistItem.FakeItem(-1))
    }

    fun addItemLast(item: PlaylistItem) {
        val pos = items.size-1
        items.add(pos, item)
        notifyItemInserted(pos)
    }
}