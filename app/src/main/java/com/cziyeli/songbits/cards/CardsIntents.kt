package com.cziyeli.songbits.cards

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.CardIntentMarker

/**
 * Created by connieli on 1/1/18.
 */
sealed class CardsIntent : MviIntent, CardIntentMarker {

    // opened CardsActivity *with* tracks to swipe
    class ScreenOpenedWithTracks(val playlist: Playlist,
                                 val tracks: List<TrackModel>) : CardsIntent()

    // opened CardsActivity, no tracks given - fetch tracks from remote
    class ScreenOpenedNoTracks(val ownerId: String,
                               val playlistId: String,
                               val onlyTrackIds: List<String> = mutableListOf(),
                               val fields: String? = null,
                               val limit: Int = 100,
                               val offset: Int = 0
    ) : CardsIntent() {
        companion object {
            fun create(ownerId: String,
                       playlistId: String,
                       onlyTrackIds: List<String> = mutableListOf(),
                       fields: String? = null,
                       limit: Int = 100,
                       offset: Int = 0): ScreenOpenedNoTracks {
                return ScreenOpenedNoTracks(ownerId, playlistId, onlyTrackIds, fields, limit, offset)
            }
        }
    }

    // command the player to play/pause/stop a track
    class CommandPlayer(val command: PlayerInterface.Command,
                        val track: TrackModel) : CardsIntent() {
        companion object {
            fun create(command: PlayerInterface.Command, track: TrackModel) : CommandPlayer {
                return CommandPlayer(command, track)
            }
        }
    }

    // like or dislike a track
    class ChangeTrackPref(val track: TrackModel,
                          val pref: TrackModel.Pref
    ) : CardsIntent(), CardIntentMarker {
        companion object {
            fun like(track: TrackModel) : ChangeTrackPref {
                return ChangeTrackPref(track, TrackModel.Pref.LIKED)
            }
            fun dislike(track: TrackModel) : ChangeTrackPref {
                return ChangeTrackPref(track, TrackModel.Pref.DISLIKED)
            }
            fun clear(track: TrackModel) : ChangeTrackPref {
                return ChangeTrackPref(track, TrackModel.Pref.UNSEEN)
            }
        }
    }

}
