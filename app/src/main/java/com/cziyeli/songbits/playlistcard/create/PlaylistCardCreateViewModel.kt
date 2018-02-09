package com.cziyeli.songbits.playlistcard.create

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.playlistcard.PlaylistCardCreateActionProcessor
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.*
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.cards.summary.SummaryIntent
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.cziyeli.songbits.playlistcard.StatsIntent
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

class PlaylistCardCreateViewModel @Inject constructor(
        val repository: RepositoryImpl,
        val actionProcessor: PlaylistCardCreateActionProcessor,
        val schedulerProvider: BaseSchedulerProvider,
        initialState: PlaylistCardCreateViewModel.ViewState
) : ViewModel(), LifecycleObserver, MviViewModel<CardIntentMarker, PlaylistCardCreateViewModel.ViewState> {

    private val TAG = PlaylistCardCreateViewModel::class.simpleName

    // Full events stream to send for processing
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    // Simple already-processed events stream
    private val resultsSubject : PublishRelay<CardResultMarker> by lazy { PublishRelay.create<CardResultMarker>() }

    // Subject to publish ViewStates
    private val viewStates: PublishRelay<PlaylistCardCreateViewModel.ViewState> by lazy {
        PublishRelay.create<PlaylistCardCreateViewModel.ViewState>() }
    var currentViewState: PlaylistCardCreateViewModel.ViewState = initialState.copy()

    private val intentFilter: ObservableTransformer<CardIntentMarker, CardIntentMarker> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<CardIntentMarker>(
                    shared.ofType(StatsIntent.FetchStats::class.java).take(1), // only hit once
                    shared.filter({ intent ->
                        intent !is StatsIntent.FetchStats})
            )
        }
    }
    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<PlaylistCardCreateViewModel.ViewState, CardResultMarker, PlaylistCardCreateViewModel.ViewState> =
            BiFunction { previousState, result ->
        when (result) {
            is StatsResult.FetchStats -> return@BiFunction processFetchStats(previousState, result)
            is SummaryResult.CreatePlaylistWithTracks -> return@BiFunction processCreatePlaylistResult(previousState, result)
            is CardResult.HeaderSet -> return@BiFunction processSetHeaderUrl(previousState, result)
            else -> return@BiFunction previousState
        }
    }
    private val compositeDisposable = CompositeDisposable()

    var pendingTracks: List<TrackModel> = listOf()
        get() = currentViewState.pendingTracks

    init {
        // create observable to push into states live data
        val observable: Observable<PlaylistCardCreateViewModel.ViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<CardActionMarker>())
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .compose(actionProcessor.combinedProcessor)
                .mergeWith(resultsSubject) // <--- pipe in direct results
                .observeOn(schedulerProvider.ui())
                .scan(currentViewState, reducer) // final scan

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    currentViewState = viewState
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.mLog(TAG, "subscription", "error", err.localizedMessage)
                })
        )
    }

    // transform intent -> action
    private fun actionFromIntent(intent: MviIntent) : MviAction {
        return when (intent) {
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            is SummaryIntent.CreatePlaylistWithTracks -> SummaryAction.CreatePlaylistWithTracks(intent.ownerId, intent.name,
                    intent.description, intent.public, intent.tracks)
            else -> None
        }
    }

    private fun processFetchStats(
            previousState: PlaylistCardCreateViewModel.ViewState,
            result: StatsResult.FetchStats
    ) : PlaylistCardCreateViewModel.ViewState {
        return when (result.status) {
            StatsResultStatus.LOADING -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = StatsResultStatus.LOADING
                )
            }
            StatsResultStatus.SUCCESS -> {
                Utils.mLog(TAG, "processFetchStats success! ${result.trackStats.toString()}")
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = StatsResultStatus.SUCCESS,
                        trackStats = result.trackStats
                )
            }
            StatsResultStatus.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = StatsResultStatus.ERROR,
                        trackStats = result.trackStats
                )
            } else -> return previousState
        }

        return previousState
    }

    private fun processSetHeaderUrl(previousState: PlaylistCardCreateViewModel.ViewState,
                                    result: CardResult.HeaderSet
    ) : PlaylistCardCreateViewModel.ViewState {
        return previousState.copy(carouselHeaderUrl = result.headerImageUrl)
    }

    private fun processCreatePlaylistResult(previousState: PlaylistCardCreateViewModel.ViewState,
                                            result: SummaryResult.CreatePlaylistWithTracks
    ) : PlaylistCardCreateViewModel.ViewState {
        Utils.mLog(TAG, "processCreatePlaylistResult", "status", result.status.toString(),
                "created with snapshotId: ", "${result.snapshotId?.snapshot_id} for new playlist: ${result.playlistId}")

       return when (result.status) {
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.LOADING,
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.SUCCESS,
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR -> {
                return previousState.copy(
                        error = result.error,
                        status = result.status
                )
            } else -> previousState
        }
    }

    override fun processIntents(intents: Observable<out CardIntentMarker>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    fun processSimpleResults(results: Observable<out CardResultMarker>) {
        compositeDisposable.add(
                results.subscribe(resultsSubject::accept)
        )
    }

    override fun states(): Observable<ViewState> {
        return viewStates
    }

    data class ViewState(val status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         val error: Throwable? = null,
                         val playlistToAdd: Playlist? = null,
                         val pendingTracks: List<TrackModel>,
                         val trackStats: TrackListStats? = null, // stats for ALL tracks
                         val carouselHeaderUrl: String? = null,
                         val lastResult: CardResultMarker? = null
    ) : MviViewState {

        fun isFetchStatsSuccess(): Boolean {
            return status == StatsResultStatus.SUCCESS && trackStats != null
        }
        fun isCreateLoading(): Boolean {
            return status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.LOADING
        }
        fun isCreateFinished(): Boolean {
            return status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR ||
                    status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.SUCCESS
        }
        fun isError(): Boolean {
            return status == MviResult.Status.ERROR || status == StatsResultStatus.ERROR
            || status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR
        }
    }
}