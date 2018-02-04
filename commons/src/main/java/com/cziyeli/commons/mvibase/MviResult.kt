package com.cziyeli.commons.mvibase

/**
 * Immutable object resulting of a processed business logic.
 */
interface MviResult {
    // Implement to specify what triggered this result
    interface StatusInterface

    // Most generic status, use if you don't need to differentiate from others (i.e. just generic success)
    enum class Status : StatusInterface {
        LOADING, SUCCESS, ERROR, IDLE
    }
}
