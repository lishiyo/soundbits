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
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.cards.TrackIntent
import com.cziyeli.songbits.cards.TrackViewState
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
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
) : ViewModel(), LifecycleObserver, MviViewModel<CardIntentMarker, PlaylistCardViewModel.PlaylistCardViewState> {
    private val TAG = PlaylistCardViewModel::class.simpleName

    private var initialViewState : PlaylistCardViewState
    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<PlaylistCardViewState> by lazy { MutableLiveData<PlaylistCardViewState>() }
    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<CardIntentMarker> by lazy { PublishSubject.create<CardIntentMarker>() }

    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<PlaylistCardViewState, CardResultMarker, PlaylistCardViewState> = BiFunction {
        previousState, result ->
            when (result) {
                is CardResult.CalculateQuickCounts -> return@BiFunction processQuickCounts(previousState, result)
                is PlaylistCardResult.FetchPlaylistTracks -> return@BiFunction processTracksFetched(previousState, result)
                is StatsResult.FetchStats -> return@BiFunction processFetchStats(previousState, result)
                is StatsResult.FetchAllTracksWithStats -> return@BiFunction processFetchAllTracksWithStats(previousState, result)
                is TrackResult.ChangePrefResult -> return@BiFunction processTrackChangePref(previousState, result)
                else -> return@BiFunction previousState
            }
    }
    private val intentFilter: ObservableTransformer<CardIntentMarker, CardIntentMarker> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<CardIntentMarker>(
                    // initial fetch (only if empty) - note this is triggered by Room afterwards
                    shared.ofType(PlaylistCardIntent.FetchSwipedTracks::class.java)
                            .filter{ liveViewState.value?.allTracksList?.isEmpty() == true },
                    shared.ofType(StatsIntent.FetchStats::class.java).take(1), // only hit once since we don't hit remote
                    shared.filter({ intent ->
                        intent !is PlaylistCardIntent.FetchSwipedTracks && intent !is StatsIntent.FetchStats})
            )
        }
    }

    private val compositeDisposable = CompositeDisposable()

    // Publish events into the stream from VM, in background
    // These are NOT coming from UI events (view-driven), these are programmatic
    private val programmaticEventsPublisher = PublishSubject.create<CardIntentMarker>()

    val playlist: Playlist?
        get() = liveViewState.value?.playlist
    // All swipeable tracks we haven't swiped yet
    val tracksToSwipe: List<TrackModel>?
        get() = liveViewState.value?.unswipedTracks
    // The swiped ones we liked
    val tracksToCreate: List<TrackModel>?
        get() = liveViewState.value?.tracksToCreate

    // secondary constructor to set initial playlist model
    init {
        initialViewState = initialState.copy()
        liveViewState.value = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<PlaylistCardViewState> = intentsSubject // View-driven events
                .mergeWith(programmaticEventsPublisher) // programmatic events for ViewModel to call events
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .filter { act -> act != PlaylistCardAction.None }
                .doOnNext { intent -> Utils.mLog(TAG, "intentsSubject", "hitActionProcessor", intent.javaClass.name) }
                .compose(actionProcessor.combinedProcessor)
                .observeOn(schedulerProvider.ui())
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
    private fun actionFromIntent(intent: MviIntent) : CardActionMarker {
        return when (intent) {
            is PlaylistCardIntent.CalculateQuickCounts -> CardAction.CalculateQuickCounts(intent.tracks)
            is PlaylistCardIntent.FetchSwipedTracks -> PlaylistCardAction.FetchPlaylistTracks(intent.ownerId, intent.playlistId, intent.onlySwiped)
            is StatsIntent.FetchTracksWithStats -> StatsAction.FetchAllTracksWithStats(intent.playlist.owner.id, intent.playlist.id)
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            is TrackIntent.ChangeTrackPref -> TrackAction.ChangeTrackPref(intent.track, intent.pref)
            else -> PlaylistCardAction.None
        }
    }

    // ===== MviViewModel =====

    override fun processIntents(intents: Observable<out CardIntentMarker>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<PlaylistCardViewState> {
        return liveViewState
    }

    // ===== Individual reducers ======

    private fun processQuickCounts(previousState: PlaylistCardViewState, result: CardResult.CalculateQuickCounts) : PlaylistCardViewState {
        val newState = previousState.copy(lastResult = result)

        when (result.status) {
            CardResult.CalculateQuickCounts.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            CardResult.CalculateQuickCounts.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.likedCount = result.likedCount
                newState.dislikedCount = result.dislikedCount
            }
            CardResult.CalculateQuickCounts.Status.ERROR -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    // process remote (all tracks) with stats
    private fun processFetchAllTracksWithStats(
            previousState: PlaylistCardViewState,
            result: StatsResult.FetchAllTracksWithStats
    ) : PlaylistCardViewState {
        val newState = previousState.copy(lastResult = result)

        when (result.status) {
            StatsResultStatus.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            StatsResultStatus.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.allTracksList = result.tracks
                newState.trackStats = result.trackStats
            }
            StatsResultStatus.ERROR -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    // might be either local or remote
    private fun processTracksFetched(
            previousState: PlaylistCardViewState,
            result: PlaylistCardResult.FetchPlaylistTracks
    ) : PlaylistCardViewState {
        val newState = previousState.copy(lastResult = result)

        when (result.status) {
            PlaylistCardResult.FetchPlaylistTracks.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                if (result.fromLocal) {
                    newState.stashedTracksList = result.items.toMutableList()
                    // now calculate the counts!
                    programmaticEventsPublisher.onNext(PlaylistCardIntent.CalculateQuickCounts(newState.stashedTracksList))
                } else {
                    newState.allTracksList = result.items
                }
            }
            PlaylistCardResult.FetchPlaylistTracks.Status.ERROR -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processFetchStats(
            previousState: PlaylistCardViewState,
            result: StatsResult.FetchStats
    ) : PlaylistCardViewState {
        val newState = previousState.copy(lastResult = result)

        when (result.status) {
            StatsResultStatus.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            StatsResultStatus.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.trackStats = result.trackStats
            }
            StatsResultStatus.ERROR -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processTrackChangePref(previousState: PlaylistCardViewState, result: TrackResult.ChangePrefResult) : PlaylistCardViewState {
        val newState = previousState.copy(lastResult = result)

        when (result.status) {
            TrackResult.ChangePrefResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            TrackResult.ChangePrefResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                Utils.mLog(TAG, "change track pref! ${result.currentTrack?.name}: ${result.currentTrack?.pref}")
            }
            TrackResult.ChangePrefResult.Status.ERROR -> {
                Utils.mLog(TAG, "change track pref ERROR! ${result.error} ${result.currentTrack?.name}: ${result.currentTrack?.pref}")
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    // ======= VIEWSTATE =======

    data class PlaylistCardViewState(var status: MviViewState.StatusInterface = MviViewState.Status.IDLE,
                                     var error: Throwable? = null,
                                     var playlist: Playlist, // card's playlist - has total count
                                     var likedCount: Int = 0,
                                     var dislikedCount: Int = 0,
                                     var stashedTracksList: MutableList<TrackModel> = mutableListOf(), // swiped tracks in db
                                     var allTracksList: List<TrackModel> = listOf(), // all tracks (from remote)
                                     var trackStats: TrackListStats? = null, // stats for ALL tracks
                                     var lastResult: CardResultMarker? = null // track the triggering 'props'
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

        // make sure the tracks are there!
        fun copy() : PlaylistCardViewState {
            // on every copy, remove the error and flip notify back to false
            return PlaylistCardViewState(status, null, playlist, likedCount, dislikedCount, stashedTracksList, allTracksList,
                    trackStats, lastResult)
        }

        companion object {
            fun create(state: TrackViewState) : PlaylistCardViewState {
                return PlaylistCardViewState(
                        playlist = state.playlist // we require a playlist here
                )
            }
        }
    }
}