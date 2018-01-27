package lishiyo.kotlin_arch.mvibase

/**
 * Immutable object which contains all the required information to render a [MviView].
 */
interface MviViewState {
    interface StatusInterface

    enum class Status : StatusInterface {
        IDLE, LOADING, SUCCESS, ERROR
    }
}
