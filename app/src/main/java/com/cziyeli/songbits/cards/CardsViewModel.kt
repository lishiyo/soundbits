package com.cziyeli.songbits.cards

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.SummaryAction
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.summary.SwipeActionMarker
import com.cziyeli.domain.summary.SwipeResultMarker
import com.cziyeli.domain.tracks.CardsActionProcessor
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.cards.summary.SummaryIntent
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject



/**
 * Created by connieli on 1/1/18.
 */
class CardsViewModel @Inject constructor(
        private val repository: RepositoryImpl,
        val actionProcessor: CardsActionProcessor,
        val schedulerProvider: BaseSchedulerProvider,
        val playlist: Playlist?
): ViewModel(), LifecycleObserver, MviViewModel<CardIntentMarker, TrackViewState, CardResultMarker> {
    private val TAG = CardsViewModel::class.simpleName

    private val compositeDisposable = CompositeDisposable()

    // Intents stream and ViewStates stream
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    private val viewStates: PublishRelay<TrackViewState> by lazy { PublishRelay.create<TrackViewState>() }
    var currentViewState: TrackViewState

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<TrackViewState, SwipeResultMarker, TrackViewState> = BiFunction { previousState, result ->
        when (result) {
            is TrackResult.LoadTrackCards -> return@BiFunction processTrackCards(previousState, result)
            is TrackResult.CommandPlayerResult -> return@BiFunction processPlayerCommand(previousState, result)
            is TrackResult.ChangePrefResult -> return@BiFunction processTrackChangePref(previousState, result)
            is SummaryResult.SaveTracks -> return@BiFunction processSaveResult(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        currentViewState = TrackViewState(playlist = playlist)

        // create observable to push into states live data
        val observable: Observable<TrackViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<SwipeActionMarker>())
                .compose(actionProcessor.combinedProcessor)
                .compose(resultFilter<SwipeResultMarker>())
                .observeOn(schedulerProvider.ui())
                .scan(currentViewState, reducer)

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    currentViewState = viewState
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.log(TAG, "ViewModel ++ ERROR " + err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : MviAction {
        return when(intent) {
            is CardsIntent.ScreenOpenedWithTracks -> TrackAction.SetTracks(intent.playlist, intent.tracks)
            is CardsIntent.ScreenOpenedNoTracks -> TrackAction.LoadTrackCards.create(
                    intent.ownerId, intent.playlistId, intent.onlyTrackIds, intent.fields, intent.limit, intent.offset)
            is CardsIntent.CommandPlayer -> TrackAction.CommandPlayer.create(intent.command, intent.track)
            is CardsIntent.ChangeTrackPref -> TrackAction.ChangeTrackPref.create(intent.track, intent.pref)
            is SummaryIntent.SaveAllTracks -> SummaryAction.SaveTracks(intent.tracks, intent.playlistId)
            else -> None // no-op all other events
        }
    }

    override fun processIntents(intents: Observable<out CardIntentMarker>) {
        compositeDisposable.add(
            intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<TrackViewState> {
        return viewStates
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unSubscribeViewModel() {
        // clear out repo subscriptions as well
        for (disposable in repository.allCompositeDisposable) {
            compositeDisposable.addAll(disposable)
        }
        compositeDisposable.clear()
    }

    // ===== Individual reducers ======

    private fun processTrackCards(previousState: TrackViewState, result: TrackResult.LoadTrackCards): TrackViewState {
        return when (result.status) {
            TrackResult.LoadTrackCards.Status.LOADING,
            TrackResult.LoadTrackCards.Status.ERROR -> {
                val status = if (result.status == TrackResult.LoadTrackCards.Status.LOADING)
                    TrackViewState.TracksLoadedStatus.LOADING else TrackViewState.TracksLoadedStatus.ERROR
                previousState.copy(
                        error = result.error,
                        status = status
                )
            }
            TrackResult.LoadTrackCards.Status.SUCCESS -> {
                val newTracks = previousState.allTracks
                newTracks.addAll(result.items.filter { it.isSwipeable })
                previousState.copy(
                        error = result.error,
                        status = TrackViewState.TracksLoadedStatus.SUCCESS,
                        allTracks = newTracks
                )
            } else -> previousState
        }
    }

    private fun processPlayerCommand(previousState: TrackViewState, result: TrackResult.CommandPlayerResult) : TrackViewState {
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.SUCCESS, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        currentTrack = result.currentTrack,
                        currentPlayerState = result.currentPlayerState,
                        status = Utils.statusFromResult(result.status)
                )
            }
            else -> previousState
        }
    }

    private fun processTrackChangePref(previousState: TrackViewState, result: TrackResult.ChangePrefResult) : TrackViewState {
        return when (result.status) {
            TrackResult.ChangePrefResult.Status.SUCCESS -> {
                val newTracks = previousState.allTracks.toMutableList()
                val track = newTracks.find { el -> el.id == result.currentTrack?.id } // find the track in the list that matches the
                // changed track
                track!!.pref = result.pref!!

                previousState.copy(
                        error = result.error,
                        currentTrack = result.currentTrack,
                        status = MviViewState.Status.SUCCESS,
                        allTracks = newTracks
                )
            }
            TrackResult.ChangePrefResult.Status.LOADING, TrackResult.ChangePrefResult.Status.ERROR -> {
                val status = if (result.status == TrackResult.ChangePrefResult.Status.LOADING) {
                    MviViewState.Status.LOADING
                } else {
                    MviViewState.Status.ERROR
                }
                previousState.copy(
                        error = result.error,
                        currentTrack = result.currentTrack,
                        status = status
                )
            }
            else -> previousState
        }
    }

    // Save results before leaving
    private fun processSaveResult(
            previousState: TrackViewState,
            result: SummaryResult.SaveTracks
    ) : TrackViewState {
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        status = Utils.statusFromResult(result.status)
                )
            }
            MviResult.Status.SUCCESS -> {
                Utils.mLog(TAG, "processSaveResult SUCCESS", "insertedTracks: ",
                        "${result.insertedTracks!!.size} for playlist: ${result.playlistId}")
                previousState.copy(
                        error = result.error,
                        status =  MviViewState.Status.SUCCESS,
                        tracksSaved = true
                )
            } else -> previousState
        }
    }

}

data class TrackViewState(val status: MviViewState.StatusInterface = MviViewState.Status.IDLE,
                          val error: Throwable? = null,
                          val allTracks: MutableList<TrackModel> = mutableListOf(),
                          val playlist: Playlist? = null, // optional playlist
                          val currentTrack: TrackModel? = null,
                          val currentPlayerState: PlayerInterface.State = PlayerInterface.State.INVALID,
                          val tracksSaved: Boolean = false
) : MviViewState {
    enum class TracksLoadedStatus : MviViewState.StatusInterface {
        LOADING, SUCCESS, ERROR
    }

    val currentSwiped: List<TrackModel>
        get() = currentLikes + currentDislikes

    private val currentLikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.LIKED }.toMutableList()

    private val currentDislikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.DISLIKED }.toMutableList()

    private val unseen: MutableList<TrackModel>
        get() = (allTracks - (currentLikes + currentDislikes)).toMutableList()

    val reachedEnd: Boolean
        get() = status == MviViewState.Status.SUCCESS && unseen.size == 0 && allTracks.size > 0

    fun isSuccess(): Boolean {
        return status == TracksLoadedStatus.SUCCESS || status == MviViewState.Status.SUCCESS
    }

    fun isLoading(): Boolean {
        return status == TracksLoadedStatus.LOADING || status == MviViewState.Status.LOADING
    }

    fun isError(): Boolean {
        return status == TracksLoadedStatus.ERROR || status == MviViewState.Status.ERROR
    }

    override fun toString(): String {
        return "status: $status -- allTracks: ${allTracks.size} -- playlist: ${playlist?.id} -- currentTrack: ${currentTrack?.name} -- " +
                "$reachedEnd"
    }
}

