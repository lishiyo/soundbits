package com.cziyeli.domain.stash

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import com.cziyeli.domain.tracks.TrackModel

/**
 * Actions for Stash tab.
 */
interface StashActionMarker : MviAction

sealed class StashAction  : StashActionMarker {
    // initial creation
    class InitialLoad : StashAction()

    class ClearTracks(val pref: Repository.Pref) : StashAction()

    class FetchUserTopTracks(val time_range: String? = "medium_term",
                             val limit: Int = 50,
                             val offset: Int = 0) : StashAction()
}

/**
 * Results for Stash tab.
 */
interface StashResultMarker : MviResult

sealed class StashResult(var status: MviResult.Status = MviResult.Status.IDLE,
                         var error: Throwable? = null) : StashResultMarker {

    class InitialLoad(status: MviResult.Status) : StashResult(status) {
        companion object {
            fun createSuccess() : InitialLoad {
                return InitialLoad(MviResult.Status.SUCCESS)
            }
        }
    }

    class ClearTracks(status: MviResult.Status, error: Throwable? = null, pref: Repository.Pref? = null) : StashResult(status, error) {
        companion object {
            fun createSuccess(pref: Repository.Pref) : ClearTracks {
                return ClearTracks(MviResult.Status.SUCCESS, null, pref)
            }
            fun createError(error: Throwable?) : ClearTracks {
                return ClearTracks(MviResult.Status.ERROR, error)
            }
            fun createLoading() : ClearTracks {
                return ClearTracks(MviResult.Status.LOADING)
            }
        }
    }

    class FetchUserTopTracks(status: MviResult.Status, error: Throwable? = null,
                             val tracks: List<TrackModel> = mutableListOf()) : StashResult(status, error) {
        companion object {
            fun createSuccess(tracks: List<TrackModel>) : FetchUserTopTracks {
                return FetchUserTopTracks(MviResult.Status.SUCCESS, null, tracks)
            }
            fun createError(error: Throwable?) : FetchUserTopTracks {
                return FetchUserTopTracks(MviResult.Status.ERROR, error)
            }
            fun createLoading() : FetchUserTopTracks {
                return FetchUserTopTracks(MviResult.Status.LOADING)
            }
        }
    }


}