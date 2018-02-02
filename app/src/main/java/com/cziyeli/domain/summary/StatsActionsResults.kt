package com.cziyeli.domain.summary

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.PlaylistCardActionMarker
import com.cziyeli.domain.playlistcard.PlaylistCardResultMarker


/**
 * Marker action/results interface for stats.
 */
interface StatsActionMarker : MviAction
interface StatsResultMarker : MviResult

/**
 * Actions related to track stats.
 */
sealed class StatsAction : StatsActionMarker, SummaryActionMarker, PlaylistCardActionMarker {

    // generic fetch stats for a list of tracks
    class FetchStats(val trackIds: List<String>) : StatsAction()
}

/**
 * Result status.
 */
enum class StatsResultStatus : MviResult.StatusInterface {
    SUCCESS, ERROR, LOADING
}

/**
 * Results for the track stats widget
 */
sealed class TrackStatsResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                              var error: Throwable? = null) : StatsResultMarker, PlaylistCardResultMarker {

    class FetchStats(status: StatsResultStatus,
                     error: Throwable?,
                     val trackStats: TrackListStats? // domain model for stats
    ) : TrackStatsResult(status, error) {
        companion object {
            fun createSuccess(trackStats: TrackListStats) : TrackStatsResult.FetchStats {
                return TrackStatsResult.FetchStats(StatsResultStatus.SUCCESS, null, trackStats)
            }
            fun createError(throwable: Throwable,
                            trackStats: TrackListStats? = null) : TrackStatsResult.FetchStats {
                return TrackStatsResult.FetchStats(StatsResultStatus.ERROR, throwable, trackStats)
            }
            fun createLoading(): TrackStatsResult.FetchStats {
                return TrackStatsResult.FetchStats(StatsResultStatus.LOADING, null, null)
            }
        }
    }
}