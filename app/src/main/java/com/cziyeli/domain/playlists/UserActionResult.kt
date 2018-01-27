package com.cziyeli.domain.playlists

import com.cziyeli.domain.user.User


sealed class UserAction : HomeAction {

    class FetchUser : UserAction()

    class ClearUser : UserAction()
}

sealed class UserResult(var status: Status = Status.IDLE, var error: Throwable? = null)  : HomeResult {
    // personal status
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }

    // fetching user playlists
    class FetchUser(status: Status, error: Throwable?, val currentUser: User? = null
    ) : UserResult(status, error) {
        companion object {
            fun createSuccess(currentUser: User) : FetchUser {
                return FetchUser(Status.SUCCESS, null, currentUser)
            }
            fun createError(throwable: Throwable) : FetchUser {
                return FetchUser(Status.FAILURE, throwable)
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
                return ClearUser(Status.FAILURE, throwable)
            }
            fun createLoading(): ClearUser {
                return ClearUser(Status.LOADING, null)
            }
        }
    }
}