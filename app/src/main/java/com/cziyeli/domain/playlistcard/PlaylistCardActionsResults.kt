package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlists.PlaylistsResult

/**
 * Marker interface for the playlist card widget.
 */
interface PlaylistCardActionMarker : MviAction
interface PlaylistCardResultMarker : MviResult

/**
 * Actions for the playlist card widget.
 */
sealed class PlaylistCardAction : PlaylistCardActionMarker {

    // fetch basic counts - liked, disliked, total
    class FetchQuickStats(val ownerId: String,
                          val playlistId: String) : PlaylistCardAction()

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                              val playlistId: String,
                              val onlySwiped: Boolean = true) : PlaylistCardAction()
}

sealed class PlaylistCardResult(var status: PlaylistsResult.Status = PlaylistsResult.Status.IDLE,
                                var error: Throwable? = null) : PlaylistCardResultMarker {

    // fetch basic counts - liked, disliked, total
    class FetchQuickStats(val ownerId: String,
                          val playlistId: String) : PlaylistCardResult()

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                              val playlistId: String,
                              val onlySwiped: Boolean = true) : PlaylistCardResult()
}
