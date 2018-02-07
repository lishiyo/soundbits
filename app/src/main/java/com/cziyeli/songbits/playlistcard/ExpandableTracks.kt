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
import com.cziyeli.songbits.base.DisLikeButton
import com.cziyeli.songbits.base.LikeButton

/**
 * The tracks in the expandable
 */
// View model wrapper around domain model - represents viewmodel for a track row
data class TrackRow(val model: TrackModel) {
    var isOpen: Boolean = false

    var liked: Boolean = model.liked
        get() = model.liked

    val name: String
        get() = model.name

    val artistName: String?
        get() = model.artistName

    val imageUrl: String?
        get() = model.imageUrl
}

// Tracks adapter
class TrackRowsAdapter(context: Context, var trackRows: MutableList<TrackModel>) : RecyclerView.Adapter<TrackRowsAdapter.MainViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)

    fun updateTrack(track: TrackModel?, notify: Boolean = true) {
        track?.apply {
            val index = trackRows.indexOfFirst { it.id == this.id }
            if (index != -1) { // refresh
                trackRows[index] = track
                if (notify) {
                    notifyItemChanged(index)
                }
            }
        }
    }

    fun setTracksAndNotify(tracks: List<TrackModel>, notify: Boolean = true) {
        trackRows.clear()
        trackRows.addAll(tracks)

        if (notify) {
            notifyDataSetChanged()
        }
    }

    fun addTracksAndNotify(tracks: List<TrackModel>) {
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

        private var likeButton: LikeButton = itemView.findViewById(R.id.like_icon_container)
        private var dislikeButton: DisLikeButton = itemView.findViewById(R.id.dislike_icon_container)

        fun bindData(rowModel: TrackModel) {
            rowModel.imageUrl?.let {
                Glide.with(itemView.context)
                        .load(it)
                        .into(imageView)
            }
            titleText.text = rowModel.name
            artistText.text = rowModel.artistName

            likeButton.setActive(rowModel.liked)
            dislikeButton.setActive(!rowModel.liked)
        }
    }
}