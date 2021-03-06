package com.cziyeli.soundbits.stash

import android.arch.lifecycle.LifecycleObserver
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResult
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.stash.SimpleCardResult
import com.cziyeli.domain.summary.*
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.soundbits.cards.CardsIntent
import com.cziyeli.soundbits.cards.summary.SummaryIntent
import com.cziyeli.soundbits.playlistcard.CardIntentMarker
import com.cziyeli.soundbits.playlistcard.StatsIntent
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * ViewModel for a [SimpleCardWidget].
 * (Note this is a [MviViewModel], not a real arch components view model).
 *
 * Simple cards represent groups of tracks not necessarily tied to a playlist, but looks similar to a [PlaylistCard].
 */
class SimpleCardViewModel constructor(
        val actionProcessor: SimpleCardActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : LifecycleObserver, MviViewModel<CardIntentMarker, SimpleCardViewModel.ViewState, CardResultMarker> {
    private val TAG = SimpleCardViewModel::class.java.simpleName
    private val compositeDisposable = CompositeDisposable()

    // Listener for own events
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    // Listener for simple already-processed events stream
    private val resultsSubject : PublishRelay<CardResultMarker> by lazy { PublishRelay.create<CardResultMarker>() }
    // Publisher for own view states
    private val viewStates: PublishRelay<SimpleCardViewModel.ViewState> by lazy {
        PublishRelay.create<SimpleCardViewModel.ViewState>() }
    lateinit var currentViewState: SimpleCardViewModel.ViewState

    val unswipedTracks: List<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.UNSEEN }

    // All tracks in this card, including swiped
    val allTracks: List<TrackModel>
        get() = currentViewState.tracks

    val inCreateMode: Boolean
        get() = currentViewState.inCreateMode

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<ViewState, CardResultMarker, ViewState> = BiFunction { previousState, result ->
        when (result) {
            is CardResult.HeaderSet -> return@BiFunction processSetHeaderUrl(previousState, result)
            is CardResult.TracksSet -> return@BiFunction processSetTracks(previousState, result)
            is SimpleCardResult.SetCreateMode -> return@BiFunction processSetCreateMode(previousState, result)
            is StatsResult.FetchFullStats -> return@BiFunction processFetchFullStats(previousState, result)
            is TrackResult.ChangePrefResult -> return@BiFunction processTrackChangePref(previousState, result)
            is SummaryResult.CreatePlaylistWithTracks -> return@BiFunction processCreatePlaylistResult(previousState, result)
            is TrackResult.CommandPlayerResult -> return@BiFunction processPlayerCommand(previousState, result)
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
                .compose(resultFilter<CardResultMarker>())
                .observeOn(schedulerProvider.ui())
                .scan(currentViewState, reducer)

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
            is CardsIntent.CommandPlayer -> TrackAction.CommandPlayer.create(
                    intent.command, intent.track
            )
            else -> None // no-op all other events
        }
    }

    override fun processSimpleResults(results: Observable<out CardResultMarker>) {
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
        return previousState.copy(carouselHeaderUrl = result.headerImageUrl, lastResult = result)
    }

    private fun processSetTracks(previousState: ViewState,
                                 result: CardResult.TracksSet
    ) : ViewState {
        return previousState.copy(
                status = MviViewState.Status.SUCCESS,
                tracks = result.tracks.toMutableList(),
                lastResult = result
        )
    }

    private fun processSetCreateMode(previousState: ViewState,
                                     result: SimpleCardResult.SetCreateMode
    ) : ViewState {
        return previousState.copy(
                status = MviViewState.Status.SUCCESS,
                inCreateMode = result.inCreateMode,
                lastResult = result
        )
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

    private fun processPlayerCommand(previousState: ViewState, result: TrackResult.CommandPlayerResult) : ViewState {
        return when (result.status) {
            MviResult.Status.LOADING, MviResult.Status.SUCCESS, MviResult.Status.ERROR -> {
                val status = when (result.status) {
                    MviResult.Status.LOADING -> MviViewState.Status.LOADING
                    MviResult.Status.SUCCESS -> MviViewState.Status.SUCCESS
                    MviResult.Status.ERROR -> MviViewState.Status.ERROR
                    else -> MviViewState.Status.IDLE
                }
                previousState.copy(
                        error = result.error,
                        currentTrack = result.currentTrack,
                        currentPlayerState = result.currentPlayerState,
                        status = status
                )
            }
            else -> previousState
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
                         val tracks: MutableList<TrackModel> = mutableListOf(), // either local or
                         val trackStats: TrackListStats? = null, // stats for ALL tracks
                         val inCreateMode: Boolean = false, // whether in 'create' state
                         val currentTrack: TrackModel? = null, // current track playing, if any
                         val currentPlayerState: PlayerInterface.State = PlayerInterface.State.INVALID,
                         val shouldRemoveTrack: (state: ViewState, track: TrackModel?) -> Boolean
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
