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
    class CreatePlaylistWithTracks(val ownerId: String,
                                   val name: String,
                                   val description: String?, // optional description
                                   val public: Boolean = false,
                                   val tracks: List<TrackModel> = listOf()
    ) : SummaryAction()

    // Add tracks to a playlist
    // https://developer.spotify.com/web-api/console/post-playlist-tracks/
    class AddTracksPlaylist(val ownerId: String,
                            val playlistId: String,
                            val tracks: List<TrackModel> // map to single string of track uris
    ) : SummaryAction()

    // Save tracks to local database
    class SaveTracks(val tracks: List<TrackModel>,
                     val playlistId: String
    ) : SummaryAction()

}