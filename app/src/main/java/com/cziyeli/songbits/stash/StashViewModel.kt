package com.cziyeli.songbits.stash

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.stash.StashAction
import com.cziyeli.domain.stash.StashActionMarker
import com.cziyeli.domain.stash.StashActionProcessor
import com.cziyeli.domain.stash.StashResultMarker
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.UserResult
import com.cziyeli.songbits.root.RootViewState
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

class StashViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: StashActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<StashIntent, StashViewModel.ViewState> {
    private val TAG = StashViewModel::class.simpleName
    private val compositeDisposable = CompositeDisposable()

    // Listener for home-specific events
    private val intentsSubject : PublishRelay<StashIntent> by lazy { PublishRelay.create<StashIntent>() }
    // Listener for root view states
    private val rootStatesSubject: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    // Publisher for own view states
    private val viewStates: PublishRelay<ViewState> by lazy { PublishRelay.create<ViewState>() }

    private val intentFilter: ObservableTransformer<StashIntent, StashIntent> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<StashIntent>(
                    shared.ofType(StashIntent.InitialLoad::class.java).take(1), // only take initial one time
                    shared.filter({ intent -> intent !is StashIntent.InitialLoad })
            )
        }
    }

    // Root ViewState => result
    private val rootResultProcessor: ObservableTransformer<RootViewState, MviResult> = ObservableTransformer { acts ->
        acts.map { rootState ->
            when {
                rootState.status == MviViewState.Status.SUCCESS && rootState.likedTracks.isNotEmpty() -> {
                    UserResult.LoadLikesCard.createSuccess(rootState.likedTracks)
                } else -> NoResult()
            }
        }
    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<ViewState, StashResultMarker, ViewState> = BiFunction { previousState, result ->
        when (result) {
            is UserResult.LoadLikesCard -> return@BiFunction processLikedTracks(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<StashActionMarker>())
                .compose(actionProcessor.combinedProcessor) // own action -> own result
                .mergeWith( // root viewstate -> own result
                        rootStatesSubject.compose(rootResultProcessor).compose(resultFilter<StashResultMarker>())
                )
                .observeOn(schedulerProvider.ui())
                .doOnNext { result -> Utils.log(TAG, "intentsSubject scanning result: ${result.javaClass.simpleName}") }
                .scan(ViewState(), reducer)
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
            StashIntent.InitialLoad() -> StashAction.InitialLoad()
            else -> None // no-op all other events
        }
    }

    /**
     * Bind to the root stream.
     */
    fun processRootViewStates(intents: Observable<RootViewState>) {
        compositeDisposable.add(
                intents.subscribe(rootStatesSubject::accept)
        )
    }

    override fun processIntents(intents: Observable<out StashIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<ViewState> {
        return viewStates
    }

    // ===== Individual reducers ======

    private fun processLikedTracks(
            previousState: ViewState,
            result: UserResult.LoadLikesCard
    ) : ViewState {
        Utils.mLog(TAG, "processLikedTracks", "status", "${result.status}", "tracks", "${result.items}")
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


    data class ViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: StashResultMarker? = null,
                         val likedTracks: MutableList<TrackModel> = mutableListOf(),
                         val dislikedTracks: MutableList<TrackModel> = mutableListOf()
    ) : MviViewState
}