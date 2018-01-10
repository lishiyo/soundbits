package com.cziyeli.domain.summary

import com.cziyeli.domain.tracks.TrackModel
import lishiyo.kotlin_arch.mvibase.MviAction

/**
 * Created by connieli on 1/7/18.
 */
sealed class SummaryAction : MviAction {
    object None : SummaryAction()

    // Fetch the stats
    class LoadStats(val trackIds: List<String>) : SummaryAction()

    // Create a new playlist
    // https://developer.spotify.com/web-api/console/post-playlists/
    class CreatePlaylist(val ownerId: Int,
                         val name: String,
                         val description: String?, // optional description
                         val public: Boolean = false
    ) : SummaryAction()

    // Add tracks to a playlist
    // https://developer.spotify.com/web-api/console/post-playlist-tracks/
    class AddTracksPlaylist(val ownerId: Int,
                            val playlistId: String,
                            val tracks: List<TrackModel> // map to uris
    ) : SummaryAction()

    // Save tracks to local database
    class SaveTracks(val tracks: List<TrackModel>,
                     val playlistId: String
    ) : SummaryAction()

}