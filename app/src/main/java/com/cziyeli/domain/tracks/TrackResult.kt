package com.cziyeli.domain.tracks

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.player.PlayerInterface
import java.util.*

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE, var error: Throwable? = null) : MviResult {

    class LoadTrackCards(status: Status,
                         error: Throwable?,
                         val items: List<TrackModel> = Collections.emptyList()
    ) : TrackResult(status, error) {
        // personal status enum
        enum class Status : MviResult.StatusInterface {
            SUCCESS, FAILURE, LOADING
        }

        companion object {
            fun createSuccess(items: List<TrackModel>) : LoadTrackCards {
                return LoadTrackCards(Status.SUCCESS, null, items)
            }
            fun createError(throwable: Throwable) : LoadTrackCards {
                return LoadTrackCards(Status.FAILURE, throwable)
            }
            fun createLoading(): LoadTrackCards {
                return LoadTrackCards(Status.LOADING, null)
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
                return CommandPlayerResult(MviResult.Status.ERROR, throwable, currentTrack, currentPlayerState)
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
                return ChangePrefResult(MviResult.Status.ERROR, throwable, currentTrack, currentTrack?.pref)
            }
        }
    }

}