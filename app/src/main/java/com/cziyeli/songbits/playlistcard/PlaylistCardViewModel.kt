package com.cziyeli.songbits.playlistcard

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
) : ViewModel(), LifecycleObserver, MviViewModel<SinglePlaylistIntent, PlaylistCardViewModel.PlaylistCardViewState> {
    private val TAG = PlaylistCardViewModel::class.simpleName

    private var initialViewState : PlaylistCardViewState
    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<PlaylistCardViewState> by lazy { MutableLiveData<PlaylistCardViewState>() }
    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<SinglePlaylistIntent> by lazy { PublishSubject.create<SinglePlaylistIntent>() }
    // reducer fn: Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<PlaylistCardViewState, PlaylistCardResultMarker, PlaylistCardViewState> = BiFunction {
        previousState, result ->
            when (result) {
                is PlaylistCardResult.CalculateQuickCounts -> return@BiFunction processQuickCounts(previousState, result)
                is PlaylistCardResult.FetchPlaylistTracks -> return@BiFunction processTracksFetched(previousState, result)
                is StatsResult.FetchStats -> return@BiFunction processFetchStats(previousState, result)
                is StatsResult.FetchAllTracksWithStats -> return@BiFunction processFetchAllTracksWithStats(previousState, result)
                else -> return@BiFunction previousState
            }
    }
    private val intentFilter: ObservableTransformer<SinglePlaylistIntent, SinglePlaylistIntent> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<SinglePlaylistIntent>(
                    shared.ofType(PlaylistCardIntent.FetchSwipedTracks::class.java)
                            .filter{ liveViewState.value?.allTracksList?.isEmpty() == true }, // only fetch if still empty
                    shared.ofType(StatsIntent.FetchStats::class.java).take(1), // only hit once
                    shared.filter({ intent ->
                        intent !is PlaylistCardIntent.FetchSwipedTracks && intent !is StatsIntent.FetchStats})
            )
        }
    }

    private val compositeDisposable = CompositeDisposable()

    // secondary constructor to set initial playlist model
    init {
        initialViewState = initialState.copy()
        liveViewState.value = initialState.copy()

        // create observable to push into states live data
        val observable: Observable<PlaylistCardViewState> = intentsSubject
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
            is PlaylistCardIntent.CalculateQuickCounts -> PlaylistCardAction.CalculateQuickCounts(intent.playlist, intent.tracks)
            is PlaylistCardIntent.FetchSwipedTracks -> PlaylistCardAction.FetchPlaylistTracks(
                    intent.ownerId, intent.playlistId)
            is StatsIntent.FetchTracksWithStats -> StatsAction.FetchAllTracksWithStats(intent.playlist.owner.id, intent.playlist.id)
            is StatsIntent.FetchStats -> StatsAction.FetchStats(intent.trackIds)
            else -> PlaylistCardAction.None
        }
    }

    // ===== MviViewModel =====

    override fun processIntents(intents: Observable<out SinglePlaylistIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<PlaylistCardViewState> {
        return liveViewState
    }

    // ===== Individual reducers ======

    private fun processQuickCounts(previousState: PlaylistCardViewState, result: PlaylistCardResult.CalculateQuickCounts) : PlaylistCardViewState {
        val newState = previousState.copy()
        newState.error = null

        Utils.mLog(TAG, "processQuickCounts", "status: ${result.status}")

        when (result.status) {
            PlaylistCardResult.CalculateQuickCounts.Status.LOADING -> {
                newState.status = PlaylistCardResult.CalculateQuickCounts.Status.LOADING
            }
            PlaylistCardResult.CalculateQuickCounts.Status.SUCCESS -> {
                newState.status = PlaylistCardResult.CalculateQuickCounts.Status.SUCCESS
                newState.likedCount = result.likedCount
                newState.dislikedCount = result.dislikedCount
            }
            PlaylistCardResult.CalculateQuickCounts.Status.ERROR -> {
                newState.status = PlaylistCardResult.CalculateQuickCounts.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processFetchAllTracksWithStats(
            previousState: PlaylistCardViewState,
            result: StatsResult.FetchAllTracksWithStats
    ) : PlaylistCardViewState {
        val newState = previousState.copy()
        newState.error = null

        Utils.mLog(TAG, "processFetchAllTracksWithStats", "status: ${result.status}")

        when (result.status) {
            StatsResultStatus.LOADING -> {
                newState.status = PlaylistCardResult.FetchPlaylistTracks.Status.LOADING
            }
            StatsResultStatus.SUCCESS -> {
                newState.status = PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS
                newState.allTracksList = result.tracks
                newState.trackStats = result.trackStats
            }
            StatsResultStatus.ERROR -> {
                newState.status = PlaylistCardResult.FetchPlaylistTracks.Status.ERROR
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
        val newState = previousState.copy()
        newState.error = null

        when (result.status) {
            PlaylistCardResult.FetchPlaylistTracks.Status.LOADING -> {
                newState.status = PlaylistCardResult.FetchPlaylistTracks.Status.LOADING
            }
            PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS -> {
                newState.status = PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS
                if (result.fromLocal) {
                    newState.stashedTracksList = result.items
                } else {
                    newState.allTracksList = result.items
                }
            }
            PlaylistCardResult.FetchPlaylistTracks.Status.ERROR -> {
                newState.status = PlaylistCardResult.FetchPlaylistTracks.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processFetchStats(
            previousState: PlaylistCardViewState,
            result: StatsResult.FetchStats
    ) : PlaylistCardViewState {
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

    // ======= VIEWSTATE =======

    data class PlaylistCardViewState(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                                     var error: Throwable? = null,
                                     var playlist: Playlist, // card's playlist - has total count
                                     var likedCount: Int = 0,
                                     var dislikedCount: Int = 0,
                                     var trackStats: TrackListStats? = null // stats for ALL tracks
    ) : MviViewState {
        var stashedTracksList: List<TrackModel> = listOf() // only local tracks
            set(value) {
                field = value
                playlist.unswipedTrackIds = unswipedTrackIds
            }

        var allTracksList: List<TrackModel> = listOf() // ALL tracks (from remote)
            set(value) {
                field = value
                playlist.unswipedTrackIds = unswipedTrackIds
            }

        private var allSwipeableTracksList: List<TrackModel> = listOf() // ALL tracks that are also swipeable
            get() = allTracksList.filter { it.isRenderable() }
            set(value) {
                field = value
                playlist.unswipedTrackIds = unswipedTrackIds
            }

        var unswipedTrackIds: List<String> = listOf()
            get() =  allSwipeableTracksList.map { it.id } - stashedTracksList.map { it.id }

        val unswipedCount: Int
            get() = unswipedTrackIds.size

        fun isSuccess(): Boolean {
            return status == PlaylistCardResult.CalculateQuickCounts.Status.SUCCESS
                    || status == PlaylistCardResult.FetchPlaylistTracks.Status.SUCCESS
                    || status == StatsResultStatus.SUCCESS
        }

        fun isLoading(): Boolean {
            return status == PlaylistCardResult.CalculateQuickCounts.Status.LOADING
                    || status == PlaylistCardResult.FetchPlaylistTracks.Status.LOADING
                    || status == StatsResultStatus.LOADING
        }

        fun isError(): Boolean {
            return status == PlaylistCardResult.CalculateQuickCounts.Status.ERROR
                    || status == PlaylistCardResult.FetchPlaylistTracks.Status.ERROR
                    || status == StatsResultStatus.ERROR
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