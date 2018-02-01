package com.cziyeli.songbits.home.oldhome

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
 * Item view for the tracks.
 *
 * Created by connieli on 1/1/18.
 */
@Deprecated("for old home screen and infinite adapter")
@Layout(R.layout.playlists_item_square)
class PlaylistItemView(val context: Context, private var playlist: Playlist?) {

    @View(R.id.playlist_name)
    private lateinit var name: TextView

    @View(R.id.playlist_owner)
    private lateinit var ownerName: TextView

    @View(R.id.tracks_count)
    private lateinit var tracksCount: TextView

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
           name.text = it.name
           ownerName.text = it.owner.display_name
           tracksCount.text = "${it.totalTracksCount}"

           imageView.setImageURI(it.imageUrl)
       }
    }

    @Click(R.id.playlist_item_container)
    private fun onClick() {
        clickListener.onClick(itemView)
    }
}