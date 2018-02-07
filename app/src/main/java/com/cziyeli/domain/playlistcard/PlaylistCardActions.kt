package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.domain.tracks.TrackModel

/**
 * Marker interface for any action that can be taken on a card.
 */
interface CardActionMarker : MviAction

/**
 * Actions shared by any card.
 */
sealed class CardAction : CardActionMarker {
    // fetch basic counts - liked, disliked, total
    class CalculateQuickCounts(val tracks: List<TrackModel>) : CardAction()

    // action to set a header
    class HeaderSet(val headerImageUrl: String) : CardAction()
}

/**
 * Actions for the playlist card widget.
 */
sealed class PlaylistCardAction(val playlistId: String? = "") : CardActionMarker {
    object None : PlaylistCardAction()

    // get list of (swiped) tracks for a playlist
    class FetchPlaylistTracks(val ownerId: String,
                              playlistId: String,
                              val onlySwiped: Boolean = true,
                              val fields: String? = null,
                              val limit: Int = 100,
                              val offset: Int = 0
    ) : PlaylistCardAction(playlistId)

}
