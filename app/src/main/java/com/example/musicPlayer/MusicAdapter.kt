package com.example.musicPlayer

import android.app.Activity
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.util.Locale

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

class MusicAdapter(private val activity: Activity, private val items: MutableList<Item>, private val onItemClick: (Item.RealItem) -> Unit) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var selectedAudioId: Long? = null

    companion object {
        private const val VIEW_TYPE_REAL = 1
        private const val VIEW_TYPE_FAKE = 2

        fun formatDuration(duration: Long): String {
            val minutes = (duration / 1000) / 60
            val seconds = (duration / 1000) % 60
            return String.format(Locale.FRANCE, "%02d:%02d", minutes, seconds)
        }
    }

    class MusicViewHolder(private val activity: Activity, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.title)
        private val artist: TextView = itemView.findViewById(R.id.artist)
        private val duration: TextView = itemView.findViewById(R.id.duration)
        private val image: ImageView = itemView.findViewById(R.id.albumImage)

        fun bind(item: Item.RealItem, musicAdapter: MusicAdapter) {
            title.text = item.title
            artist.text = item.artist
            duration.text = formatDuration(item.duration)
            Utils.loadAlbumArt(activity, item.albumArtUri, image, R.drawable.music)

            // Changer l'apparence de l'élément sélectionné
            if (item.id == musicAdapter.selectedAudioId) {
                itemView.setBackgroundColor(Color.LTGRAY)
            } else {
                itemView.setBackgroundColor(Color.TRANSPARENT)
            }

            itemView.setOnClickListener {
                // Met à jour l'apparence de la précédente musique
                musicAdapter.onItemClick(item)
                musicAdapter.selectedAudioId = item.id
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

    fun setSelectedAudioId(audioId: Long, index: Int) {
        val indexOld = items.indexOfFirst { it.id == selectedAudioId }
        selectedAudioId = audioId
        notifyItemChanged(indexOld)
        notifyItemChanged(index)
    }
}