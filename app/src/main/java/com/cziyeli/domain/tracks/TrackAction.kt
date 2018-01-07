package com.cziyeli.domain.tracks

import com.cziyeli.domain.player.PlayerInterface
import lishiyo.kotlin_arch.mvibase.MviAction

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackAction : MviAction {
    // no-op
    object None : TrackAction()

    // https://developer.spotify.com/web-api/console/get-playlist-tracks/
    class LoadTrackCards(val ownerId: String,
                         val playlistId: String,
                         val fields: String?,
                         val limit: Int = 100,
                         val offset: Int = 0) : TrackAction() {
        companion object {
            fun create(ownerId: String,
                       playlistId: String,
                       fields: String?,
                       limit: Int = 100,
                       offset: Int = 0): LoadTrackCards{
                return LoadTrackCards(ownerId, playlistId, fields, limit, offset)
            }
        }
    }

    // command the player to do something with a track
    class CommandPlayer(val command: PlayerInterface.Command,
                        val track: TrackModel) : TrackAction() {
        companion object {
            fun create(command: PlayerInterface.Command, track: TrackModel) : CommandPlayer {
                return CommandPlayer(command, track)
            }
        }
    }

    class ChangeTrackPref(val track: TrackModel,
                          val pref: TrackModel.Pref) : TrackAction() {
        companion object {
            fun create(track: TrackModel, pref: TrackModel.Pref) : ChangeTrackPref {
                return ChangeTrackPref(track, pref)
            }
        }
    }
}