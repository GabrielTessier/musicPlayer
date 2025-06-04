package com.example.musicPlayer

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PlaylistView(private val main: MainActivity): com.example.musicPlayer.View {

    private lateinit var recyclerView: RecyclerView
    private var scrollY: Int = 0

    private lateinit var playlistManager: PlaylistManager
    private lateinit var playlistAdapter: PlaylistAdapter

    private lateinit var items: MutableList<PlaylistItem>

    private lateinit var buttonPlus: Button

    private val resultLauncher = main.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            if (data != null) {
                val id: Long = data.getLongExtra("id", 0)
                val index = playlistManager.getPlaylists().indexOfFirst { it.id == id }
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
        playlistManager = PlaylistManager(main) { playlists ->
            updateItemList(playlists)
            playlistAdapter = PlaylistAdapter(items) { playlistItem ->
                val index = playlists.indexOfFirst { it.id == playlistItem.id }
                val playlist = playlists[index]
                // Mettre à jour l'ID de la playlist
                playlistAdapter.setSelectedAudioId(playlist.id, index)

                openPlaylistActivity(playlist)
            }
        }
    }

    private fun updateItem(index: Int) {
        val playlist = playlistManager.getPlaylistById(items[index].id)!!
        items[index] = PlaylistItem.RealItem(
            id = playlist.id,
            name = playlist.name,
            audios = playlist.audios
        )
        playlistAdapter.notifyItemChanged(index)
    }

    fun updateItemList(playlists: List<Playlist>) {
        items = MutableList(playlists.size+1) { index: Int ->
            if (index != playlists.size) {
                val playlist = playlists[index]
                PlaylistItem.RealItem(
                    id = playlist.id,
                    name = playlist.name,
                    audios = playlist.audios
                )
            } else {
                PlaylistItem.FakeItem(-1)
            }
        }
    }

    fun addItemList(playlist: Playlist) {
        items.add(items.size-1,
            PlaylistItem.RealItem(
                id = playlist.id,
                name = playlist.name,
                audios = playlist.audios
            )
        )
    }

    override fun open() {
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

        buttonPlus = main.findViewById(R.id.button_plus)
        buttonPlus.setOnClickListener {
            val playlist = playlistManager.createPlaylist("Test ${items.size}")
            addItemList(playlist)
            val pos = playlistManager.getPlaylists().size-1
            playlistAdapter.notifyItemChanged(pos)
            playlistAdapter.notifyItemInserted(pos+1)
        }
    }

    override fun onUpdateAddSong(position: Int) {
        //TODO("Not yet implemented")
    }

    override fun onUpdateAudioFiles() {
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

class PlaylistAdapter(private val items: MutableList<PlaylistItem>, private val onItemClick: (PlaylistItem.RealItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectedPlaylistId: Long? = null

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
            if (item.id == playlistAdapter.selectedPlaylistId) {
                itemView.setBackgroundColor(Color.LTGRAY)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                playlistAdapter.onItemClick(item)
                playlistAdapter.selectedPlaylistId = item.id
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

    fun setSelectedAudioId(audioId: Long, index: Int) {
        val indexOld = items.indexOfFirst { it.id == selectedPlaylistId }
        selectedPlaylistId = audioId
        notifyItemChanged(indexOld)
        notifyItemChanged(index)
    }
}