package com.cziyeli.domain.tracks

import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.SwipeActionMarker

/**
 * Track-specific actions on the swipe screen.
 */
sealed class TrackAction : SwipeActionMarker {
    // just set tracks directly
    class SetTracks(val playlist: Playlist? = null, val tracks: List<TrackModel>): TrackAction()

    // https://developer.spotify.com/web-api/console/get-playlist-tracks/
    class LoadTrackCards(val ownerId: String,
                         val playlistId: String,
                         val onlyTrackIds: List<String> = mutableListOf(),
                         val fields: String?,
                         val limit: Int = 100,
                         val offset: Int = 0) : TrackAction() {
        companion object {
            fun create(ownerId: String,
                       playlistId: String,
                       onlyTrackIds: List<String> = mutableListOf(),
                       fields: String?,
                       limit: Int = 100,
                       offset: Int = 0): LoadTrackCards{
                return LoadTrackCards(ownerId, playlistId, onlyTrackIds, fields, limit, offset)
            }
        }
    }

    // command the player to do something with a track
    class CommandPlayer(val command: PlayerInterface.Command,
                        val track: TrackModel) : TrackAction(), CardActionMarker {
        companion object {
            fun create(command: PlayerInterface.Command, track: TrackModel) : CommandPlayer {
                return CommandPlayer(command, track)
            }
        }
    }

    // like/dislike a track
    class ChangeTrackPref(val track: TrackModel,
                          val pref: TrackModel.Pref) : TrackAction(), CardActionMarker {
        companion object {
            fun create(track: TrackModel, pref: TrackModel.Pref) : ChangeTrackPref {
                return ChangeTrackPref(track, pref)
            }
        }
    }

}