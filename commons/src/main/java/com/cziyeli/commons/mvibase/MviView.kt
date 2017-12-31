package lishiyo.kotlin_arch.mvibase

import io.reactivex.Observable

/**
 * Object representing a UI that will
 * a) emit its intents to a view model,
 * b) subscribes to a view model for rendering its UI.
 *
 * @param <I> Top class of the [MviIntent] that the [MviView] will be emitting.
 * @param <S> Top class of the [MviViewState] the [MviView] will be subscribing to.
</S></I> */
interface MviView<out I : MviIntent, in S : MviViewState> {
    /**
     * Unique [<] used by the [MviViewModel]
     * to listen to the [MviView].
     * All the [MviView]'s [MviIntent]s must go through this [<].
     */
    fun intents(): Observable<out I>

    /**
     * Entry point for the [MviView] to render itself based on a [MviViewState].
     */
    fun render(state: S)
}
