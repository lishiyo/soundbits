package com.cziyeli.domain.tracks

import android.app.Activity
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

    // opened - create a new Player
    class CreatePlayer(val activity: Activity, val accessToken: String) : TrackAction() {
        companion object {
            fun create(activity: Activity, accessToken: String) : CreatePlayer {
                return CreatePlayer(activity, accessToken)
            }
        }
    }
}