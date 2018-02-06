package com.cziyeli.songbits.home

import android.arch.lifecycle.*
import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.*
import com.cziyeli.domain.user.QuickCounts
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

class HomeViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: HomeActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<HomeIntent, HomeViewState> {
    private val TAG = HomeViewModel::class.simpleName

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<HomeViewState> by lazy { MutableLiveData<HomeViewState>() }

    // subject to publish ViewStates
    private val intentsSubject : PublishRelay<HomeIntent> by lazy { PublishRelay.create<HomeIntent>() }

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
    private val reducer: BiFunction<HomeViewState, HomeResult, HomeViewState> = BiFunction { previousState, result ->
        when (result) {
            is PlaylistsResult.UserPlaylists -> return@BiFunction processUserPlaylists(previousState, result)
            is UserResult.FetchUser -> return@BiFunction processCurrentUser(previousState, result)
            is UserResult.ClearUser -> return@BiFunction processClearedUser(previousState, result)
            is UserResult.FetchQuickCounts -> return@BiFunction processUserQuickCounts(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable: Observable<HomeViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe{ Utils.log(TAG, "subscribed!") }
                .doOnDispose{ Utils.log( TAG, "disposed!") }
                .doOnTerminate { Utils.log( TAG, "terminated!") }
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .doOnNext { intent -> Utils.log(TAG, "intentsSubject hitActionProcessor: ${intent.javaClass.simpleName}") }
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
                    Utils.log(TAG, err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : HomeAction {
        return when(intent) {
            is HomeIntent.LoadPlaylists -> PlaylistsAction.UserPlaylists(intent.limit, intent.offset)
            is HomeIntent.FetchUser -> UserAction.FetchUser()
            is HomeIntent.LogoutUser -> UserAction.ClearUser()
            is HomeIntent.FetchUserQuickCounts -> UserAction.FetchQuickCounts()
            else -> PlaylistsAction.None // no-op all other events
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

    private fun processUserPlaylists(previousState: HomeViewState, result: PlaylistsResult.UserPlaylists): HomeViewState {
        val newState: HomeViewState = previousState.copy()
        newState.error = null

        when (result.status) {
            PlaylistsResult.Status.LOADING -> {
                newState.status = MviViewState.Status.LOADING
            }
            PlaylistsResult.Status.SUCCESS, PlaylistsResult.Status.IDLE -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.playlists.addAll(result.playlists)
            }
            PlaylistsResult.Status.ERROR -> {
                newState.status = MviViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processUserQuickCounts(previousState: HomeViewState, result: UserResult.FetchQuickCounts) : HomeViewState {
        val newState: HomeViewState = previousState.copy()
        newState.error = null
        Utils.mLog(TAG, "processUserQuickCounts", "status", result.status.toString())

        when (result.status) {
            UserResult.FetchQuickCounts.Status.SUCCESS -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.quickCounts = result.quickCounts
            }
            UserResult.FetchQuickCounts.Status.ERROR -> {
                newState.status = MviViewState.Status.SUCCESS
                newState.error = result.error
            }
        }
        return newState
    }

    private fun processClearedUser(previousState: HomeViewState, result: UserResult.ClearUser) : HomeViewState {
        val newState: HomeViewState = previousState.copy()
        newState.error = null
        Utils.mLog(TAG, "processClearedUser", "status", result.status.toString())

        when (result.status) {
            UserResult.Status.ERROR -> {
                newState.error = result.error
            }
        }

        return newState
    }

    private fun processCurrentUser(previousState: HomeViewState, result: UserResult.FetchUser) : HomeViewState {
        val newState: HomeViewState = previousState.copy()
        newState.error = null

        Utils.mLog(TAG, "processCurrentUser", "status", result.status.toString(), "result.currentUser",
                result.currentUser?.toString())

        when (result.status) {
            UserResult.Status.LOADING -> {
                newState.loggedInStatus = UserResult.Status.LOADING
            }
            UserResult.Status.SUCCESS -> {
                if (result.currentUser != null) {
                    newState.loggedInStatus = UserResult.Status.SUCCESS
                }
            }
            UserResult.Status.ERROR -> {
                newState.loggedInStatus = UserResult.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }
}

data class HomeViewState(var status: MviViewState.Status = MviViewState.Status.IDLE,
                         var loggedInStatus: UserResult.Status = UserResult.Status.IDLE,
                         var error: Throwable? = null,
                         val playlists: MutableList<Playlist> = mutableListOf(),
                         var quickCounts: QuickCounts? = null
) : MviViewState