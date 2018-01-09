package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import lishiyo.kotlin_arch.mvibase.MviResult

/**
 * Created by connieli on 1/7/18.
 */
sealed class SummaryResult(var status: MviResult.Status = MviResult.Status.IDLE, var error: Throwable? = null) : MviResult {

    class LoadStatsResult(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats? // domain model for stats
    ) : SummaryResult(status, error) {

        companion object {
            private val TAG = SummaryResult::class.simpleName

            fun createSuccess(trackStats: TrackListStats) : LoadStatsResult {
                Utils.log(TAG, "createSuccess! size: ${trackStats.trackStats.size}")
                return LoadStatsResult(MviResult.Status.SUCCESS, null, trackStats)
            }
            fun createError(throwable: Throwable,
                            trackStats: TrackListStats? = null) : LoadStatsResult {
                return LoadStatsResult(MviResult.Status.FAILURE, throwable, trackStats)
            }
            fun createLoading(): LoadStatsResult {
                return LoadStatsResult(MviResult.Status.LOADING, null, null)
            }
        }
    }


}