package com.cziyeli.commons.mvibase

import io.reactivex.Observable

/**
 * Created by connieli on 2/20/18.
 */
interface MviSubView<in I: MviIntent, S: MviViewState> {

    /**
     * Allow subview to listen to intents stream.
     */
    fun processIntents(intents: Observable<out I>)

    /**
     * Stream of view states for parent view to subscribe to.
     */
    fun states(): Observable<S>
}