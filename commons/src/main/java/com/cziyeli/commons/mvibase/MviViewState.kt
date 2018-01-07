package lishiyo.kotlin_arch.mvibase

/**
 * Immutable object which contains all the required information to render a [MviView].
 */
interface MviViewState {
    enum class Status {
        IDLE, LOADING, SUCCESS, ERROR
    }
}
