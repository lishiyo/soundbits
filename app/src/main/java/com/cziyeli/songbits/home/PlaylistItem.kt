package com.cziyeli.songbits.home

import android.content.Context
import android.view.ViewGroup
import android.widget.TextView
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import com.cziyeli.songbits.cards.CardsActivity
import com.facebook.drawee.view.SimpleDraweeView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


/**
 * Item view for the items
 *
 * Created by connieli on 1/1/18.
 */
@Layout(R.layout.playlists_item_square)
class PlaylistItem(val context: Context, private var playlist: Playlist?) {

    @View(R.id.playlist_name)
    private lateinit var name: TextView

    @View(R.id.playlist_owner)
    private lateinit var ownerName: TextView

    @View(R.id.playlist_image)
    private lateinit var imageView: SimpleDraweeView

    @View(R.id.playlist_item_container)
    private lateinit var itemView: ViewGroup

    private val clickListener: android.view.View.OnClickListener = android.view.View.OnClickListener {
        playlist?.let {
            context.startActivity(CardsActivity.create(context, it))
        }
    }

    @Resolve
    private fun onResolved() {
       playlist?.let {
           name.setText(it.name)
           ownerName.setText(it.owner?.display_name)

           imageView.setImageURI(it.coverImage?.url)

           itemView.setOnClickListener(clickListener)
       }
    }

    @Click(R.id.playlist_item_container)
    private fun onClick() {
        clickListener.onClick(itemView)
    }
}