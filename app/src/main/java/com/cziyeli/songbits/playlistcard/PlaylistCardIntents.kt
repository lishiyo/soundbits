package com.cziyeli.songbits.playlistcard

import com.cziyeli.commons.mvibase.MviIntent

/**
 * Marker for the [PlaylistCardActivity] screen.
 */
interface SinglePlaylistIntent : MviIntent

/**
 * Events for [PlaylistCardWidget].
 **/
sealed class PlaylistCardIntent : SinglePlaylistIntent {
    // liked, disliked, total
    class FetchQuickStats(val ownerId: String,
                        val playlistId: String) : PlaylistCardIntent()

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                      val playlistId: String,
                      val onlySwiped: Boolean = true) : PlaylistCardIntent()
}

/**
 * Events for [TrackStatsView].
 */
sealed class TrackStatsIntent : SinglePlaylistIntent {

    // fetch stats for a single playlist
    class FetchStats(val trackIds: List<String>) : TrackStatsIntent()
}