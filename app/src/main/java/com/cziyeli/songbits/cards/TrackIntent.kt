package com.cziyeli.songbits.cards

import android.app.Activity
import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackIntent : MviIntent {

    // opened CardsActivity, no player yet - create player
    class NewScreen(val activity: Activity, val accessToken: String) : TrackIntent() {
        companion object {
            fun create(activity: Activity, accessToken: String) : NewScreen {
                return NewScreen(activity, accessToken)
            }
        }
    }

    // opened CardsActivity, loaded view model - load tracks
    class ScreenOpened(val ownerId: String,
                       val playlistId: String,
                       val fields: String? = null,
                       val limit: Int = 100,
                       val offset: Int = 0
    ) : TrackIntent() {
        companion object {
            fun create(ownerId: String,
                       playlistId: String,
                       fields: String? = null,
                       limit: Int = 100,
                       offset: Int = 0): ScreenOpened {
                return ScreenOpened(ownerId, playlistId, fields, limit, offset)
            }
        }
    }


    // command track

    // destroy player
}
