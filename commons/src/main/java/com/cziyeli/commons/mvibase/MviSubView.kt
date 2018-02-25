package com.cziyeli.commons.mvibase

import io.reactivex.Observable

/**
 * Subviews are child views of an [MviView], which is typically an activity/frag that can bind to a real arch-components [ViewModel].
 * Each subview has its own "viewmodel" (i.e. a presenter since it can't use arch-components) and view state.
 *
 * The parent [MviView] can act as a coordinator that:
 *   - delegates events to subviews (ex passing events from one subview to another, or pass lifecycle events)
 *   - observe the subview states to construct the full [MviViewState] for the activity/frag
 *
 * Created by connieli on 2/20/18.
 */
interface MviSubView<in I: MviIntent, S: MviViewState> {

    /**
     * Bind subview to intents stream - the subview will choose which events it is interested in.
     */
    fun processIntents(intents: Observable<out I>)

    /**
     * Entry point for the [MviSubView] to render itself based on a [MviViewState].
     */
    fun render(state: S)
}