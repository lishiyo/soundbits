package com.cziyeli.songbits.playlistcard.create

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlistcard.*
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

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<PlaylistCardCreateViewModel.ViewState> by lazy {
        MutableLiveData<PlaylistCardCreateViewModel.ViewState>() }
    // subject to publish ViewStates
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }

    private val resultsSubject : PublishRelay<CardResultMarker> by lazy { PublishRelay.create<CardResultMarker>() }

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
        get() = if (liveViewState.value?.pendingTracks == null) listOf() else liveViewState.value!!.pendingTracks

    init {
        liveViewState.value = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<PlaylistCardCreateViewModel.ViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .filter { act -> act != PlaylistCardAction.None }
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .compose(actionProcessor.combinedProcessor)
                .mergeWith(resultsSubject) // pipe in results directly!
                .observeOn(schedulerProvider.ui())
                .scan(liveViewState.value, reducer)

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    liveViewState.postValue(viewState) // triggers render in the view
                }, { err ->
                    Utils.mLog(TAG, "subscription", "error", err.localizedMessage)
                })
        )
    }

    // transform intent -> action
    private fun actionFromIntent(intent: MviIntent) : CardActionMarker {
        return when (intent) {
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            is SummaryIntent.CreatePlaylistWithTracks -> SummaryAction.CreatePlaylistWithTracks(intent.ownerId, intent.name,
                    intent.description, intent.public, intent.tracks)
//            is CardIntent.HeaderSet -> CardAction.HeaderSet(intent.headerImageUrl)
            else -> PlaylistCardAction.None
        }
    }

    private fun processFetchStats(
            previousState: PlaylistCardCreateViewModel.ViewState,
            result: StatsResult.FetchStats
    ) : PlaylistCardCreateViewModel.ViewState {
        val newState = previousState.copy()
        newState.error = null

        when (result.status) {
            StatsResultStatus.LOADING -> {
                newState.status = StatsResultStatus.LOADING
            }
            StatsResultStatus.SUCCESS -> {
                Utils.mLog(TAG, "processFetchStats success! ${result.trackStats.toString()}")
                newState.status = StatsResultStatus.SUCCESS
                newState.trackStats = result.trackStats
            }
            StatsResultStatus.ERROR -> {
                newState.status = StatsResultStatus.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processSetHeaderUrl(previousState: PlaylistCardCreateViewModel.ViewState,
                                    result: CardResult.HeaderSet
    ) : PlaylistCardCreateViewModel.ViewState {
        val newState = previousState.copy()
        newState.carouselHeaderUrl = result.headerImageUrl
        return newState
    }

    private fun processCreatePlaylistResult(previousState: PlaylistCardCreateViewModel.ViewState,
                                            result: SummaryResult.CreatePlaylistWithTracks
    ) : PlaylistCardCreateViewModel.ViewState {
        val newState = previousState.copy()
        newState.error = null
        newState.status = result.status
        Utils.mLog(TAG, "processCreatePlaylistResult", "status", result.status.toString(),
                "created with snapshotId: ", "${result.snapshotId?.snapshot_id} for new playlist: ${result.playlistId}")

        when (result.status) {
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.LOADING -> {

            }
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.SUCCESS -> {

            }
            SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR -> {
                newState.error = result.error
            }
        }

        return newState
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

    override fun states(): LiveData<ViewState> {
        return liveViewState
    }

    data class ViewState(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null,
                         var playlistToAdd: Playlist? = null,
                         var pendingTracks: List<TrackModel>,
                         var trackStats: TrackListStats? = null, // stats for ALL tracks
                         var carouselHeaderUrl: String? = null
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

        // make sure the tracks are there!
        fun copy() : ViewState {
            return ViewState(status, error, playlistToAdd, pendingTracks, trackStats, carouselHeaderUrl)
        }
    }
}