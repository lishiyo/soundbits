package com.cziyeli.songbits.home

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import io.github.luizgrp.sectionedrecyclerviewadapter.Section
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters


class PlaylistSection(val title: String,
                      var itemList: MutableList<Playlist> = mutableListOf(),
                      private val listener: ClickListener? = null
) : Section(
        SectionParameters.Builder(R.layout.grid_item_playlist)
                .headerResourceId(R.layout.grid_item_section_header)
                .footerResourceId(R.layout.grid_item_section_footer)
//                .failedResourceId(R.layout.section_ex3_failed)
                .loadingResourceId(R.layout.grid_item_section_loading)
                .build()
) {

    interface ClickListener {
        // clicked a playlist card
        fun onItemClick(view: View, item: Playlist)

        // clicked footer in section
        fun onFooterClick(section: PlaylistSection)
    }

    fun addPlaylists(list: MutableList<Playlist>) {
        itemList.addAll(list)
    }

    override fun getContentItemsTotal(): Int {
        return itemList.size // number of items of this section
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        // return a custom instance of TrackViewHolder for the items of this section
        return PlaylistItemViewHolder(view)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val itemHolder = holder as PlaylistItemViewHolder
        val playlistItem = itemList[position]
        holder.title.text = playlistItem.name

        Glide.with(holder.itemView.context)
                .load(playlistItem.imageUrl)
                .apply(RequestOptions
                        .noTransformation()
                        .dontTransform()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                )
                .into(itemHolder.imageView)

        itemHolder.bind(playlistItem, listener)
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return HeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        val headerHolder = holder as HeaderViewHolder
        headerHolder.sectionTitle.text = title
        holder.itemView.isClickable = true
    }

    override fun getFooterViewHolder(view: View): RecyclerView.ViewHolder {
        return FooterViewHolder(view)
    }

    override fun onBindFooterViewHolder(holder: RecyclerView.ViewHolder?) {
        val footerHolder = holder as FooterViewHolder?

        footerHolder?.rootView?.setOnClickListener({
            listener?.onFooterClick(this)
        })
    }
}

internal class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var sectionTitle: TextView = view.findViewById(R.id.section_title)
}

private class FooterViewHolder internal constructor(val rootView: View) : RecyclerView.ViewHolder(rootView)

internal class PlaylistItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    var imageView: ImageView = itemView.findViewById(R.id.playlist_image)
    var title: TextView = itemView.findViewById(R.id.title)

    fun bind(item: Playlist, listener: PlaylistSection.ClickListener? = null) {
        itemView.setOnClickListener{ listener?.onItemClick(itemView, item) }
    }
}