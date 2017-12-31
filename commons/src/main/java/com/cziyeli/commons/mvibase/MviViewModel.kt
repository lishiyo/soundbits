package lishiyo.kotlin_arch.mvibase

import android.arch.lifecycle.LiveData
import io.reactivex.Observable

/**
 * Object that will subscribes to a [MviView]'s [MviIntent]s,
 * process it and emit a [MviViewState] back.
 *
 * @param <I> Top class of the [MviIntent] that the [MviViewModel] will be subscribing
 * to.
 * @param <S> Top class of the [MviViewState] the [MviViewModel] will be emitting.
</S></I> */
interface MviViewModel<in I : MviIntent, S : MviViewState> {
    // out means immutable, producer, ? extends I
    // List<CurrencyIntent> is subtype of List<MviIntent>, can do List<MviIntent> = List<CurrencyIntent>
    fun processIntents(intents: Observable<out I>)

    fun states(): LiveData<S>
}
