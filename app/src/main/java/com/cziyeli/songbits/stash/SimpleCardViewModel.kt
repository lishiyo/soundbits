package com.cziyeli.songbits.stash

import android.arch.lifecycle.LifecycleObserver
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.cziyeli.songbits.playlistcard.StatsIntent
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * Not a real arch components view model.
 *
 */
class SimpleCardViewModel constructor(
        val actionProcessor: SimpleCardActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : LifecycleObserver, MviViewModel<CardIntentMarker, SimpleCardViewModel.ViewState> {
    private val TAG = SimpleCardViewModel::class.java.simpleName
    private val compositeDisposable = CompositeDisposable()

    // Listener for home-specific events
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    // Listener for root view states
//    private val rootStatesSubject: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    // Publisher for own view states
    private val viewStates: PublishRelay<SimpleCardViewModel.ViewState> by lazy {
        PublishRelay.create<SimpleCardViewModel.ViewState>() }
    var currentViewState: SimpleCardViewModel.ViewState = ViewState()

    // Root ViewState => Result
//    private val rootResultProcessor: ObservableTransformer<RootViewState, MviResult> = ObservableTransformer { acts ->
//        acts.map { rootState ->
//            when {
//                rootState.status == MviViewState.Status.SUCCESS && rootState.likedTracks.isNotEmpty() -> {
//                    UserResult.LoadLikesCard.createSuccess(rootState.likedTracks!!)
//                } else -> NoResult()
//            }
//        }
//    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<ViewState, CardResultMarker, ViewState> = BiFunction { previousState, result ->
        when (result) {
//            is UserResult.LoadLikesCard -> return@BiFunction processLikedTracks(previousState, result)
            is CardResult.HeaderSet -> return@BiFunction processSetHeaderUrl(previousState, result)
            is StatsResult.FetchStats -> return@BiFunction processFetchStats(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    // secondary constructor
    constructor(actionProcessor: SimpleCardActionProcessor,
                schedulerProvider: BaseSchedulerProvider,
                initialState: ViewState) : this(actionProcessor, schedulerProvider) {
        currentViewState = initialState.copy()

        // create observable to push into states live data
        val observable = intentsSubject
                .subscribeOn(schedulerProvider.io())
//                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<CardActionMarker>())
                .compose(actionProcessor.combinedProcessor) // action -> result
//                .mergeWith( // root viewstate -> home result
//                        rootStatesSubject.compose(rootResultProcessor).compose(resultFilter<CardResultMarker>())
//                )
                .observeOn(schedulerProvider.ui())
                .doOnNext { result -> Utils.log(TAG, "intentsSubject scanning result: ${result.javaClass.simpleName}") }
                .scan(ViewState(), reducer)
                .replay(1)
                .autoConnect(0) // automatically connect

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    currentViewState = viewState
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.log(TAG, err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : MviAction {
        return when(intent) {
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            else -> None // no-op all other events
        }
    }

    override fun processIntents(intents: Observable<out CardIntentMarker>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<SimpleCardViewModel.ViewState> {
        return viewStates
    }

    // ===== Individual reducers ======

    private fun processSetHeaderUrl(previousState: ViewState,
                                    result: CardResult.HeaderSet
    ) : ViewState {
        return previousState.copy(carouselHeaderUrl = result.headerImageUrl)
    }

    private fun processFetchStats(
            previousState: ViewState,
            result: StatsResult.FetchStats
    ) : ViewState {
        return when (result.status) {
            StatsResultStatus.LOADING, StatsResultStatus.ERROR -> {
                val status = if (result.status == StatsResultStatus.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = status
                )
            }
            StatsResultStatus.SUCCESS -> {
                Utils.mLog(TAG, "processFetchStats success! ${result.trackStats.toString()}")
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        trackStats = result.trackStats
                )
            }
            else -> return previousState
        }

        return previousState
    }

    data class ViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: CardResultMarker? = null,
                         val carouselHeaderUrl: String? = null,
                         val tracks: MutableList<TrackModel> = mutableListOf(),
                         val trackStats: TrackListStats? = null // stats for ALL tracks
    ) : MviViewState {
        fun isFetchStatsSuccess(): Boolean {
            return lastResult == StatsResultStatus.SUCCESS && trackStats != null
        }
    }
}
