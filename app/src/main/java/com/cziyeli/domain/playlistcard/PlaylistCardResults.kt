package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.tracks.TrackModel
import java.util.*

interface CardResultMarker : MviResult

/**
 * Any kind of card.
 */
sealed class CardResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                        var error: Throwable? = null) : CardResultMarker {
    // calculate basic counts - liked, disliked, total
    class CalculateQuickCounts(status: Status,
                               error: Throwable?,
                               val likedCount: Int = 0,
                               val dislikedCount: Int = 0
    ) : CardResult(status, error) {
        enum class Status : MviResult.StatusInterface {
            LOADING, SUCCESS, ERROR, IDLE
        }

        companion object {
            fun createSuccess(likedCount: Int, dislikedCount: Int) : CalculateQuickCounts {
                return CalculateQuickCounts(Status.SUCCESS, null, likedCount, dislikedCount)
            }
            fun createError(throwable: Throwable) : CalculateQuickCounts {
                return CalculateQuickCounts(Status.ERROR, throwable)
            }
            fun createLoading(): CalculateQuickCounts {
                return CalculateQuickCounts(Status.LOADING, null)
            }
        }
    }

    class HeaderSet(val headerImageUrl: String) : CardResult(MviResult.Status.SUCCESS)

    class TracksSet(val tracks: List<TrackModel>) : CardResult(MviResult.Status.SUCCESS)
}

/**
 * Results for a single-playlist card.
 */
sealed class PlaylistCardResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                                var error: Throwable? = null) : CardResultMarker {

    // return list of tracks from either local or remote
    class FetchPlaylistTracks(status: FetchPlaylistTracks.Status,
                              error: Throwable?,
                              val items: List<TrackModel> = Collections.emptyList(),
                              val fromLocal: Boolean = true
    ) : PlaylistCardResult(status, error) {
        enum class Status : MviResult.StatusInterface {
            LOADING, SUCCESS, ERROR, IDLE
        }

        companion object {
            fun createSuccess(tracks: List<TrackModel>, fromLocal: Boolean) : FetchPlaylistTracks {
                return FetchPlaylistTracks(FetchPlaylistTracks.Status.SUCCESS, null, tracks, fromLocal)
            }
            fun createError(throwable: Throwable, fromLocal: Boolean) : FetchPlaylistTracks {
                return FetchPlaylistTracks(FetchPlaylistTracks.Status.ERROR, throwable, fromLocal = fromLocal)
            }
            fun createLoading(fromLocal: Boolean): FetchPlaylistTracks {
                return FetchPlaylistTracks(FetchPlaylistTracks.Status.LOADING, null, fromLocal = fromLocal)
            }
        }
    }

}
