package com.cziyeli.soundbits.cards.summary

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.widget.Toast
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.commons.toast
import com.cziyeli.data.Repository
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.*
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.soundbits.cards.TrackViewState
import com.cziyeli.soundbits.di.App
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * ViewModel for a [SummaryLayout] widget.
 * (Note this is a [MviViewModel], not a real arch components [ViewModel]).
 *
 * Shown after you finish swiping all the cards.
 *
 * Created by connieli on 1/6/18.
 */
class SummaryViewModel constructor(
        val actionProcessor: SummaryActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : LifecycleObserver, MviViewModel<SummaryIntent, SummaryViewState, SummaryResultMarker> {
    private val TAG = SummaryViewModel::class.simpleName

    // intents stream
    private val intentsSubject : PublishRelay<SummaryIntent> by lazy { PublishRelay.create<SummaryIntent>() }
    // Simple already-processed events stream
    private val resultsSubject : PublishRelay<SummaryResultMarker> by lazy { PublishRelay.create<SummaryResultMarker>() }
    private val viewStates: PublishRelay<SummaryViewState> by lazy { PublishRelay.create<SummaryViewState>() }
    internal lateinit var currentViewState: SummaryViewState
    private val compositeDisposable = CompositeDisposable()

    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<SummaryViewState, SummaryResultMarker, SummaryViewState> = BiFunction { previousState, result ->
        when (result) {
            is SummaryResult.FetchLikedStats -> return@BiFunction processLikedStats(previousState, result)
            is SummaryResult.FetchDislikedStats -> return@BiFunction processDislikedStats(previousState, result)
            is SummaryResult.SaveTracks -> return@BiFunction processSaveResult(previousState, result)
            is SummaryResult.CreatePlaylistWithTracks -> return@BiFunction processCreatePlaylistResult(previousState, result)
            is SummaryResult.SetTracks -> return@BiFunction processSetTracks(previousState, result)
            is SummaryResult.PlaylistCreated -> return@BiFunction processPlaylistCreated(previousState, result)
            is SummaryResult.ChangeTrackPref -> return@BiFunction processChangeTrackPref(previousState, result)
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
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<SummaryActionMarker>())
                .compose(actionProcessor.combinedProcessor)
                .mergeWith(resultsSubject) // <--- pipe in direct results
                .compose(resultFilter<SummaryResultMarker>())
                .observeOn(schedulerProvider.ui())
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .scan(currentViewState, reducer)

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
            is SummaryIntent.FetchFullStats -> {
                when {
                    intent.pref == Repository.Pref.LIKED && currentViewState.currentLikes.isNotEmpty() ->
                        StatsAction.FetchFullStats(currentViewState.currentLikes, intent.pref)
                    intent.pref == Repository.Pref.DISLIKED && currentViewState.currentDislikes.isNotEmpty() ->
                        StatsAction.FetchFullStats(currentViewState.currentDislikes, intent.pref)
                    else -> None
                }
            }
            is SummaryIntent.SaveAllTracks -> SummaryAction.SaveTracks(intent.tracks, intent.playlistId)
            is SummaryIntent.CreatePlaylistWithTracks -> SummaryAction.CreatePlaylistWithTracks(intent.ownerId, intent.name,
                    intent.description, intent.public, intent.tracks)
            else -> None // no-op all other events
        }
    }

    // ===== Individual reducers ======

    private fun processSetTracks(previousState: SummaryViewState, result: SummaryResult.SetTracks) : SummaryViewState {
        val status = when (result.status) {
            MviResult.Status.LOADING -> MviViewState.Status.LOADING
            MviResult.Status.SUCCESS -> MviViewState.Status.SUCCESS
            else -> MviViewState.Status.ERROR
        }
        return previousState.copy(lastResult = result, status = status)
    }

    private fun processPlaylistCreated(previousState: SummaryViewState, result: SummaryResult.PlaylistCreated) : SummaryViewState {
        val status = when (result.status) {
            MviResult.Status.LOADING -> MviViewState.Status.LOADING
            MviResult.Status.SUCCESS -> MviViewState.Status.SUCCESS
            else -> MviViewState.Status.ERROR
        }
        return previousState.copy(lastResult = result, status = status)
    }

    // Viewmodel only - this does NOT save in database.
    private fun processChangeTrackPref(previousState: SummaryViewState, result: SummaryResult.ChangeTrackPref) : SummaryViewState {
        val status = when (result.status) {
            MviResult.Status.LOADING -> MviViewState.Status.LOADING
            MviResult.Status.SUCCESS -> MviViewState.Status.SUCCESS
            else -> MviViewState.Status.ERROR
        }
        val changedTrackIndex = previousState.allTracks.indexOfLast { it.id == result.track.id }
        val newTracks = previousState.allTracks.toMutableList()
        newTracks[changedTrackIndex] = result.track
        return previousState.copy(
                lastResult = result,
                status = status,
                allTracks = newTracks
        )
    }

    private fun processLikedStats(previousState: SummaryViewState, result: SummaryResult.FetchLikedStats) : SummaryViewState {
        Utils.mLog(TAG, "processLikedStats! ${result.pref} -- ${result.status}")

        return when {
            (result.status == MviResult.Status.LOADING || result.status == MviResult.Status.ERROR) -> {
                val status = if (result.status == MviResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        status = status,
                        lastResult = result
                )
            }
            result.pref == Repository.Pref.LIKED && result.status == MviResult.Status.SUCCESS -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.SUCCESS,
                        lastResult = result,
                        likedStats = result.trackStats
                )
            }
            else -> previousState
        }
    }

    private fun processDislikedStats(previousState: SummaryViewState, result: SummaryResult.FetchDislikedStats) : SummaryViewState {
        Utils.mLog(TAG, "processDislikedStats! ${result.pref} -- ${result.status}")

        return when {
            (result.status == MviResult.Status.LOADING || result.status == MviResult.Status.ERROR) -> {
                val status = if (result.status == MviResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        status = status,
                        lastResult = result
                )
            }
            result.pref == Repository.Pref.DISLIKED && result.status == MviResult.Status.SUCCESS -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.SUCCESS,
                        lastResult = result,
                        dislikedStats = result.trackStats
                )
            }
            else -> previousState
        }
    }

    private fun processSaveResult(previousState: SummaryViewState, result: SummaryResult.SaveTracks) : SummaryViewState {
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                val status = if (result.status == MviResult.Status.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        status = status,
                        lastResult = result
                )
            }
            MviResult.Status.SUCCESS -> {
                Utils.mLog(TAG, "processSaveResult SUCCESS", "insertedTracks: ",
                        "${result.insertedTracks!!.size} for playlist: ${result.playlistId}")
                previousState.copy(
                        error = result.error,
                        status =  MviViewState.Status.SUCCESS,
                        lastResult = result
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
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun processSimpleResults(results: Observable<out SummaryResultMarker>) {
        compositeDisposable.add(
                results.subscribe(resultsSubject::accept)
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
                            val lastResult: MviResult,
                            val allTracks: List<TrackModel> = listOf(),
                            val playlist: Playlist? = null, // relevant playlist if coming from one
                            val likedStats: TrackListStats? = null,
                            val dislikedStats: TrackListStats? = null
) : MviViewState {
    val currentLikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.LIKED }.toMutableList()

    val currentDislikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.DISLIKED }.toMutableList()

    val unseen: MutableList<TrackModel>
        get() = (allTracks - (currentLikes + currentDislikes)).toMutableList()

    override fun toString(): String {
        return "lastResult: ${lastResult} -- status: ${status} -- allTracks: ${allTracks.size} -- likedStats: $likedStats -- disliked: " +
                "$dislikedStats"
    }

    companion object {
        fun create(state: TrackViewState) : SummaryViewState {
            return SummaryViewState(
                    allTracks = state.allTracks,
                    lastResult = NoResult(),
                    playlist = state.playlist
            )
        }
    }
}