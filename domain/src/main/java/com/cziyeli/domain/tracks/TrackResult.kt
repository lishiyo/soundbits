package com.cziyeli.domain.tracks

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

}