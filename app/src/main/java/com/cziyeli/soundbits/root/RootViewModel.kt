package com.cziyeli.soundbits.root

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.QuickCounts
import com.cziyeli.domain.user.UserAction
import com.cziyeli.domain.user.UserResult
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Viewmodel for [RootActivity] and shared data between its tabs - a kind of "global state".
 */
class RootViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: RootActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<RootIntent, RootViewState, MviResult> {
    private val TAG = RootViewModel::class.simpleName

    private val intentsSubject : PublishRelay<RootIntent> by lazy { PublishRelay.create<RootIntent>() }
    private val viewStates: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    private val compositeDisposable = CompositeDisposable()
    private val intentFilter: ObservableTransformer<RootIntent, RootIntent> = ObservableTransformer { intents ->
        intents.publish { _ ->
            intents.publish { shared -> shared
                Observable.merge<RootIntent>(
                        shared.ofType(RootIntent.FetchQuickCounts::class.java).take(1),
                        shared.ofType(RootIntent.LoadLikedTracks::class.java).take(1),
                        shared.ofType(RootIntent.LoadDislikedTracks::class.java).take(1)
                ).mergeWith(
                        shared.filter({ intent -> intent !is SingleEventIntent || (intent.shouldRefresh())})
                )
            }
        }
    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<RootViewState, MviResult, RootViewState> = BiFunction { previousState, result ->
        when (result) {
            is UserResult.FetchQuickCounts -> return@BiFunction processUserQuickCounts(previousState, result)
            is UserResult.LoadLikedTracks -> return@BiFunction processLikedTracks(previousState, result)
            is UserResult.LoadDislikedTracks -> return@BiFunction processDislikedTracks(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable: Observable<RootViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .doOnSubscribe{ Utils.log(TAG, "subscribed!") }
                .doOnDispose{ Utils.log( TAG, "disposed!") }
                .doOnTerminate { Utils.log( TAG, "terminated!") }
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<MviAction>())
                .compose(actionProcessor.combinedProcessor) // action -> result
                .observeOn(schedulerProvider.ui())
                .doOnNext { result -> Utils.log(TAG, "intentsSubject scanning result: ${result.javaClass.simpleName}") }
                .scan(RootViewState(), reducer)
                .replay(1)
                .autoConnect(0) // automatically connect

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.log(TAG, err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : MviAction {
        return when(intent) {
            is RootIntent.FetchQuickCounts -> UserAction.FetchQuickCounts()
            is RootIntent.LoadLikedTracks -> UserAction.LoadLikedTracks(intent.limit, intent.offset)
            is RootIntent.LoadDislikedTracks -> UserAction.LoadDislikedTracks(intent.limit, intent.offset)
            else -> None // no-op all other events
        }
    }

    private fun processUserQuickCounts(previousState: RootViewState, result: UserResult.FetchQuickCounts) : RootViewState {
        Utils.mLog(TAG, "processUserQuickCounts", "status", result.status.toString())

        return when (result.status) {
            UserResult.FetchQuickCounts.Status.SUCCESS -> {
                previousState.copy(
                        error = null,
                        status = MviViewState.Status.SUCCESS,
                        lastResult = result,
                        quickCounts = result.quickCounts
                )
            }
            UserResult.FetchQuickCounts.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.ERROR,
                        lastResult = result,
                        quickCounts = result.quickCounts
                )
            }
            else -> previousState
        }
    }

    private fun processLikedTracks(
            previousState: RootViewState,
            result: UserResult.LoadLikedTracks
    ) : RootViewState {
        return when (result.status) {
            UserResult.Status.LOADING, MviResult.Status.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            UserResult.Status.SUCCESS, MviResult.Status.SUCCESS -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        likedTracks = result.items.toMutableList()
                )

            }
            UserResult.Status.ERROR, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR
                )
            }
            else -> previousState
        }
    }

    private fun processDislikedTracks(
            previousState: RootViewState,
            result: UserResult.LoadDislikedTracks
    ) : RootViewState {
        return when (result.status) {
            UserResult.Status.LOADING, MviResult.Status.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            UserResult.Status.SUCCESS, MviResult.Status.SUCCESS -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        dislikedTracks = result.items.toMutableList()
                )
            }
            UserResult.Status.ERROR, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR
                )
            }
            else -> previousState
        }
    }

    override fun processIntents(intents: Observable<out RootIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<RootViewState> {
        return viewStates
    }

}

data class RootViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: MviResult = NoResult(),
                         val quickCounts: QuickCounts? = null,
                         val likedTracks: MutableList<TrackModel> = mutableListOf(),
                         val dislikedTracks: MutableList<TrackModel> = mutableListOf()
) : MviViewState {

    override fun toString(): String {
        return "status: $status -- lastResult: $lastResult -- liked: ${likedTracks.size} -- disliked: ${dislikedTracks.size}"
    }
}