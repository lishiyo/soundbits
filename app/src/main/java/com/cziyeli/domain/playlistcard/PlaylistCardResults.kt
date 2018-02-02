package com.cziyeli.domain.playlistcard


sealed class PlaylistCardResult(var status: PlaylistCardResult.Status = PlaylistCardResult.Status.IDLE,
                                var error: Throwable? = null) : PlaylistCardResultMarker {
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }

    // fetch basic counts - liked, disliked, total
    class FetchQuickStats(status: PlaylistCardResult.Status,
                          error: Throwable?,
                          val likedCount: Int = 0,
                          val dislikedCount: Int = 0
    ) : PlaylistCardResult(status, error) {
        companion object {
            fun createSuccess(likedCount: Int, dislikedCount: Int) : FetchQuickStats {
                return FetchQuickStats(PlaylistCardResult.Status.SUCCESS, null, likedCount, dislikedCount)
            }
            fun createError(throwable: Throwable) : FetchQuickStats{
                return FetchQuickStats(PlaylistCardResult.Status.FAILURE, throwable)
            }
            fun createLoading(): FetchQuickStats {
                return FetchQuickStats(PlaylistCardResult.Status.LOADING, null)
            }
        }
    }

    // get list of all (swiped) tracks
    class FetchPlaylistTracks(val ownerId: String,
                              val playlistId: String,
                              val onlySwiped: Boolean = true) : PlaylistCardResult()
}
