package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.widget.Toast
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.toast
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.*
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.cards.TrackViewState
import com.cziyeli.songbits.di.App
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * Not a real arch components view model.
 *
 * Shown after you finish surfing all the cards.
 *
 * Created by connieli on 1/6/18.
 */
class SummaryViewModel constructor(
        val actionProcessor: SummaryActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : LifecycleObserver, MviViewModel<SummaryIntent, SummaryViewState> {
    private val TAG = SummaryViewModel::class.simpleName

    private val compositeDisposable = CompositeDisposable()

    // intents stream, viewstates stream
    private val intentsSubject : PublishSubject<SummaryIntent> by lazy { PublishSubject.create<SummaryIntent>() }
    private val viewStates: PublishRelay<SummaryViewState> by lazy { PublishRelay.create<SummaryViewState>() }
    lateinit var currentViewState: SummaryViewState

    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<SummaryViewState, SummaryResultMarker, SummaryViewState> = BiFunction { previousState, result ->
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
        currentViewState = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<SummaryViewState> = intentsSubject
                .filter { initialState.allTracks.isNotEmpty() }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<SummaryActionMarker>())
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .compose(actionProcessor.combinedProcessor)
                .scan(currentViewState, reducer)
                .replay(1)
                .autoConnect()

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
        return when(intent) {
            is SummaryIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            is SummaryIntent.SaveAllTracks -> SummaryAction.SaveTracks(intent.tracks, intent.playlistId)
            is SummaryIntent.CreatePlaylistWithTracks -> SummaryAction.CreatePlaylistWithTracks(intent.ownerId, intent.name,
                    intent.description, intent.public, intent.tracks)
            else -> None // no-op all other events
        }
    }

    // ===== Individual reducers ======

    private fun processStats(previousState: SummaryViewState, result: SummaryResult.FetchLikedStats) : SummaryViewState {
        Utils.mLog(TAG, "processStats! ${result.status}")

        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                val status = if (result.status == MviResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        status = status
                )
            }
            MviResult.Status.SUCCESS -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.SUCCESS,
                        stats = result.trackStats
                )
            } else -> previousState
        }
    }

    private fun processSaveResult(previousState: SummaryViewState, result: SummaryResult.SaveTracks) : SummaryViewState {
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                val status = if (result.status == MviResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        status = status
                )
            }
            MviResult.Status.SUCCESS -> {
                Utils.mLog(TAG, "processSaveResult SUCCESS", "insertedTracks: ",
                        "${result.insertedTracks!!.size} for playlist: ${result.playlistId}")
                previousState.copy(
                        error = result.error,
                        status =  MviViewState.Status.SUCCESS
                )
            } else -> previousState
        }
    }

    private fun processCreatePlaylistResult(previousState: SummaryViewState, result: SummaryResult.CreatePlaylistWithTracks) : SummaryViewState {
        Utils.mLog(TAG, "processCreatePlaylistResult", "status", result.status.toString(),
                "created with snapshotId: ", "${result.snapshotId?.snapshot_id} for new playlist: ${result.playlistId}")

        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                val status = if (result.status == MviResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        status = status
                )
            }
            MviResult.Status.SUCCESS -> {
                "create playlist success! ${result.playlistId}".toast(App.appComponent.appContext(), Toast.LENGTH_SHORT)
                previousState.copy(
                        error = result.error,
                        status =  MviViewState.Status.SUCCESS
                )
            } else -> previousState
        }
    }

    // ===== MviViewModel =====

    override fun processIntents(intents: Observable<out SummaryIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): Observable<SummaryViewState> {
        return viewStates
    }


    // ===== Lifecycle =====

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unSubscribeViewModel() {
        compositeDisposable.clear()
    }
}

data class SummaryViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                            val error: Throwable? = null,
                            val allTracks: List<TrackModel> = listOf(),
                            val playlist: Playlist, // relevant playlist if coming from one
                            val stats: TrackListStats? = null // main model
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