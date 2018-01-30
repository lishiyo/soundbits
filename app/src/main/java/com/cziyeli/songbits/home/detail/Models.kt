package com.cziyeli.songbits.home.detail

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.cziyeli.songbits.R
import java.util.*


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

// Tracks adapter
class TrackRowsAdapter(context: Context, list: List<RowTrackItem>) : RecyclerView.Adapter<TrackRowsAdapter.MainViewHolder>() {
    private var inflater: LayoutInflater = LayoutInflater.from(context)
    private var modelList: List<RowTrackItem> = ArrayList<RowTrackItem>(list)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val view = inflater.inflate(R.layout.playlistcard_list_item_track, parent, false)
        return MainViewHolder(view)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        holder.bindData(modelList[position])
    }

    override fun getItemCount(): Int {
        return modelList.size
    }

    inner class MainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private var mainText: TextView = itemView.findViewById(R.id.track_title) as TextView
        private var subText: TextView = itemView.findViewById(R.id.track_artist) as TextView

        fun bindData(rowModel: RowTrackItem) {
            mainText.text = rowModel.name
            subText.text = rowModel.artist
        }
    }
}