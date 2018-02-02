package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult

/**
 * Marker interface for the playlist card widget.
 */
interface PlaylistCardActionMarker : MviAction
interface PlaylistCardResultMarker : MviResult

/**
 * Actions for the playlist card widget.
 */
sealed class PlaylistCardAction : PlaylistCardActionMarker {
    object None : PlaylistCardAction()

    // fetch basic counts - liked, disliked, total
    class FetchQuickStats(val playlistId: String) : PlaylistCardAction()

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                              val playlistId: String,
                              val onlySwiped: Boolean = true,
                              val fields: String? = null,
                              val limit: Int = 100,
                              val offset: Int = 0) : PlaylistCardAction()
}
