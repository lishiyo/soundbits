package com.cziyeli.domain.playlists

import lishiyo.kotlin_arch.mvibase.MviResult
import java.util.*

/**
 * Created by connieli on 12/31/17.
 */
sealed class PlaylistResult(var status: Status = Status.IDLE, var error: Throwable? = null) : MviResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }

    // fetching user playlists
    class UserPlaylists(status: Status, error: Throwable?, val playlists: List<Playlist> = Collections.emptyList())
        : PlaylistResult(status, error) {
        companion object {
            fun createSuccess(playlists: List<Playlist>) : UserPlaylists {
                return UserPlaylists(Status.SUCCESS, null, playlists)
            }
            fun createError(throwable: Throwable) : UserPlaylists {
                return UserPlaylists(Status.FAILURE, throwable)
            }
            fun createLoading(): UserPlaylists {
                return UserPlaylists(Status.LOADING, null)
            }
        }
    }

}