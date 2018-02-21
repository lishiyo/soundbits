package com.cziyeli.commons.mvibase

import io.reactivex.Observable

/**
 * Object that will subscribes to a [MviView]'s [MviIntent]s,
 * process it and emit a [MviViewState] back.
 *
 * @param <I> Top class of the [MviIntent] that the [MviViewModel] will be subscribing
 * to.
 * @param <S> Top class of the [MviViewState] the [MviViewModel] will be emitting.
</S></I> */
interface MviViewModel<in I : MviIntent, S : MviViewState, in R: MviResult> {

    /**
     * Bind view model to any intents (ex. from multiple views).
     */
    fun processIntents(intents: Observable<out I>)

    /**
     * Stream of view states for views to subscribe to.
     */
    fun states(): Observable<S>

    /**
     * Optional - bind view model to a stream of [MviResult]s, shortcircuiting [MviIntent] and [MviAction] processing.
     */
    fun processSimpleResults(results: Observable<out R>) { /* no-op */ }
}
