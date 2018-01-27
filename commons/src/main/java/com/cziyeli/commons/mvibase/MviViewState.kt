package com.cziyeli.commons.mvibase

/**
 * Immutable object which contains all the required information to render a [MviView].
 */
interface MviViewState {
    // Implement to specify the exact status
    interface StatusInterface

    // Most generic status, use if you don't need to differentiate from others (i.e. just generic success)
    enum class Status : StatusInterface {
        IDLE, LOADING, SUCCESS, ERROR
    }
}
