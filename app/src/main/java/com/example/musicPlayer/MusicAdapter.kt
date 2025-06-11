package com.example.musicPlayer

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

sealed class Item (open val id: Long) {
    data class RealItem(
        override val id: Long,
        val title: String,
        val artist: String,
        val duration: Long,
        val albumArtUri: String,
        val data: String
    ) : Item(id)

    data class FakeItem(
        override val id: Long,
    ) : Item(id)
}

class MusicAdapter(private val activity: Activity, private val maxSelected: Int, private val items: MutableList<Item>, private val onItemClick: (Item.RealItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var selectedAudioIdList: ArrayList<Long> = arrayListOf()
    private var lastSelectedAudioId: Long? = null

    private var selectedColor: Int = Color.LTGRAY
    private var lastSelectedColor: Int = Color.LTGRAY

    private var isMenuButtonHide: Boolean = false

    companion object {
        private const val VIEW_TYPE_REAL = 1
        private const val VIEW_TYPE_FAKE = 2
    }

    class MusicViewHolder(private val activity: Activity, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.title)
        private val artist: TextView = itemView.findViewById(R.id.artist)
        private val duration: TextView = itemView.findViewById(R.id.duration)
        private val image: ImageView = itemView.findViewById(R.id.albumImage)

        fun bind(item: Item.RealItem, musicAdapter: MusicAdapter) {
            title.text = item.title
            artist.text = item.artist
            duration.text = Utils.formatDuration(item.duration)
            Utils.loadAlbumArt(activity, item.albumArtUri, image, R.drawable.music)

            // Changer l'apparence de l'élément sélectionné
            if (item.id == musicAdapter.lastSelectedAudioId) {
                itemView.setBackgroundColor(musicAdapter.lastSelectedColor)
            } else if (musicAdapter.selectedAudioIdList.any { it == item.id }) {
                itemView.setBackgroundColor(musicAdapter.selectedColor)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            if (musicAdapter.isMenuButtonHide) {
                val button: ImageButton = itemView.findViewById(R.id.menuButton)
                button.visibility = View.GONE
            }

            itemView.setOnClickListener {
                musicAdapter.onItemClick(item)
            }
        }
    }
    class FakeItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Item.FakeItem, musicAdapter: MusicAdapter) {}
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is Item.RealItem -> VIEW_TYPE_REAL
            is Item.FakeItem -> VIEW_TYPE_FAKE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_REAL -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
                MusicViewHolder(activity, view)
            }
            VIEW_TYPE_FAKE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.fake_item, parent, false)
                FakeItemViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is Item.RealItem -> (holder as MusicViewHolder).bind(item, this@MusicAdapter)
            is Item.FakeItem -> (holder as FakeItemViewHolder).bind(item, this@MusicAdapter)
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

    fun hideMenuButton() {
        isMenuButtonHide = true
    }

    fun getItems(): MutableList<Item> {
        return items
    }

    fun clearItems() {
        items.clear()
        items.add(Item.FakeItem(-1))
    }

    fun addItemLast(item: Item) {
        val pos = items.size-1
        items.add(pos, item)
        notifyItemInserted(pos)
    }
}