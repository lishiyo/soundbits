package com.cziyeli.domain.tracks

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.summary.SwipeResultMarker
import java.util.*

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE, var error: Throwable? = null
) : SwipeResultMarker {

    class LoadTrackCards(status: Status,
                         error: Throwable?,
                         val items: List<TrackModel> = Collections.emptyList()
    ) : TrackResult(status, error) {
        // personal status enum
        enum class Status : MviResult.StatusInterface {
            SUCCESS, ERROR, LOADING
        }

        companion object {
            fun createSuccess(items: List<TrackModel>) : LoadTrackCards {
                return LoadTrackCards(Status.SUCCESS, null, items)
            }
            fun createError(throwable: Throwable) : LoadTrackCards {
                return LoadTrackCards(Status.ERROR, throwable)
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
    ) : TrackResult(status, error), CardResultMarker {
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

    class ChangePrefResult(status: Status,
                           error: Throwable?,
                           val currentTrack: TrackModel?,
                           val pref: TrackModel.Pref?
    ) : TrackResult(status, error), CardResultMarker {
        enum class Status : MviResult.StatusInterface {
            SUCCESS, ERROR, LOADING
        }

        companion object {
            fun createSuccess(currentTrack: TrackModel,
                              pref: TrackModel.Pref) : ChangePrefResult {
                return ChangePrefResult(Status.SUCCESS, null, currentTrack, pref)
            }
            fun createError(throwable: Throwable,
                            currentTrack: TrackModel? = null) : ChangePrefResult {
                // roll back to the original pref
                return ChangePrefResult(Status.ERROR, throwable, currentTrack, currentTrack?.pref)
            }
            fun createLoading(currentTrack: TrackModel? = null) : ChangePrefResult {
                return ChangePrefResult(Status.LOADING, null, currentTrack, currentTrack?.pref)
            }
        }
    }

}