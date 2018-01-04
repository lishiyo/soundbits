package com.cziyeli.domain.tracks

import com.cziyeli.domain.player.PlayerInterface
import lishiyo.kotlin_arch.mvibase.MviResult
import java.util.*

/**
 * Created by connieli on 1/1/18.
 */
sealed class TrackResult(var status: Status = Status.IDLE, var error: Throwable? = null) : MviResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }

    class TrackCards(status: Status, error: Throwable?, val items: List<TrackCard> = Collections.emptyList())
        : TrackResult(status, error) {
        companion object {
            fun createSuccess(items: List<TrackCard>) : TrackCards {
                return TrackCards(Status.SUCCESS, null, items)
            }
            fun createError(throwable: Throwable) : TrackCards {
                return TrackCards(Status.FAILURE, throwable)
            }
            fun createLoading(): TrackCards {
                return TrackCards(Status.LOADING, null)
            }
        }
    }

    class CommandPlayerResult(status: Status,
                              error: Throwable?,
                              val currentTrack: TrackCard?,
                              val currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED)
        : TrackResult(status, error) {
        companion object {
            fun createSuccess(currentTrack: TrackCard,
                              currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED) :
                    CommandPlayerResult {
                return CommandPlayerResult(Status.SUCCESS, null, currentTrack, currentPlayerState)
            }
            fun createError(throwable: Throwable,
                            currentTrack: TrackCard?,
                            currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED) : CommandPlayerResult {
                return CommandPlayerResult(Status.FAILURE, throwable, currentTrack, currentPlayerState)
            }
            fun createLoading(currentTrack: TrackCard,
                              currentPlayerState: PlayerInterface.State = PlayerInterface.State.NOT_PREPARED): CommandPlayerResult {
                return CommandPlayerResult(Status.LOADING, null, currentTrack, currentPlayerState)
            }
        }
    }
}