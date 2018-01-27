package com.cziyeli.songbits.cards

import android.arch.lifecycle.*
import com.cziyeli.commons.Utils
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject



/**
 * Created by connieli on 1/1/18.
 */
class CardsViewModel @Inject constructor(
        private val repository: RepositoryImpl,
        actionProcessor: TrackActionProcessor,
        schedulerProvider: BaseSchedulerProvider,
        playlist: Playlist
): ViewModel(), LifecycleObserver, MviViewModel<TrackIntent, TrackViewState> {
    private val TAG = CardsViewModel::class.simpleName

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<TrackViewState> by lazy {
        val liveData = MutableLiveData<TrackViewState>()
        liveData.value = TrackViewState(playlist = playlist)
        liveData
    }

    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<TrackIntent> by lazy { PublishSubject.create<TrackIntent>() }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<TrackViewState, TrackResult, TrackViewState> = BiFunction { previousState, result ->
        when (result) {
            is TrackResult.LoadTrackCards -> return@BiFunction processTrackCards(previousState, result)
            is TrackResult.CommandPlayerResult -> return@BiFunction processPlayerCommand(previousState, result)
            is TrackResult.ChangePrefResult -> return@BiFunction processTrackChangePref(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable: Observable<TrackViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe{ Utils.log(TAG,"subscribed!") }
                .doOnDispose{ Utils.log(TAG,"disposed!") }
                .doOnTerminate { Utils.log(TAG, "terminated!") }
                .map{ it -> actionFromIntent(it)}
                .doOnNext { intent -> Utils.log(TAG, "ViewModel ++ intentsSubject hitActionProcessor: ${intent.javaClass.name}") }
                .compose(actionProcessor.combinedProcessor)
                .scan(liveViewState.value, reducer)

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    liveViewState.postValue(viewState)
                }, { err ->
                    Utils.log(TAG, "ViewModel ++ ERROR " + err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : TrackAction {
        return when(intent) {
            is TrackIntent.ScreenOpened -> TrackAction.LoadTrackCards.create(
                    intent.ownerId, intent.playlistId, intent.fields, intent.limit, intent.offset)
            is TrackIntent.CommandPlayer -> TrackAction.CommandPlayer.create(
                    intent.command, intent.track
            )
            is TrackIntent.ChangeTrackPref -> TrackAction.ChangeTrackPref.create(
                    intent.track, intent.pref
            )
            else -> TrackAction.None // no-op all other events
        }
    }

    override fun processIntents(intents: Observable<out TrackIntent>) {
        compositeDisposable.add(
            intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<TrackViewState> {
        return liveViewState
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun unSubscribeViewModel() {
        // clear out repo subscriptions as well
        for (disposable in repository.allCompositeDisposable) {
            compositeDisposable.addAll(disposable)
        }
        compositeDisposable.clear()

        // reset
        liveViewState.value = null
    }

    // ===== Individual reducers ======

    private fun processTrackCards(previousState: TrackViewState, result: TrackResult.LoadTrackCards): TrackViewState {
        val newState = previousState.copy()
        newState.error = null

        when (result.status) {
            TrackResult.LoadTrackCards.Status.LOADING -> {
                newState.status = TrackViewState.TracksLoadedStatus.LOADING
            }
            TrackResult.LoadTrackCards.Status.SUCCESS -> {
                newState.status = TrackViewState.TracksLoadedStatus.SUCCESS
                newState.allTracks.addAll(result.items.filter { it.isRenderable() })
            }
            TrackResult.LoadTrackCards.Status.FAILURE -> {
                newState.status = TrackViewState.TracksLoadedStatus.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processPlayerCommand(previousState: TrackViewState, result: TrackResult.CommandPlayerResult) : TrackViewState {
        val newState = previousState.copy()
        newState.error = null
        newState.currentTrack = result.currentTrack
        newState.currentPlayerState = result.currentPlayerState

        when (result.status) {
            MviResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            MviResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
            }
            MviResult.Status.FAILURE -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processTrackChangePref(previousState: TrackViewState, result: TrackResult.ChangePrefResult) : TrackViewState {
        val newState = previousState.copy()

        when (result.status) {
            MviResult.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.error = null
                newState.currentTrack = result.currentTrack

                val track = newState.allTracks.find { el -> el.id == newState.currentTrack?.id }
                track!!.pref = result.pref!!
                Utils.mLog(TAG, "processTrackChange ${track.name}",
                        "pref: ", track!!.pref.toString(),
                        "currentLikes: ", newState.currentLikes.size.toString(),
                        "currentDislikes: ", newState.currentDislikes.size.toString(),
                        "unseen: ", newState.unseen.size.toString())
            }
            MviResult.Status.FAILURE -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

}

data class TrackViewState(var status: MviViewState.StatusInterface = MviViewState.Status.IDLE,
                          var error: Throwable? = null,
                          val allTracks: MutableList<TrackModel> = mutableListOf(),
                          var playlist: Playlist,
                          var currentTrack: TrackModel? = null,
                          var currentPlayerState: PlayerInterface.State = PlayerInterface.State.INVALID
) : MviViewState {

    enum class TracksLoadedStatus : MviViewState.StatusInterface {
        LOADING, SUCCESS, ERROR
    }

    val currentLikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.LIKED }.toMutableList()

    val currentDislikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.DISLIKED }.toMutableList()

    val unseen: MutableList<TrackModel>
        get() = (allTracks - (currentLikes + currentDislikes)).toMutableList()

    val reachedEnd: Boolean
        get() = status == MviViewState.Status.SUCCESS && unseen.size == 0 && allTracks.size > 0

    override fun toString(): String {
        return "allTracks: ${allTracks.size} -- playlist: ${playlist.id} -- currentTrack: ${currentTrack?.name}"
    }
}

