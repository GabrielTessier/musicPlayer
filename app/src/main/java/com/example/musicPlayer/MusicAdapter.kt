package com.example.musicPlayer

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList
import java.util.Locale

class MusicAdapter(private var audioFiles: ArrayList<AudioFile>, private val onItemClick: (AudioFile) -> Unit) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    private var selectedAudioId: Long? = null

    class MusicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val artist: TextView = itemView.findViewById(R.id.artist)
        val duration: TextView = itemView.findViewById(R.id.duration)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_music, parent, false)
        return MusicViewHolder(view)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        val audioFile = audioFiles[position]
        holder.title.text = audioFile.title
        holder.artist.text = audioFile.artist
        holder.duration.text = formatDuration(audioFile.duration)

        // Changer l'apparence de l'élément sélectionné
        if (audioFile.id == selectedAudioId) {
            holder.itemView.setBackgroundColor(Color.LTGRAY)
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            // Met à jour l'apparence de la précédente musique
            onItemClick(audioFile)
            selectedAudioId = audioFile.id
        }
    }

    override fun getItemCount(): Int = audioFiles.size

    private fun formatDuration(duration: Long): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format(Locale.FRANCE, "%02d:%02d", minutes, seconds)
    }

    fun setSelectedAudioId(audioId: Long, index: Int) {
        val indexOld = audioFiles.indexOfFirst { it.id == selectedAudioId }
        notifyItemChanged(indexOld)
        selectedAudioId = audioId
        // Met à jour l'apparence de la musique à jouer
        notifyItemChanged(index)
    }
}