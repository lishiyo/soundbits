package com.cziyeli.songbits.home

import android.arch.lifecycle.*
import com.cziyeli.commons.Utils
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.playlists.PlaylistAction
import com.cziyeli.domain.playlists.PlaylistActionProcessor
import com.cziyeli.domain.playlists.PlaylistResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.mvibase.MviIntent
import lishiyo.kotlin_arch.mvibase.MviViewModel
import lishiyo.kotlin_arch.mvibase.MviViewState
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider
import javax.inject.Inject

/**
 * Created by connieli on 12/31/17.
 */
class HomeViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: PlaylistActionProcessor
) : ViewModel(), LifecycleObserver, MviViewModel<HomeIntent, HomeViewState> {

    val schedulerProvider = SchedulerProvider

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<HomeViewState> by lazy { MutableLiveData<HomeViewState>() }

    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<HomeIntent> by lazy { PublishSubject.create<HomeIntent>() }

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private val intentFilter: ObservableTransformer<HomeIntent, HomeIntent> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<HomeIntent>(
                    shared.ofType(HomeIntent.Initial::class.java).take(1), // only take initial one time
                    shared.filter({ intent -> intent !is HomeIntent.Initial })
            )
        }
    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<HomeViewState, PlaylistResult, HomeViewState> = BiFunction { previousState, result ->
        Utils.log("reducer ++ result: ${result.javaClass.simpleName} with status: ${result.status}")
        when (result) {
            is PlaylistResult.UserPlaylists -> return@BiFunction processUserPlaylists(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable: Observable<HomeViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe{ Utils.log("subscribed!") }
                .doOnDispose{ Utils.log( "disposed!") }
                .doOnTerminate { Utils.log( "terminated!") }
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .doOnNext { intent -> Utils.log("ViewModel ++ intentsSubject hitActionProcessor: ${intent.javaClass.name}") }
                .compose(actionProcessor.combinedProcessor)
                .scan(HomeViewState(), reducer)
                // Emit the last one event of the stream on subscription
                // Useful when a View rebinds to the ViewModel after rotation.
                .replay(1)
                // Create the stream on creation without waiting for anyone to subscribe
                // This allows the stream to stay alive even when the UI disconnects and
                // match the stream's lifecycle to the ViewModel's one.
                .autoConnect(0) // automatically connect

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    liveViewState.postValue(viewState) // should be on main thread (if worker, use postValue)
                }, { err ->
                    Utils.log("ViewModel ++ ERROR " + err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : PlaylistAction {
        return when(intent) {
            is HomeIntent.LoadPlaylists -> PlaylistAction.UserPlaylists.create(intent.limit, intent.offset)
            else -> PlaylistAction.None // no-op all other events
        }
    }

    override fun processIntents(intents: Observable<out HomeIntent>) {
        intents.subscribe(intentsSubject)
    }

    override fun states(): LiveData<HomeViewState> {
        return liveViewState
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

    private fun processUserPlaylists(previousState: HomeViewState, result: PlaylistResult.UserPlaylists): HomeViewState {
        val newState: HomeViewState = previousState.copy()
        newState.error = null

        when (result.status) {
            PlaylistResult.Status.LOADING -> {
                newState.status = HomeViewState.Status.LOADING
            }
            PlaylistResult.Status.SUCCESS, PlaylistResult.Status.IDLE -> {
                newState.status = HomeViewState.Status.SUCCESS
                newState.playlists.addAll(result.playlists)
            }
            PlaylistResult.Status.FAILURE -> {
                newState.status = HomeViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }
}

data class HomeViewState(var status: Status = Status.IDLE,
                         var error: Throwable? = null,
                         val playlists: MutableList<Playlist> = mutableListOf()) : MviViewState {
    enum class Status {
        IDLE, LOADING, SUCCESS, ERROR
    }

}