package com.cziyeli.domain.playlists

import com.cziyeli.domain.user.User


sealed class UserAction : HomeAction {

    class FetchUser : UserAction()

}

sealed class UserResult(var status: Status = Status.IDLE, var error: Throwable? = null)  : HomeResult {

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
}