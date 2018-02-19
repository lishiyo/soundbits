package com.cziyeli.domain.summary

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.tracks.TrackModel

/**
 * Actions for the summary screen.
 */
interface SummaryActionMarker : MviAction

sealed class SummaryAction : SummaryActionMarker {
    // Create a new playlist
    // https://developer.spotify.com/web-api/console/post-playlists/
    class CreatePlaylistWithTracks(val ownerId: String,
                                   val name: String,
                                   val description: String?, // optional description
                                   val public: Boolean = false,
                                   val tracks: List<TrackModel> = listOf()
    ) : SummaryAction(), CardActionMarker

    // Add tracks to a playlist
    // https://developer.spotify.com/web-api/console/post-playlist-tracks/
    class AddTracksPlaylist(val ownerId: String,
                            val playlistId: String,
                            val tracks: List<TrackModel> // map to single string of track uris
    ) : SummaryAction(), CardActionMarker

    // Save tracks to local database
    class SaveTracks(val tracks: List<TrackModel>,
                     val playlistId: String?
    ) : SummaryAction()

}