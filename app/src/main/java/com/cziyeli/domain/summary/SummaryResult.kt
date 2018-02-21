package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.tracks.TrackModel
import kaaes.spotify.webapi.android.models.SnapshotId

/**
 * Represents results that can handled by the summary screen.
 *
 * Created by connieli on 1/7/18.
 */
interface SummaryResultMarker : MviResult

sealed class SummaryResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE, var error: Throwable? = null) : SummaryResultMarker {
    companion object {
        private val TAG = SummaryResult::class.simpleName
    }

    /**
     * Single event - set tracks upon load.
     */
    class SetTracks : SummaryResult(MviResult.Status.SUCCESS)

    /**
     * Change liked/disliked status for a pending track.
     */
    class ChangeTrackPref(val track: TrackModel) : SummaryResult(MviResult.Status.SUCCESS, null)

    /**
     * Single event - mark playlist created.
     */
    class PlaylistCreated : SummaryResult(MviResult.Status.SUCCESS)

    /**
     * Load stats for the Liked tracks.
     */
    class FetchLikedStats(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats?,
                          val pref: Repository.Pref = Repository.Pref.LIKED
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

    class FetchDislikedStats(status: MviResult.Status,
                          error: Throwable?,
                          val trackStats: TrackListStats?,
                          val pref: Repository.Pref = Repository.Pref.DISLIKED
    ) : SummaryResult(status, error), StatsResultMarker {
        companion object {
            fun createSuccess(trackStats: TrackListStats) : FetchDislikedStats {
                Utils.mLog(TAG, "FetchDislikedStats success!")
                return FetchDislikedStats(MviResult.Status.SUCCESS, null, trackStats)
            }
            fun createError(throwable: Throwable,
                            trackStats: TrackListStats? = null) : FetchDislikedStats {
                Utils.mLog(TAG, "FetchDislikedStats error!")
                return FetchDislikedStats(MviResult.Status.ERROR, throwable, trackStats)
            }
            fun createLoading(): FetchDislikedStats {
                return FetchDislikedStats(MviResult.Status.LOADING, null, null)
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
    ) : SummaryResult(status, error), CardResultMarker {
        enum class CreateStatus : MviResult.StatusInterface {
           LOADING, SUCCESS, ERROR
        }
        companion object {
            fun createSuccess(playlistId: String,
                              snapshotId: SnapshotId
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
                Utils.log(TAG, "SaveAllTracks --- createSuccess! size: ${insertedTracks.size} for $playlistId")
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