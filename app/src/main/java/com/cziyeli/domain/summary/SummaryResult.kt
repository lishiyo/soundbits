package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.domain.tracks.TrackModel
import lishiyo.kotlin_arch.mvibase.MviResult

/**
 * Created by connieli on 1/7/18.
 */
sealed class SummaryResult(var status: MviResult.Status = MviResult.Status.IDLE, var error: Throwable? = null) : MviResult {
    companion object {
        private val TAG = SummaryResult::class.simpleName
    }

    class LoadStatsResult(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats? // domain model for stats
    ) : SummaryResult(status, error) {
        companion object {
            fun createSuccess(trackStats: TrackListStats) : LoadStatsResult {
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


    class SaveTracks(status: MviResult.Status,
                     error: Throwable?,
                     val insertedTracks: List<TrackModel>? = null,
                     val playlistId: String? = null
    ) : SummaryResult(status, error) {
        companion object {
            fun createSuccess(insertedTracks: List<TrackModel>, playlistId: String? = null) : SaveTracks {
                Utils.log(TAG, "SaveAllTracks --- createSuccess! size: ${insertedTracks.size} for ${playlistId}")
                return SaveTracks(MviResult.Status.SUCCESS, null, insertedTracks, playlistId)
            }
            fun createError(throwable: Throwable) : SaveTracks {
                Utils.log(TAG, "SaveAllTracks --- createError! ${throwable.localizedMessage}")
                return SaveTracks(MviResult.Status.FAILURE, throwable)
            }
            fun createLoading(): SaveTracks {
                return SaveTracks(MviResult.Status.LOADING, null)
            }
        }
    }
}