package com.cziyeli.songbits.home

import android.content.Context
import android.widget.TextView
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.songbits.R
import com.facebook.drawee.view.SimpleDraweeView
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View


/**
 * Item view for the playlists
 *
 * Created by connieli on 1/1/18.
 */
@Layout(R.layout.playlists_item_square)
class PlaylistItem(val context: Context, var playlist: Playlist?) {

    @View(R.id.playlist_name)
    private lateinit var name: TextView

    @View(R.id.playlist_owner)
    private lateinit var ownerName: TextView

    @View(R.id.playlist_image)
    private lateinit var imageView: SimpleDraweeView

    @Resolve
    private fun onResolved() {
       playlist?.let {
           name.setText(it.name)
           ownerName.setText(it.owner?.display_name)

           val image = it.images?.get(0)
           image?.let {
               imageView.setImageURI(it.url)
           }
       }
    }

}