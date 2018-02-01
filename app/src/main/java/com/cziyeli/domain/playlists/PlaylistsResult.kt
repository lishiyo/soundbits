package com.cziyeli.domain.playlists

import com.cziyeli.commons.mvibase.MviResult
import java.util.*

/**
 * Created by connieli on 12/31/17.
 */
interface HomeResult : MviResult

sealed class PlaylistsResult(var status: Status = Status.IDLE, var error: Throwable? = null) : HomeResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }

    // fetching user playlists
    class UserPlaylists(status: Status, error: Throwable?, val playlists: List<Playlist> = Collections.emptyList())
        : PlaylistsResult(status, error) {
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