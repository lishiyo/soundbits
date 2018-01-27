package lishiyo.kotlin_arch.mvibase

/**
 * Immutable object resulting of a processed business logic.
 */
interface MviResult {
    interface StatusInterface

    enum class Status : StatusInterface {
        LOADING, SUCCESS, FAILURE, IDLE
    }
}
