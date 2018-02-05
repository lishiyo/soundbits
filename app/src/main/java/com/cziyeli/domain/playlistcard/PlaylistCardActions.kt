package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.domain.tracks.TrackModel

/**
 * Marker interface for the playlist card widget.
 */
interface PlaylistCardActionMarker : MviAction

sealed class CardAction : PlaylistCardActionMarker {
    // fetch basic counts - liked, disliked, total
    class CalculateQuickCounts(val tracks: List<TrackModel>) : CardAction()
}

/**
 * Actions for the playlist card widget.
 */
sealed class PlaylistCardAction(val playlistId: String? = "") : PlaylistCardActionMarker {
    object None : PlaylistCardAction()

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                              playlistId: String,
                              val onlySwiped: Boolean = true,
                              val fields: String? = null,
                              val limit: Int = 100,
                              val offset: Int = 0) : PlaylistCardAction(playlistId)

    class CreateHeaderSet(val headerImageUrl: String) : PlaylistCardAction()
}
