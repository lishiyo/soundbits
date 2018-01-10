package com.cziyeli.songbits.cards.summary

import com.cziyeli.domain.tracks.TrackModel
import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Events in the summary layout.
 *
 * Created by connieli on 1/6/18.
 */
sealed class SummaryIntent : MviIntent {
    // TODO: what do I need to create pending PlaylistCard on open
    // fetch audio features for a bunch of tracks => transform to domain model TrackListStats
    class LoadStats(val trackIds: List<String>) : SummaryIntent()

    // create playlist out of liked -> hit db and spotify api
    class CreatePlaylist(val ownerId: Int,
                         val name: String,
                         val description: String?, // optional description
                         val public: Boolean = false
    ) : SummaryIntent()

    // add to an existing playlist -> hit db and spotify
    class AddTracksPlaylist(val ownerId: Int,
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