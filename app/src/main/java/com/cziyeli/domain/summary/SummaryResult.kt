package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.PlaylistCardResultMarker
import com.cziyeli.domain.tracks.TrackModel
import kaaes.spotify.webapi.android.models.SnapshotId

/**
 * Created by connieli on 1/7/18.
 */
interface SummaryResultMarker : MviResult

enum class SinglePlaylistResult : MviResult.StatusInterface {
    SUCCESS, ERROR, LOADING
}

sealed class SummaryResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE, var error: Throwable? = null) : SummaryResultMarker {
    companion object {
        private val TAG = SummaryResult::class.simpleName
    }

    /**
     * Load stats for the Liked tracks.
     */
    class FetchLikedStats(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats? // domain model for stats
    ) : SummaryResult(status, error), StatsResultMarker {
        companion object {
            fun createSuccess(trackStats: TrackListStats) : FetchLikedStats {
                return FetchLikedStats(MviResult.Status.SUCCESS, null, trackStats)
            }
            fun createError(throwable: Throwable,
                            trackStats: TrackListStats? = null) : FetchLikedStats {
                return FetchLikedStats(MviResult.Status.ERROR, throwable, trackStats)
            }
            fun createLoading(): FetchLikedStats {
                return FetchLikedStats(MviResult.Status.LOADING, null, null)
            }
        }
    }

    /**
     * Create a playlist out of tracks.
     */
    class CreatePlaylistWithTracks(status: CreateStatus,
                                   error: Throwable? = null,
                                   val playlistId: String? = null,
                                   val snapshotId: SnapshotId? = null
//                                   val tracks: List<TrackModel> = listOf()
    ) : SummaryResult(status, error), PlaylistCardResultMarker {
        enum class CreateStatus : MviResult.StatusInterface {
           LOADING, SUCCESS, ERROR
        }
        companion object {
            fun createSuccess(playlistId: String,
                              snapshotId: SnapshotId
//                              tracks: List<TrackModel> = listOf()
            ) : CreatePlaylistWithTracks {
                return CreatePlaylistWithTracks(CreateStatus.SUCCESS, playlistId = playlistId, snapshotId = snapshotId)
            }
            fun createError(throwable: Throwable) : CreatePlaylistWithTracks {
                return CreatePlaylistWithTracks(CreateStatus.ERROR, throwable)
            }
            fun createLoading(): CreatePlaylistWithTracks {
                return CreatePlaylistWithTracks(CreateStatus.LOADING)
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
                return SaveTracks(MviResult.Status.ERROR, throwable)
            }
            fun createLoading(): SaveTracks {
                return SaveTracks(MviResult.Status.LOADING, null)
            }
        }
    }
}