package com.cziyeli.songbits.stash

import android.arch.lifecycle.LifecycleObserver
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.summary.*
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.cards.CardsIntent
import com.cziyeli.songbits.cards.summary.SummaryIntent
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

    // Listener for own events
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    // Listener for simple already-processed events stream
    private val resultsSubject : PublishRelay<CardResultMarker> by lazy { PublishRelay.create<CardResultMarker>() }
    // Publisher for own view states
    private val viewStates: PublishRelay<SimpleCardViewModel.ViewState> by lazy {
        PublishRelay.create<SimpleCardViewModel.ViewState>() }
    var currentViewState: SimpleCardViewModel.ViewState = ViewState()

    var pendingTracks: List<TrackModel> = listOf()
        get() = currentViewState.tracks

    val inCreateMode: Boolean
        get() = currentViewState.inCreateMode

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<ViewState, CardResultMarker, ViewState> = BiFunction { previousState, result ->
        when (result) {
            is CardResult.HeaderSet -> return@BiFunction processSetHeaderUrl(previousState, result)
            is CardResult.TracksSet -> return@BiFunction processSetTracks(previousState, result)
            is StatsResult.FetchFullStats -> return@BiFunction processFetchFullStats(previousState, result)
            is TrackResult.ChangePrefResult -> return@BiFunction processTrackChangePref(previousState, result)
            is SummaryResult.CreatePlaylistWithTracks -> return@BiFunction processCreatePlaylistResult(previousState, result)
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
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<CardActionMarker>())
                .compose(actionProcessor.combinedProcessor) // action -> result
                .mergeWith(resultsSubject) // <--- pipe in direct results
                .observeOn(schedulerProvider.ui())
                .doOnNext { result -> Utils.log(TAG, "intentsSubject scanning result: ${result.javaClass.simpleName}") }
                .scan(ViewState(), reducer)

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    currentViewState = viewState
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.mLog(TAG, "init", "error", err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : MviAction {
        return when(intent) {
            is StatsIntent.FetchFullStats -> StatsAction.FetchFullStats(intent.trackModels)
            is CardsIntent.ChangeTrackPref -> TrackAction.ChangeTrackPref(intent.track, intent.pref)
            is SummaryIntent.CreatePlaylistWithTracks -> SummaryAction.CreatePlaylistWithTracks(intent.ownerId, intent.name,
                    intent.description, intent.public, intent.tracks)
            else -> None // no-op all other events
        }
    }

    fun processSimpleResults(results: Observable<out CardResultMarker>) {
        compositeDisposable.add(
                results.subscribe(resultsSubject::accept)
        )
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

    private fun processSetTracks(previousState: ViewState,
                                 result: CardResult.TracksSet
    ) : ViewState {
        return previousState.copy(
                status = MviViewState.Status.SUCCESS,
                tracks = result.tracks.toMutableList())
    }

    private fun processFetchFullStats(
            previousState: ViewState,
            result: StatsResult.FetchFullStats
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
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        trackStats = result.trackStats
                )
            }
            else -> return previousState
        }
    }

    private fun processTrackChangePref(previousState: ViewState,
                                       result: TrackResult.ChangePrefResult) : ViewState {
        return when (result.status) {
            TrackResult.ChangePrefResult.Status.LOADING, TrackResult.ChangePrefResult.Status.ERROR -> {
                val status = if (result.status == TrackResult.ChangePrefResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = status
                )
            }
            TrackResult.ChangePrefResult.Status.SUCCESS -> {
                Utils.mLog(TAG, "change track pref! ${result.currentTrack?.name}: ${result.currentTrack?.pref}")
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS
                )
            } else -> previousState
        }
    }

    private fun processCreatePlaylistResult(previousState: ViewState,
                                            result: SummaryResult.CreatePlaylistWithTracks
    ) : ViewState {
        Utils.mLog(TAG, "processCreatePlaylistResult", "status", result.status.toString(),
                "created with snapshotId: ", "${result.snapshotId?.snapshot_id} for new playlist: ${result.playlistId}")
        return when (result.status) {
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.LOADING, SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR -> {
                val status = if (result.status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = status
                )
            }
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.SUCCESS -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS
                )
            }
            else -> return previousState
        }
    }

    data class ViewState(val status: MviViewState.StatusInterface = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: CardResultMarker? = null,
                         val carouselHeaderUrl: String? = null,
                         val tracks: MutableList<TrackModel> = mutableListOf(),
                         val trackStats: TrackListStats? = null, // stats for ALL tracks
                         val inCreateMode: Boolean = false
    ) : MviViewState {
        fun isFetchStatsSuccess(): Boolean {
            return lastResult is StatsResult.FetchFullStats && trackStats != null
        }
        fun isCreateLoading(): Boolean {
            return status == MviViewState.Status.LOADING && lastResult is SummaryResult.CreatePlaylistWithTracks
        }
        fun isCreateFinished(): Boolean {
            return lastResult is SummaryResult.CreatePlaylistWithTracks && (
                    status == MviViewState.Status.ERROR || status == MviViewState.Status.SUCCESS)
        }
        fun isError(): Boolean {
            return status == MviResult.Status.ERROR
        }
    }
}
