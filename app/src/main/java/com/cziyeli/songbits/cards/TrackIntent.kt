package com.cziyeli.songbits.cards

import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackIntent : MviIntent {

    // https://developer.spotify.com/web-api/console/get-playlist-tracks/
    class LoadTrackCards(val ownerId: String,
                         val playlistId: String,
                         val fields: String?,
                         val limit: Int = 100,
                         val offset: Int = 0
    ) : TrackIntent() {
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

}
