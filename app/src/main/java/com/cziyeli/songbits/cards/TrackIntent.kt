package com.cziyeli.songbits.cards

import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.tracks.TrackCard
import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackIntent : MviIntent {

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

    // command the player to do something with a track
    class CommandPlayer(val command: PlayerInterface.Command,
                        val track: TrackCard) : TrackIntent() {
        companion object {
            fun create(command: PlayerInterface.Command, track: TrackCard) : CommandPlayer {
                return CommandPlayer(command, track)
            }
        }
    }

    // destroy player
}
