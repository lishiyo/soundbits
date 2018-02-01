package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.tracks.TrackModel
import kaaes.spotify.webapi.android.models.SnapshotId

/**
 * Created by connieli on 1/7/18.
 */
sealed class SummaryResult(var status: MviResult.Status = MviResult.Status.IDLE, var error: Throwable? = null) : MviResult {
    companion object {
        private val TAG = SummaryResult::class.simpleName
    }

    /**
     * Load stats for the Liked tracks.
     */
    class FetchLikedStats(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats? // domain model for stats
    ) : SummaryResult(status, error) {
        companion object {
            fun createSuccess(trackStats: TrackListStats) : FetchLikedStats {
                return FetchLikedStats(MviResult.Status.SUCCESS, null, trackStats)
            }
            fun createError(throwable: Throwable,
                            trackStats: TrackListStats? = null) : FetchLikedStats {
                return FetchLikedStats(MviResult.Status.FAILURE, throwable, trackStats)
            }
            fun createLoading(): FetchLikedStats {
                return FetchLikedStats(MviResult.Status.LOADING, null, null)
            }
        }
    }

    /**
     * Create a playlit out of tracks.
     */
    class CreatePlaylistWithTracks(status: MviResult.Status,
                                   error: Throwable? = null,
                                   val playlistId: String? = null,
                                   val snapshotId: SnapshotId? = null
//                                   val tracks: List<TrackModel> = listOf()
    ) : SummaryResult(status, error) {
        companion object {
            fun createSuccess(playlistId: String,
                              snapshotId: SnapshotId
//                              tracks: List<TrackModel> = listOf()
            ) : CreatePlaylistWithTracks {
                return CreatePlaylistWithTracks(MviResult.Status.SUCCESS, playlistId = playlistId, snapshotId = snapshotId)
            }
            fun createError(throwable: Throwable) : CreatePlaylistWithTracks {
                return CreatePlaylistWithTracks(MviResult.Status.FAILURE, throwable)
            }
            fun createLoading(): CreatePlaylistWithTracks {
                return CreatePlaylistWithTracks(MviResult.Status.LOADING)
            }
        }
    }

    /**
     * Save tracks to database.
     */
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