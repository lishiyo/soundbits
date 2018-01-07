package com.cziyeli.domain.tracks

import com.cziyeli.domain.player.PlayerInterface
import lishiyo.kotlin_arch.mvibase.MviResult
import java.util.*

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackResult(var status: MviResult.Status = MviResult.Status.IDLE, var error: Throwable? = null) : MviResult {

    class LoadTrackCards(status: MviResult.Status, error: Throwable?,
                         val items: List<TrackModel> = Collections.emptyList()
    ) : TrackResult(status, error) {
        companion object {
            fun createSuccess(items: List<TrackModel>) : LoadTrackCards {
                return LoadTrackCards(MviResult.Status.SUCCESS, null, items)
            }
            fun createError(throwable: Throwable) : LoadTrackCards {
                return LoadTrackCards(MviResult.Status.FAILURE, throwable)
            }
            fun createLoading(): LoadTrackCards {
                return LoadTrackCards(MviResult.Status.LOADING, null)
            }
        }
    }

    class CommandPlayerResult(status: MviResult.Status,
                              error: Throwable?,
                              val currentTrack: TrackModel?,
                              val currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED
    ) : TrackResult(status, error) {
        companion object {
            fun createSuccess(currentTrack: TrackModel,
                              currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED) :
                    CommandPlayerResult {
                return CommandPlayerResult(MviResult.Status.SUCCESS, null, currentTrack, currentPlayerState)
            }
            fun createError(throwable: Throwable,
                            currentTrack: TrackModel?,
                            currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED) : CommandPlayerResult {
                return CommandPlayerResult(MviResult.Status.FAILURE, throwable, currentTrack, currentPlayerState)
            }
            fun createLoading(currentTrack: TrackModel,
                              currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED): CommandPlayerResult {
                return CommandPlayerResult(MviResult.Status.LOADING, null, currentTrack, currentPlayerState)
            }
        }
    }

    class ChangePrefResult(status: MviResult.Status,
                           error: Throwable?,
                           val currentTrack: TrackModel?,
                           val pref: TrackModel.Pref?
    ) : TrackResult(status, error) {
        companion object {
            fun createSuccess(currentTrack: TrackModel,
                              pref: TrackModel.Pref) : ChangePrefResult {
                return ChangePrefResult(MviResult.Status.SUCCESS, null, currentTrack, pref)
            }
            fun createError(throwable: Throwable,
                            currentTrack: TrackModel? = null) : ChangePrefResult {
                // roll back to the original pref
                return ChangePrefResult(MviResult.Status.FAILURE, throwable, currentTrack, currentTrack?.pref)
            }
        }
    }

}