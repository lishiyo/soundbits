package com.cziyeli.songbits.playlistcard

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlistcard.*
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.cards.TrackViewState
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject


class PlaylistCardViewModel @Inject constructor(
        private val repository: RepositoryImpl,
        val actionProcessor: PlaylistCardActionProcessor,
        val schedulerProvider: BaseSchedulerProvider,
        val initialState: PlaylistCardViewState
) : ViewModel(), LifecycleObserver, MviViewModel<PlaylistCardIntent, PlaylistCardViewModel.PlaylistCardViewState> {
    private val TAG = PlaylistCardViewModel::class.simpleName

    private var initialViewState : PlaylistCardViewState
    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<PlaylistCardViewState> by lazy { MutableLiveData<PlaylistCardViewState>() }
    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<PlaylistCardIntent> by lazy { PublishSubject.create<PlaylistCardIntent>() }
    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<PlaylistCardViewState, PlaylistCardResultMarker, PlaylistCardViewState> = BiFunction {
        previousState, result ->
            when (result) {
                is PlaylistCardResult.FetchQuickStats -> return@BiFunction processQuickStats(previousState, result)
                else -> return@BiFunction previousState
            }
    }

    private val compositeDisposable = CompositeDisposable()

    // secondary constructor to set initial playlist model
    init {
//    constructor(actionProcessor: PlaylistCardActionProcessor,
//                schedulerProvider: BaseSchedulerProvider,
//                initialState: PlaylistCardViewState) : this(actionProcessor, schedulerProvider) {

        initialViewState = initialState.copy()
        liveViewState.value = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<PlaylistCardViewState> = intentsSubject
                .filter { initialViewState != null }
                .subscribeOn(schedulerProvider.io())
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
            is PlaylistCardIntent.FetchQuickStats -> PlaylistCardAction.FetchQuickStats(intent.playlistId)
            is TrackStatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            else -> PlaylistCardAction.None
        }
    }

    // ===== MviViewModel =====

    override fun processIntents(intents: Observable<out PlaylistCardIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<PlaylistCardViewState> {
        return liveViewState
    }

    // ===== Individual reducers ======
    private fun processQuickStats(previousState: PlaylistCardViewState, result: PlaylistCardResult.FetchQuickStats) : PlaylistCardViewState {
        val newState = previousState.copy()
        newState.error = null

        when (result.status) {
            PlaylistCardResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            PlaylistCardResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.likedCount = result.likedCount
                newState.dislikedCount = result.dislikedCount
            }
            PlaylistCardResult.Status.FAILURE -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processTracksStats() {

    }

    private fun processTrackRows() {

    }

    data class PlaylistCardViewState(var status: MviViewState.Status = MviViewState.Status.IDLE,
                                     var error: Throwable? = null,
                                     var playlist: Playlist, // card's playlist - has total count
                                     var likedCount: Int = 0,
                                     var dislikedCount: Int = 0,
                                     val tracksList: List<TrackModel> = listOf(), // tracks for the expandable adapter
                                     var stats: TrackListStats? = null // main model
    ) : MviViewState {
        companion object {
            fun create(state: TrackViewState) : PlaylistCardViewState {
                return PlaylistCardViewState(
                        playlist = state.playlist // we require a playlist here
                )
            }
        }
    }
}