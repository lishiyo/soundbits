package com.cziyeli.domain.playlists

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.summary.StatsResultMarker
import com.cziyeli.domain.user.QuickCounts
import com.cziyeli.domain.user.User


sealed class UserAction : HomeAction {

    class FetchUser : UserAction()

    class ClearUser : UserAction()

    class FetchQuickCounts : UserAction()
}

interface UserStatusResult : MviResult.StatusInterface

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
            LOADING, SUCCESS, ERROR, IDLE
        }

        companion object {
            fun createSuccess(counts: QuickCounts) : FetchQuickCounts {
                return FetchQuickCounts(FetchQuickCounts.Status.SUCCESS, null, counts)
            }
            fun createError(throwable: Throwable) : FetchQuickCounts {
                return FetchQuickCounts(FetchQuickCounts.Status.ERROR, throwable)
            }
            fun createLoading(): FetchQuickCounts {
                return FetchQuickCounts(FetchQuickCounts.Status.LOADING, null)
            }
        }
    }
}