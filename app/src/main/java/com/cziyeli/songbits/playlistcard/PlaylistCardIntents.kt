package com.cziyeli.songbits.playlistcard

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.domain.playlists.Playlist

/**
 * Marker for the [PlaylistCardActivity] screen.
 */
interface SinglePlaylistIntent : MviIntent

/**
 * Events for [PlaylistCardWidget].
 **/
sealed class PlaylistCardIntent : SinglePlaylistIntent {

    // fetch basic counts - liked, disliked, total
//    class CalculateQuickCounts(val playlistId: String) : PlaylistCardIntent()

    // get list of all (swiped) tracks to show in the expandable rows
    class FetchSwipedTracks(val ownerId: String,
                            val playlistId: String,
                            val onlySwiped: Boolean = true) : PlaylistCardIntent()
}

/**
 * Events for [TrackStatsView], widget representing stats for a [PlaylistCardWidget].
 */
sealed class StatsIntent : SinglePlaylistIntent {

    // fetch tracks and tats for a playlist
    class FetchTracksWithStats(val playlist: Playlist) : StatsIntent()

    // fetch stats for given list of track ids
    class FetchStats(val trackIds: List<String>) : StatsIntent()
}