package com.cziyeli.domain.stash

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult

/**
 * Actions for Stash tab.
 */
interface StashActionMarker : MviAction

sealed class StashAction  : StashActionMarker {
    // initial creation
    class InitialLoad : StashAction()
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

}