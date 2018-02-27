package com.cziyeli.soundbits.playlistcard

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.soundbits.profile.ProfileIntentMarker

/**
 * Marker for any action that takes place on a card.
 */
interface CardIntentMarker : MviIntent

/**
 * Events for [PlaylistCardWidget].
 **/
sealed class PlaylistCardIntent : CardIntentMarker {

    // fetch basic counts - liked, disliked, total
    class CalculateQuickCounts(val tracks: List<TrackModel>) : PlaylistCardIntent()

    // get list of all (swiped) tracks to show in the expandable rows
    class FetchSwipedTracks(val ownerId: String,
                            val playlistId: String,
                            val onlySwiped: Boolean = true) : PlaylistCardIntent()
}

/**
 * Events for [TrackStatsView], widget representing stats for a card.
 */
sealed class StatsIntent : CardIntentMarker {

    // fetch tracks and stats for a playlist
    class FetchTracksWithStats(val playlist: Playlist) : StatsIntent()

    // fetch stats for given list of track ids
    class FetchStats(val trackIds: List<String>) : StatsIntent()

    // fetch full stats for given list of tracks
    class FetchFullStats(val trackModels: List<TrackModel>) : StatsIntent(), ProfileIntentMarker
}