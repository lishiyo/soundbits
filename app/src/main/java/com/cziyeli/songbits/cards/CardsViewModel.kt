package com.cziyeli.songbits.cards

import android.arch.lifecycle.*
import com.cziyeli.commons.Utils
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.domain.tracks.TrackCard
import com.cziyeli.domain.tracks.TrackResult
import com.cziyeli.songbits.cards.di.CardsModule
import com.cziyeli.songbits.di.App
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.mvibase.MviIntent
import lishiyo.kotlin_arch.mvibase.MviViewModel
import lishiyo.kotlin_arch.mvibase.MviViewState
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Inject

/**
 * Created by connieli on 1/1/18.
 */
class CardsViewModel : ViewModel(), LifecycleObserver, MviViewModel<TrackIntent, TrackViewState> {

    // Dagger
    @Inject lateinit var repository: RepositoryImpl
    @Inject lateinit var actionProcessor: TrackActionProcessor
    val schedulerProvider = SchedulerProvider

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<TrackViewState> by lazy { MutableLiveData<TrackViewState>() }

    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<TrackIntent> by lazy { PublishSubject.create<TrackIntent>() }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<TrackViewState, TrackResult, TrackViewState> = BiFunction { previousState, result ->
        Utils.log("reducer ++ result: ${result.javaClass.simpleName} with status: ${result.status}")
        when (result) {
            is TrackResult.TrackCards -> return@BiFunction processTrackCards(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    private val component by lazy { App.appComponent.plus(CardsModule()) }

    init {
        // inject repo, actionprocessor, scheduler
        initializeDagger()

        // create observable to push into states live data
        val observable: Observable<TrackViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe{ Utils.log("subscribed!") }
                .doOnDispose{ Utils.log( "disposed!") }
                .doOnTerminate { Utils.log( "terminated!") }
                .map{ it -> actionFromIntent(it)}
                .doOnNext { intent -> Utils.log("ViewModel ++ intentsSubject hitActionProcessor: ${intent.javaClass.name}") }
                .compose(actionProcessor.combinedProcessor)
                .scan(TrackViewState(), reducer)

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    liveViewState.postValue(viewState)
                }, { err ->
                    Utils.log("ViewModel ++ ERROR " + err.localizedMessage)
                })
        )
    }

    fun setPlaylist(playlist: Playlist) {
        liveViewState.value?.playlist = playlist
    }

    private fun actionFromIntent(intent: MviIntent) : TrackAction {
        return when(intent) {
            is TrackIntent.LoadTrackCards -> TrackAction.LoadTrackCards.create(
                    intent.ownerId, intent.playlistId, intent.fields, intent.limit, intent.offset)
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

    private fun processTrackCards(previousState: TrackViewState, result: TrackResult.TrackCards): TrackViewState {
        val newState = previousState.copy()
        Utils.log("previous items: ${previousState.items.size}")
        newState.error = null

        when (result.status) {
            TrackResult.Status.LOADING -> {
                newState.status = TrackViewState.Status.LOADING
            }
            TrackResult.Status.SUCCESS -> {
                newState.status = TrackViewState.Status.SUCCESS
//                newState.items.clear()
                newState.items.addAll(result.items.filter { it.isRenderable() })
            }
            TrackResult.Status.FAILURE -> {
                newState.status = TrackViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun initializeDagger() = component.inject(this)
}

data class TrackViewState(var status: Status = Status.IDLE,
                          var error: Throwable? = null,
                          val items: MutableList<TrackCard> = mutableListOf(),
                          var playlist: Playlist? = null) : MviViewState {
    enum class Status {
        IDLE, LOADING, SUCCESS, ERROR
    }
}