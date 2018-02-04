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
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.SinglePlaylistIntent
import com.cziyeli.songbits.playlistcard.StatsIntent
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

class PlaylistCardCreateViewModel @Inject constructor(
        val repository: RepositoryImpl,
        val actionProcessor: PlaylistCardCreateActionProcessor,
        val schedulerProvider: BaseSchedulerProvider,
        val initialState: PlaylistCardCreateViewModel.ViewState
) : ViewModel(), LifecycleObserver, MviViewModel<SinglePlaylistIntent, PlaylistCardCreateViewModel.ViewState> {

    private val TAG = PlaylistCardCreateViewModel::class.simpleName

    private var initialViewState : PlaylistCardCreateViewModel.ViewState
    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<PlaylistCardCreateViewModel.ViewState> by lazy {
        MutableLiveData<PlaylistCardCreateViewModel.ViewState>() }
    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<SinglePlaylistIntent> by lazy { PublishSubject.create<SinglePlaylistIntent>() }
    private val intentFilter: ObservableTransformer<SinglePlaylistIntent, SinglePlaylistIntent> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<SinglePlaylistIntent>(
                    shared.ofType(StatsIntent.FetchStats::class.java).take(1), // only hit once
                    shared.filter({ intent ->
                        intent !is StatsIntent.FetchStats})
            )
        }
    }
    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<PlaylistCardCreateViewModel.ViewState, PlaylistCardResultMarker, PlaylistCardCreateViewModel.ViewState> =
            BiFunction { previousState, result ->
        when (result) {
            is StatsResult.FetchStats -> return@BiFunction processFetchStats(previousState, result)
            else -> return@BiFunction previousState
        }
    }
    private val compositeDisposable = CompositeDisposable()

    init {
        initialViewState = initialState.copy()
        liveViewState.value = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<PlaylistCardCreateViewModel.ViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .filter { act -> act != PlaylistCardAction.None }
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .observeOn(schedulerProvider.ui())
                .compose(actionProcessor.combinedProcessor)
                .scan(initialViewState, reducer)

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    liveViewState.postValue(viewState) // triggers render in the view
                }, { err ->
                    Utils.mLog(TAG, "subscription", "error", err.localizedMessage)
                })
        )
    }

    // transform intent -> action
    private fun actionFromIntent(intent: MviIntent) : PlaylistCardActionMarker {
        return when (intent) {
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
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

    override fun processIntents(intents: Observable<out SinglePlaylistIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<ViewState> {
        return liveViewState
    }

    data class ViewState(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null,
                         var playlistToAdd: Playlist? = null,
                         var pendingTracks: List<TrackModel>,
                         var trackStats: TrackListStats? = null // stats for ALL tracks
    ) : MviViewState {
        fun isSuccess(): Boolean {
            return status == StatsResultStatus.SUCCESS
        }
        // make sure the tracks are there!
        fun copy() : ViewState {
            return ViewState(status, error, playlistToAdd, pendingTracks, trackStats)
        }
    }
}