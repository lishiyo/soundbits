package com.cziyeli.songbits.playlistcard

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlistcard.*
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.cards.CardsIntent
import com.cziyeli.songbits.cards.TrackViewState
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject


class PlaylistCardViewModel @Inject constructor(
        val actionProcessor: PlaylistCardActionProcessor,
        val schedulerProvider: BaseSchedulerProvider,
        initialState: PlaylistCardViewState
) : ViewModel(), LifecycleObserver, MviViewModel<CardIntentMarker, PlaylistCardViewModel.PlaylistCardViewState, CardResultMarker> {
    private val TAG = PlaylistCardViewModel::class.simpleName

    // Events stream from view
    private val intentsSubject : PublishRelay<CardIntentMarker> by lazy { PublishRelay.create<CardIntentMarker>() }
    // Publish events into the stream from VM, in background
    // These are NOT coming from UI events, these are programmatic
    private val programmaticEventsPublisher = PublishRelay.create<CardIntentMarker>()

    // Subject to publish ViewStates
    private val viewStates: PublishRelay<PlaylistCardViewState> by lazy { PublishRelay.create<PlaylistCardViewState>() }
    var currentViewState: PlaylistCardViewState = initialState.copy()

    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<PlaylistCardViewState, CardResultMarker, PlaylistCardViewState> = BiFunction {
        previousState, result ->
            when (result) {
                is CardResult.CalculateQuickCounts -> return@BiFunction processQuickCounts(previousState, result)
                is PlaylistCardResult.FetchPlaylistTracks -> return@BiFunction processTracksFetched(previousState, result)
                is StatsResult.FetchStats -> return@BiFunction processFetchStats(previousState, result)
                is StatsResult.FetchAllTracksWithStats -> return@BiFunction processFetchAllTracksWithStats(previousState, result)
                is TrackResult.ChangePrefResult -> return@BiFunction processTrackChangePref(previousState, result)
                is TrackResult.CommandPlayerResult -> return@BiFunction processPlayerCommand(previousState, result)
                else -> return@BiFunction previousState
            }
    }
    private val intentFilter: ObservableTransformer<CardIntentMarker, CardIntentMarker> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<CardIntentMarker>(
                    // initial fetch (only if empty) - note this is triggered by Room afterwards
                    shared.ofType(PlaylistCardIntent.FetchSwipedTracks::class.java)
                            .filter{ currentViewState.allTracksList.isEmpty() },
                    shared.ofType(StatsIntent.FetchStats::class.java).take(1), // only hit once since we don't hit remote
                    shared.filter({ intent ->
                        intent !is PlaylistCardIntent.FetchSwipedTracks && intent !is StatsIntent.FetchStats})
            )
        }
    }

    private val compositeDisposable = CompositeDisposable()

    val playlist: Playlist?
        get() = currentViewState.playlist
    // All swipeable tracks we haven't swiped yet
    val tracksToSwipe: List<TrackModel>?
        get() = currentViewState.unswipedTracks
    // The swiped ones we liked
    val tracksToCreate: List<TrackModel>?
        get() = currentViewState.tracksToCreate

    // secondary constructor to set initial playlist model
    init {
        // create observable to push into states live data
        val observable: Observable<PlaylistCardViewState> = intentsSubject // View-driven events
                .mergeWith(programmaticEventsPublisher) // programmatic events for ViewModel to call events
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<CardActionMarker>())
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .compose(actionProcessor.combinedProcessor)
                .compose(resultFilter<CardResultMarker>())
                .observeOn(schedulerProvider.ui())
                .scan(currentViewState, reducer) // final reduction

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
            is PlaylistCardIntent.CalculateQuickCounts -> CardAction.CalculateQuickCounts(intent.tracks)
            is PlaylistCardIntent.FetchSwipedTracks -> PlaylistCardAction.FetchPlaylistTracks(intent.ownerId, intent.playlistId, intent.onlySwiped)
            is StatsIntent.FetchTracksWithStats -> StatsAction.FetchAllTracksWithStats(intent.playlist.owner.id, intent.playlist.id)
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            is CardsIntent.ChangeTrackPref -> TrackAction.ChangeTrackPref(intent.track, intent.pref)
            is CardsIntent.CommandPlayer -> TrackAction.CommandPlayer.create(
                    intent.command, intent.track
            )
            else -> None
        }
    }

    // ===== MviViewModel =====

    override fun processIntents(intents: Observable<out CardIntentMarker>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<PlaylistCardViewState> {
        return viewStates
    }

    // ===== Individual reducers ======

    private fun processQuickCounts(previousState: PlaylistCardViewState, result: CardResult.CalculateQuickCounts) : PlaylistCardViewState {
        return when (result.status) {
            CardResult.CalculateQuickCounts.Status.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            CardResult.CalculateQuickCounts.Status.SUCCESS -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        likedCount = result.likedCount,
                        dislikedCount = result.dislikedCount
                )
            }
            CardResult.CalculateQuickCounts.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR,
                        likedCount = result.likedCount,
                        dislikedCount = result.dislikedCount
                )
            }
            else -> previousState
        }
    }

    // process remote (all tracks) with stats
    private fun processFetchAllTracksWithStats(
            previousState: PlaylistCardViewState,
            result: StatsResult.FetchAllTracksWithStats
    ) : PlaylistCardViewState {
        return when (result.status) {
            StatsResultStatus.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            StatsResultStatus.SUCCESS -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        allTracksList = result.tracks,
                        trackStats = result.trackStats
                )
            }
            StatsResultStatus.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR
                )
            }
            else -> previousState
        }
    }

    // might be either local or remote
    private fun processTracksFetched(
            previousState: PlaylistCardViewState,
            result: PlaylistCardResult.FetchPlaylistTracks
    ) : PlaylistCardViewState {
        return when (result.status) {
            PlaylistCardResult.FetchPlaylistTracks.Status.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS -> {
                if (result.fromLocal) {
                    // now calculate the counts!
                    programmaticEventsPublisher.accept(PlaylistCardIntent.CalculateQuickCounts(result.items.toMutableList()))
                    previousState.copy(
                            error = null,
                            lastResult = result,
                            status = MviViewState.Status.SUCCESS,
                            stashedTracksList = result.items.toMutableList()
                    )
                } else {
                    previousState.copy(
                            error = null,
                            lastResult = result,
                            status = MviViewState.Status.SUCCESS,
                            allTracksList = result.items
                    )
                }

            }
            PlaylistCardResult.FetchPlaylistTracks.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR
                )
            }
            else -> previousState
        }
    }

    // stats
    private fun processFetchStats(
            previousState: PlaylistCardViewState,
            result: StatsResult.FetchStats
    ) : PlaylistCardViewState {
        return when (result.status) {
            StatsResultStatus.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            StatsResultStatus.SUCCESS -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        trackStats = result.trackStats
                )
            }
            StatsResultStatus.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR
                )
            }
            else -> previousState
        }
    }

    private fun processPlayerCommand(previousState: PlaylistCardViewState, result: TrackResult.CommandPlayerResult) : PlaylistCardViewState {
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

    private fun processTrackChangePref(previousState: PlaylistCardViewState, result: TrackResult.ChangePrefResult) : PlaylistCardViewState {
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

    // ======= VIEWSTATE =======

    data class PlaylistCardViewState(val status: MviViewState.StatusInterface = MviViewState.Status.IDLE,
                                     val error: Throwable? = null,
                                     val playlist: Playlist, // card's playlist - has total count
                                     val likedCount: Int = 0,
                                     val dislikedCount: Int = 0,
                                     val stashedTracksList: MutableList<TrackModel> = mutableListOf(), // swiped tracks in db
                                     val allTracksList: List<TrackModel> = listOf(), // all tracks (from remote)
                                     val trackStats: TrackListStats? = null, // stats for ALL tracks
                                     val lastResult: CardResultMarker? = null, // track the triggering 'props'
                                     val currentTrack: TrackModel? = null, // current track playing, if any
                                     val currentPlayerState: PlayerInterface.State = PlayerInterface.State.INVALID
    ) : MviViewState {
        // Creating playlist will
        val tracksToCreate: List<TrackModel>
            get() = stashedTracksList.filter { it.liked }

        val unswipedTracks: List<TrackModel>
            get() = allTracksList.filter { it.isSwipeable }.filter { !stashedTracksList.map { it.id }.contains(it.id) }

        val unswipedCount: Int
            get() =  unswipedTracks.size

        fun isSuccess(): Boolean {
            return status == MviViewState.Status.SUCCESS || status == CardResult.CalculateQuickCounts.Status.SUCCESS
                    || status == PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS
                    || status == StatsResultStatus.SUCCESS
        }

        fun isLoading(): Boolean {
            return status == MviViewState.Status.LOADING || status == CardResult.CalculateQuickCounts.Status.LOADING
                    || status == PlaylistCardResult.FetchPlaylistTracks.Status.LOADING
                    || status == StatsResultStatus.LOADING
        }

        fun isError(): Boolean {
            return status == MviViewState.Status.ERROR || status == CardResult.CalculateQuickCounts.Status.ERROR
                    || status == PlaylistCardResult.FetchPlaylistTracks.Status.ERROR
                    || status == StatsResultStatus.ERROR
        }

        companion object {
            fun create(state: TrackViewState) : PlaylistCardViewState {
                return PlaylistCardViewState(
                        playlist = state.playlist!! // we require a playlist here
                )
            }
        }
    }
}