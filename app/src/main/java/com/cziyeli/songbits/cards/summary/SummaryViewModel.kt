package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.*
import android.widget.Toast
import com.cziyeli.commons.SingleLiveEvent
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.commons.toast
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.SummaryAction
import com.cziyeli.domain.summary.SummaryActionProcessor
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.cards.TrackViewState
import com.cziyeli.songbits.di.App
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Shown after you finish surfing all the cards.
 *
 * Created by connieli on 1/6/18.
 */

class SummaryViewModel @Inject constructor(
        val actionProcessor: SummaryActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<SummaryIntent, SummaryViewState> {
    private val TAG = SummaryViewModel::class.simpleName

    private var initialViewState : SummaryViewState? = null

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: SingleLiveEvent<SummaryViewState> by lazy { SingleLiveEvent<SummaryViewState>() }

    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<SummaryIntent> by lazy { PublishSubject.create<SummaryIntent>() }

    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<SummaryViewState, SummaryResult, SummaryViewState> = BiFunction { previousState, result ->
        when (result) {
            is SummaryResult.FetchLikedStats -> return@BiFunction processStats(previousState, result)
            is SummaryResult.SaveTracks -> return@BiFunction processSaveResult(previousState, result)
            is SummaryResult.CreatePlaylistWithTracks -> return@BiFunction processCreatePlaylistResult(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    // secondary constructor to set initial
    constructor(actionProcessor: SummaryActionProcessor,
                schedulerProvider: BaseSchedulerProvider,
                initialState: SummaryViewState) : this(actionProcessor, schedulerProvider) {
        initialViewState = initialState.copy()
        liveViewState.value = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<SummaryViewState> = intentsSubject
                .filter { initialViewState != null }
                .filter { !initialViewState!!.allTracks.isEmpty() }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .map{ it -> actionFromIntent(it)}
                .filter { act -> act != SummaryAction.None }
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
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
    private fun actionFromIntent(intent: MviIntent) : SummaryAction {
        return when(intent) {
            is SummaryIntent.FetchStats -> SummaryAction.LoadLikedStats(intent.trackIds)
            is SummaryIntent.SaveAllTracks -> SummaryAction.SaveTracks(intent.tracks, intent.playlistId)
            is SummaryIntent.CreatePlaylistWithTracks -> SummaryAction.CreatePlaylistWithTracks(intent.ownerId, intent.name,
                    intent.description, intent.public, intent.tracks)
            else -> SummaryAction.None // no-op all other events
        }
    }

    // ===== Individual reducers ======

    private fun processStats(previousState: SummaryViewState, result: SummaryResult.FetchLikedStats) : SummaryViewState {
        val newState = previousState.copy()
        newState.error = null

        when (result.status) {
            MviResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            MviResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.stats = result.trackStats
            }
            MviResult.Status.FAILURE -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processSaveResult(previousState: SummaryViewState, result: SummaryResult.SaveTracks) : SummaryViewState {
        val newState = previousState.copy()
        newState.error = null

        when (result.status) {
            MviResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            MviResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                Utils.mLog(TAG, "processSaveResult SUCCESS", "insertedTracks: ",
                        "${result.insertedTracks!!.size} for playlist: ${result.playlistId}")
            }
            MviResult.Status.FAILURE -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processCreatePlaylistResult(previousState: SummaryViewState, result: SummaryResult.CreatePlaylistWithTracks) : SummaryViewState {
        val newState = previousState.copy()
        newState.error = null

        Utils.mLog(TAG, "processCreatePlaylistResult", "statis", result.status.toString(),
                "created with snapshotId: ", "${result.snapshotId?.snapshot_id} for new playlist: ${result.playlistId}")

        when (result.status) {
            MviResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            MviResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                "create playlist success! ${result.playlistId}".toast(App.appComponent.appContext(), Toast.LENGTH_SHORT)
            }
            MviResult.Status.FAILURE -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    // ===== MviViewModel =====

    override fun processIntents(intents: Observable<out SummaryIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<SummaryViewState> {
        return liveViewState
    }

    // ===== Lifecycle =====

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unSubscribeViewModel() {
        compositeDisposable.clear()

        // reset
        liveViewState.value = null
    }
}

data class SummaryViewState(var status: MviViewState.Status = MviViewState.Status.IDLE,
                            var error: Throwable? = null,
                            val allTracks: List<TrackModel> = listOf(),
                            var playlist: Playlist, // relevant playlist if coming from one
                            var stats: TrackListStats? = null // main model
) : MviViewState {
    val currentLikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.LIKED }.toMutableList()

    val currentDislikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.DISLIKED }.toMutableList()

    val unseen: MutableList<TrackModel>
        get() = (allTracks - (currentLikes + currentDislikes)).toMutableList()

    // fetch stats of likes
    fun trackIdsForStats() : List<String> {
        return currentLikes.map { it.id }
    }

    fun copy() : SummaryViewState {
        return SummaryViewState(this.status, this.error, this.allTracks.toList(), this.playlist, this.stats)
    }

    override fun toString(): String {
        return "allTracks: ${allTracks.size} -- playlist: ${playlist.id} -- status: ${status}"
    }

    companion object {
        fun create(state: TrackViewState) : SummaryViewState {
            return SummaryViewState(
                    allTracks = state.allTracks,
                    playlist = state.playlist!! // we require a playlist here
            )
        }
    }
}