package com.cziyeli.songbits.playlistcard

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R

/**
 * The tracks in the expandable
 */
// Wrapper around domain model - represents viewmodel for a track row
data class TrackRow(val model: TrackModel) {
    val name: String
        get() = model.name

    val primaryArtistName: String?
        get() = model.artistName

    val imageUrl: String?
        get() = model.imageUrl
}

// Tracks adapter
class TrackRowsAdapter(context: Context, var trackRows: MutableList<TrackRow>) : RecyclerView.Adapter<TrackRowsAdapter.MainViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)

    fun setTracksAndNotify(tracks: List<TrackRow>) {
        trackRows.clear()
        trackRows.addAll(tracks)
        notifyDataSetChanged()
    }

    fun addTracksAndNotify(tracks: List<TrackRow>) {
        trackRows.addAll(tracks)
        notifyItemRangeInserted(trackRows.size - tracks.size, tracks.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = inflater.inflate(R.layout.playlistcard_list_item_track, parent, false)
        return MainViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bindData(trackRows[position])
    }

    override fun getItemCount(): Int {
        return trackRows.size
    }

    inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var titleText: TextView = itemView.findViewById(R.id.track_title) as TextView
        private var artistText: TextView = itemView.findViewById(R.id.track_artist) as TextView
        private var imageView: ImageView = itemView.findViewById(R.id.track_image) as ImageView

        fun bindData(rowModel: TrackRow) {
            rowModel.imageUrl?.let {
                Glide.with(itemView.context)
                        .load(it)
                        .into(imageView)
            }
            titleText.text = rowModel.name
            artistText.text = rowModel.primaryArtistName
        }
    }
}