package com.cziyeli.songbits.playlistcard

import com.cziyeli.commons.mvibase.MviIntent

/**
 * Marker for the [PlaylistCardActivity] screen.
 */
interface SinglePlaylistIntent : MviIntent
interface StatsIntent : MviIntent

/**
 * Events for [PlaylistCardWidget].
 **/
sealed class PlaylistCardIntent : SinglePlaylistIntent {
    // fetch basic counts - liked, disliked, total
    class FetchQuickStats(val ownerId: String,
                          val playlistId: String) : PlaylistCardIntent()

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                      val playlistId: String,
                      val onlySwiped: Boolean = true) : PlaylistCardIntent()
}

/**
 * Events for [TrackStatsView], widget representing stats for a [PlaylistCardWidget].
 */
sealed class TrackStatsIntent : StatsIntent {

    // fetch stats for all tracks
    class FetchStats(val trackIds: List<String>) : TrackStatsIntent()
}