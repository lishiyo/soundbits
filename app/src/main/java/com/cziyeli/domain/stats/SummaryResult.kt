package com.cziyeli.domain.stats

import lishiyo.kotlin_arch.mvibase.MviResult

/**
 * Created by connieli on 1/7/18.
 */
sealed class SummaryResult(var status: MviResult.Status = MviResult.Status.IDLE, var error: Throwable? = null) : MviResult {

    class LoadStatsResult(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats? // domain model for stats
    ) : SummaryResult(status, error) {
        fun createSuccess(trackStats: TrackListStats) : LoadStatsResult {
            return LoadStatsResult(MviResult.Status.SUCCESS, null, trackStats)
        }
        fun createError(throwable: Throwable,
                        trackStats: TrackListStats) : LoadStatsResult {
            return LoadStatsResult(MviResult.Status.FAILURE, throwable, trackStats)
        }
        fun createLoading(trackIds: List<String>): LoadStatsResult {
            return LoadStatsResult(MviResult.Status.LOADING, null, null)
        }
    }
}