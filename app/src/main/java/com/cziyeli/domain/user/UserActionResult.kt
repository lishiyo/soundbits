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
 * User actions.
 */
sealed class UserAction : HomeActionMarker, StashActionMarker, CardActionMarker {

    class FetchUser : UserAction()

    class ClearUser : UserAction()

    class FetchQuickCounts : UserAction()

    open class LoadStashedTracks(val areLiked: Boolean = false,
                                 val limit: Int = 20,
                                 val offset: Int = 0,
                                 val fields: String? = null
    ) : UserAction()

    class LoadLikedTracks(limit: Int = 20,
                          offset: Int = 0,
                          fields: String? = null
    ) : LoadStashedTracks(true, limit, offset, fields)

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
sealed class UserResult(var status: UserStatusResult = Status.IDLE, var error: Throwable? = null)  : HomeResult {
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

    class ClearUser(status: Status, error: Throwable?) : UserResult(status, error) {
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
    class LoadLikesCard(status: UserStatusResult,
                        error: Throwable?,
                        val items: List<TrackModel> = Collections.emptyList()
    ) : UserResult(status, error), StashResultMarker {
        companion object {
            fun createSuccess(tracks: List<TrackModel>) : LoadLikesCard {
                return LoadLikesCard(Status.SUCCESS, null, tracks)
            }
            fun createError(throwable: Throwable) : LoadLikesCard {
                return LoadLikesCard(Status.ERROR, throwable)
            }
            fun createLoading(): LoadLikesCard {
                return LoadLikesCard(Status.LOADING, null)
            }
        }
    }

    // fetching swiped dislikes
    class LoadDislikesCard(status: UserStatusResult,
                           error: Throwable?,
                           val items: List<TrackModel> = Collections.emptyList()
    ) : UserResult(status, error), StashResultMarker {
        companion object {
            fun createSuccess(tracks: List<TrackModel>) : LoadDislikesCard {
                return LoadDislikesCard(Status.SUCCESS, null, tracks)
            }
            fun createError(throwable: Throwable) : LoadDislikesCard {
                return LoadDislikesCard(Status.ERROR, throwable)
            }
            fun createLoading(): LoadDislikesCard {
                return LoadDislikesCard(Status.LOADING, null)
            }
        }
    }
}