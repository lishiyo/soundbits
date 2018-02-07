package com.cziyeli.domain.summary

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.tracks.TrackModel


/**
 * Marker action/results interface for stats.
 */
interface StatsActionMarker : MviAction
interface StatsResultMarker : MviResult

/**
 * Actions related to track stats.
 */
sealed class StatsAction : StatsActionMarker, SummaryActionMarker, CardActionMarker {

    // fetch all tracks + stats given playlist id
    class FetchAllTracksWithStats(val ownerId: String,
                                  val playlistId: String,
                                  val fields: String? = null,
                                  val limit: Int = 100,
                                  val offset: Int = 0) : StatsAction()

    // fetch stats for a given list of tracks
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
sealed class StatsResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null) : StatsResultMarker, CardResultMarker {

    class FetchAllTracksWithStats(status: StatsResultStatus,
                                  error: Throwable?,
                                  val tracks: List<TrackModel> = listOf(),
                                  val trackStats: TrackListStats? // domain model for stats
    ) : StatsResult(status, error) {
        companion object {
            fun createSuccess(tracks: List<TrackModel>, trackStats: TrackListStats) : StatsResult.FetchAllTracksWithStats {
                return StatsResult.FetchAllTracksWithStats(StatsResultStatus.SUCCESS, null, tracks, trackStats)
            }
            fun createError(throwable: Throwable,
                            tracks: List<TrackModel> = listOf(),
                            trackStats: TrackListStats? = null) : StatsResult.FetchAllTracksWithStats {
                return StatsResult.FetchAllTracksWithStats(StatsResultStatus.ERROR, throwable, tracks, trackStats)
            }
            fun createLoading(): StatsResult.FetchAllTracksWithStats {
                return StatsResult.FetchAllTracksWithStats(StatsResultStatus.LOADING, null, listOf(), null)
            }
        }
    }

    class FetchStats(status: StatsResultStatus,
                     error: Throwable?,
                     val trackStats: TrackListStats? // domain model for stats
    ) : StatsResult(status, error) {
        companion object {
            fun createSuccess(trackStats: TrackListStats) : StatsResult.FetchStats {
                return StatsResult.FetchStats(StatsResultStatus.SUCCESS, null, trackStats)
            }
            fun createError(throwable: Throwable,
                            trackStats: TrackListStats? = null) : StatsResult.FetchStats {
                return StatsResult.FetchStats(StatsResultStatus.ERROR, throwable, trackStats)
            }
            fun createLoading(): StatsResult.FetchStats {
                return StatsResult.FetchStats(StatsResultStatus.LOADING, null, null)
            }
        }
    }
}