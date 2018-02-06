package com.cziyeli.songbits.cards.summary

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.CardIntentMarker

/**
 * Events in the summary layout after swiping through the cards.
 *
 * Created by connieli on 1/6/18.
 */
sealed class SummaryIntent : MviIntent, CardIntentMarker {
    // TODO: what do I need to create pending PlaylistCard on open
    // fetch audio features for a bunch of tracks => transform to domain model TrackListStats
    class FetchStats(val trackIds: List<String>) : SummaryIntent()

    // create playlist out of liked -> hit db and spotify api
    class CreatePlaylistWithTracks(val ownerId: String,
                                   val name: String,
                                   val description: String? = null, // optional description
                                   val public: Boolean = false,
                                   val tracks: List<TrackModel>
    ) : SummaryIntent()

    // add to an existing playlist -> hit db and spotify
    class AddTracksPlaylist(val ownerId: String,
                            val playlistId: String,
                            val tracks: List<TrackModel>
    ) : SummaryIntent()

    // save liked and disliked and keep surfing other playlists -> hit db
    class SaveAllTracks(val tracks: List<TrackModel>,
                        val playlistId: String // coming from playlist
    ) : SummaryIntent()

    // review Disliked pile -> multi-selection view -> edit liked/disliked -> Summary
    class ViewAllTracks(val likedTracks: List<TrackModel>,
                       val dislikedTracks: List<TrackModel>,
                       val playlistId: String // coming from playlist
    ) : SummaryIntent()
}