package lishiyo.kotlin_arch.mvibase

/**
 * Immutable object resulting of a processed business logic.
 */
interface MviResult {
    enum class Status {
        LOADING, SUCCESS, FAILURE, IDLE
    }
}
