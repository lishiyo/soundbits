package com.cziyeli.domain.user

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlists.HomeActionMarker
import com.cziyeli.domain.playlists.HomeResult
import com.cziyeli.domain.stash.StashActionMarker
import com.cziyeli.domain.stash.StashResultMarker
import com.cziyeli.domain.summary.StatsResultMarker
import com.cziyeli.domain.tracks.TrackModel
import java.util.*

/**
 * User actions. See also [ProfileAction] for personalization actions.
 */
sealed class UserAction : HomeActionMarker, StashActionMarker, CardActionMarker {

    class FetchUser : UserAction()

    class ClearUser : UserAction(), ProfileActionMarker

    class FetchQuickCounts : UserAction()

    // ========= STASHED ========

    open class LoadStashedTracks(val areLiked: Boolean = false,
                                 val limit: Int = 20,
                                 val offset: Int = 0,
                                 val fields: String? = null
    ) : UserAction()

    class LoadLikedTracks(limit: Int = 20,
                          offset: Int = 0,
                          fields: String? = null
    ) : LoadStashedTracks(true, limit, offset, fields), ProfileActionMarker

    class LoadDislikedTracks(limit: Int = 20,
                             offset: Int = 0,
                             fields: String? = null
    ) : LoadStashedTracks(false, limit, offset, fields)

}

// ==== RESULTS ====

interface UserStatusResult : MviResult.StatusInterface

/**
 * User results.
 */
sealed class UserResult(var status: UserStatusResult = Status.IDLE, var error: Throwable? = null) : HomeResult {
    // personal status
    enum class Status : UserStatusResult {
        LOADING, SUCCESS, ERROR, IDLE
    }

    // fetching user playlists
    class FetchUser(status: Status, error: Throwable?, val currentUser: User? = null) : UserResult(status, error) {
        companion object {
            fun createSuccess(currentUser: User) : FetchUser {
                return FetchUser(Status.SUCCESS, null, currentUser)
            }
            fun createError(throwable: Throwable) : FetchUser {
                return FetchUser(Status.ERROR, throwable)
            }
            fun createLoading(): FetchUser {
                return FetchUser(Status.LOADING, null)
            }
        }
    }

    class ClearUser(status: Status, error: Throwable?) : UserResult(status, error), ProfileResultMarker {
        companion object {
            fun createSuccess() : ClearUser {
                return ClearUser(Status.SUCCESS, null)
            }
            fun createError(throwable: Throwable) : ClearUser {
                return ClearUser(Status.ERROR, throwable)
            }
            fun createLoading(): ClearUser {
                return ClearUser(Status.LOADING, null)
            }
        }
    }

    class FetchQuickCounts(status: Status, error: Throwable?, val quickCounts: QuickCounts? = null
    ) : UserResult(status, error), StatsResultMarker {
        // counts status
        enum class Status : UserStatusResult {
            LOADING, SUCCESS, ERROR
        }

        companion object {
            fun createSuccess(counts: QuickCounts) : FetchQuickCounts {
                return FetchQuickCounts(Status.SUCCESS, null, counts)
            }
            fun createError(throwable: Throwable) : FetchQuickCounts {
                return FetchQuickCounts(Status.ERROR, throwable)
            }
            fun createLoading(): FetchQuickCounts {
                return FetchQuickCounts(Status.LOADING, null)
            }
        }
    }

    // fetching swiped likes
    class LoadLikedTracks(status: UserStatusResult,
                          error: Throwable?,
                          val items: List<TrackModel> = Collections.emptyList()
    ) : UserResult(status, error), StashResultMarker, ProfileResultMarker {
        companion object {
            fun createSuccess(tracks: List<TrackModel>) : LoadLikedTracks {
                return LoadLikedTracks(Status.SUCCESS, null, tracks)
            }
            fun createError(throwable: Throwable) : LoadLikedTracks {
                return LoadLikedTracks(Status.ERROR, throwable)
            }
            fun createLoading(): LoadLikedTracks {
                return LoadLikedTracks(Status.LOADING, null)
            }
        }
    }

    // fetching swiped dislikes
    class LoadDislikedTracks(status: UserStatusResult,
                             error: Throwable?,
                             val items: List<TrackModel> = Collections.emptyList()
    ) : UserResult(status, error), StashResultMarker {
        companion object {
            fun createSuccess(tracks: List<TrackModel>) : LoadDislikedTracks {
                return LoadDislikedTracks(Status.SUCCESS, null, tracks)
            }
            fun createError(throwable: Throwable) : LoadDislikedTracks {
                return LoadDislikedTracks(Status.ERROR, throwable)
            }
            fun createLoading(): LoadDislikedTracks {
                return LoadDislikedTracks(Status.LOADING, null)
            }
        }
    }
}