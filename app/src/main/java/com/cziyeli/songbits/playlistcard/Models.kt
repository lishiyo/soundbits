package com.cziyeli.songbits.playlistcard

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cziyeli.domain.SimpleImage
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.R

// Track list
data class RowTrackItem(val name: String, val artist: String = "")

fun getDummyTracks() : List<RowTrackItem> {
    return listOf(
            RowTrackItem("devilman crybaby", "first artist"),
            RowTrackItem("abandoned kitten", "just sitting there"),
            RowTrackItem("broadchurch", "auntie ellie"),
            RowTrackItem("fullmetal alchemist", "elric"),
            RowTrackItem("le pain quot", "russell tovey"),
            RowTrackItem("modest writing success", "the dogooder")
    )
}

// Wrapper around domain model - represents viewmodel for a track row
data class TrackRow(val model: TrackModel) {
    val name: String
        get() = model.name

    val primaryArtistName: String
        get() = if (model.artist == null) "" else model.artist!!.name

    val image: SimpleImage?
        get() = model.album?.images?.get(0)

    val imageUrl: String?
        get() = image?.url
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

        fun bindData(rowModel: TrackRow) {
            titleText.text = rowModel.name
            artistText.text = rowModel.primaryArtistName
        }
    }
}