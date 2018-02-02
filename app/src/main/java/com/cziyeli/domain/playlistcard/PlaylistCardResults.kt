package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.tracks.TrackModel
import java.util.*


sealed class PlaylistCardResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                                var error: Throwable? = null) : PlaylistCardResultMarker {

    // fetch basic counts - liked, disliked, total
    class FetchQuickStats(status: Status,
                          error: Throwable?,
                          val likedCount: Int = 0,
                          val dislikedCount: Int = 0
    ) : PlaylistCardResult(status, error) {
        enum class Status : MviResult.StatusInterface {
            LOADING, SUCCESS, ERROR, IDLE
        }

        companion object {
            fun createSuccess(likedCount: Int, dislikedCount: Int) : FetchQuickStats {
                return FetchQuickStats(Status.SUCCESS, null, likedCount, dislikedCount)
            }
            fun createError(throwable: Throwable) : FetchQuickStats{
                return FetchQuickStats(Status.ERROR, throwable)
            }
            fun createLoading(): FetchQuickStats {
                return FetchQuickStats(Status.LOADING, null)
            }
        }
    }

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(status: FetchPlaylistTracks.Status,
                              error: Throwable?,
                              val items: List<TrackModel> = Collections.emptyList()
    ) : PlaylistCardResult(status, error) {
        enum class Status : MviResult.StatusInterface {
            LOADING, SUCCESS, ERROR, IDLE
        }

        companion object {
            fun createSuccess(items: List<TrackModel>) : FetchPlaylistTracks{
                return FetchPlaylistTracks(FetchPlaylistTracks.Status.SUCCESS, null, items)
            }
            fun createError(throwable: Throwable) : FetchPlaylistTracks {
                return FetchPlaylistTracks(FetchPlaylistTracks.Status.ERROR, throwable)
            }
            fun createLoading(): FetchPlaylistTracks {
                return FetchPlaylistTracks(FetchPlaylistTracks.Status.LOADING, null)
            }
        }
    }
}
