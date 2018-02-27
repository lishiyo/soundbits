package com.cziyeli.soundbits.home

import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.*
import com.cziyeli.domain.user.QuickCounts
import com.cziyeli.domain.user.UserAction
import com.cziyeli.domain.user.UserResult
import com.cziyeli.soundbits.root.RootViewState
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Viewmodel for the [HomeFragment].
 */
class HomeViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: HomeActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<HomeIntent, HomeViewState, HomeResult> {
    private val TAG = HomeViewModel::class.simpleName
    private val compositeDisposable = CompositeDisposable()

    // Listener for home-specific events
    private val intentsSubject: PublishRelay<HomeIntent> by lazy { PublishRelay.create<HomeIntent>() }
    // Listener for root view states
    private val rootStatesSubject: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    // Publisher for own view states
    private val viewStates: PublishRelay<HomeViewState> by lazy { PublishRelay.create<HomeViewState>() }

    /**
     * take only the first ever InitialIntent and all intents of other types
     * to avoid reloading data on config changes
     */
    private val intentFilter: ObservableTransformer<HomeIntent, HomeIntent> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<HomeIntent>(
                    shared.ofType(HomeIntent.Initial::class.java).take(1),
                    shared.ofType(HomeIntent.FetchUser::class.java).take(1), // only take initial one time
                    shared.ofType(HomeIntent.LoadUserPlaylists::class.java).take(1),
                    shared.ofType(HomeIntent.LoadFeaturedPlaylists::class.java).take(1)
            ).mergeWith(
                    shared.ofType(HomeIntent.FetchQuickCounts::class.java).take(1)
            ).mergeWith(
                    shared.filter({ intent -> intent !is SingleEventIntent || (intent.shouldRefresh())})
            )
        }
    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<HomeViewState, HomeResult, HomeViewState> = BiFunction { previousState, result ->
        when (result) {
            is PlaylistsResult.UserPlaylists -> return@BiFunction processUserPlaylists(previousState, result)
            is PlaylistsResult.FeaturedPlaylists -> return@BiFunction processFeaturedPlaylists(previousState, result)
            is UserResult.FetchUser -> return@BiFunction processCurrentUser(previousState, result)
            is UserResult.ClearUser -> return@BiFunction processClearedUser(previousState, result)
            is UserResult.FetchQuickCounts -> return@BiFunction processUserQuickCounts(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<HomeActionMarker>())
                .compose(actionProcessor.combinedProcessor) // action -> result
                .scan(HomeViewState(), reducer)
                .observeOn(schedulerProvider.ui())
                // Emit the last one event of the stream on subscription
                // Useful when a View rebinds to the ViewModel after rotation.
                .replay(1)
                // Create the stream on creation without waiting for anyone to subscribe
                // This allows the stream to stay alive even when the UI disconnects and
                // match the stream's lifecycle to the ViewModel's one.
                .autoConnect(0) // automatically connect

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.log(TAG, err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : MviAction {
        return when(intent) {
            is HomeIntent.LoadUserPlaylists -> PlaylistsAction.UserPlaylists(intent.limit, intent.offset)
            is HomeIntent.LoadFeaturedPlaylists -> PlaylistsAction.FeaturedPlaylists(intent.limit, intent.offset)
            is HomeIntent.FetchUser -> UserAction.FetchUser()
            is HomeIntent.FetchQuickCounts -> UserAction.FetchQuickCounts()
            is HomeIntent.LogoutUser -> UserAction.ClearUser()
            else -> None // no-op all other events
        }
    }

    /**
     * Bind to the root stream.
     */
    fun processRootViewStates(intents: Observable<RootViewState>) {
        compositeDisposable.add(
                intents.subscribe(rootStatesSubject::accept)
        )
    }

    override fun processIntents(intents: Observable<out HomeIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<HomeViewState> {
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

    private fun processUserPlaylists(previousState: HomeViewState, result: PlaylistsResult.UserPlaylists): HomeViewState {
        return when (result.status) {
            PlaylistsResult.Status.LOADING -> {
                previousState.copy(error = null, status = MviViewState.Status.LOADING, lastResult = result)
            }
            PlaylistsResult.Status.SUCCESS -> {
                val newPlaylists = mutableListOf<Playlist>()
                newPlaylists.addAll(previousState.userPlaylists + result.playlists)
                previousState.copy(
                        error = null,
                        status = MviViewState.Status.SUCCESS,
                        lastResult = result,
                        userPlaylists = newPlaylists
                )
            }
            PlaylistsResult.Status.ERROR -> {
                previousState.copy(error = result.error, status = MviViewState.Status.ERROR, lastResult = result)
            }
            else -> previousState
        }
    }

    private fun processFeaturedPlaylists(previousState: HomeViewState, result: PlaylistsResult.FeaturedPlaylists): HomeViewState {
        Utils.mLog(TAG, "processFeaturedPlaylists", "${result.status} -- ${result.playlists.size}")
        return when (result.status) {
            PlaylistsResult.Status.LOADING -> {
                previousState.copy(error = null, status = MviViewState.Status.LOADING, lastResult = result)
            }
            PlaylistsResult.Status.SUCCESS -> {
                val newPlaylists = mutableListOf<Playlist>()
                newPlaylists.addAll(previousState.featuredPlaylists + result.playlists)
                previousState.copy(
                        error = null,
                        status = MviViewState.Status.SUCCESS,
                        lastResult = result,
                        featuredPlaylists = newPlaylists
                )
            }
            PlaylistsResult.Status.ERROR -> {
                previousState.copy(error = result.error, status = MviViewState.Status.ERROR, lastResult = result)
            }
            else -> previousState
        }
    }

    private fun processUserQuickCounts(previousState: HomeViewState, result: UserResult.FetchQuickCounts) : HomeViewState {
        Utils.mLog(TAG, "processUserQuickCounts", "status", result.status.toString())

        return when (result.status) {
            UserResult.FetchQuickCounts.Status.SUCCESS -> {
                previousState.copy(
                        error = null,
                        status = MviViewState.Status.SUCCESS,
                        lastResult = result,
                        quickCounts = result.quickCounts
                )
            }
            UserResult.FetchQuickCounts.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.ERROR,
                        lastResult = result
                )
            }
            else -> previousState
        }
    }

    private fun processClearedUser(previousState: HomeViewState, result: UserResult.ClearUser) : HomeViewState {
        Utils.mLog(TAG, "processClearedUser", "status", result.status.toString())

        return when (result.status) {
            UserResult.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.ERROR,
                        lastResult = result
                )
            }
            else -> previousState
        }
    }

    private fun processCurrentUser(previousState: HomeViewState, result: UserResult.FetchUser) : HomeViewState {
        Utils.mLog(TAG, "processCurrentUser", "status", result.status.toString(),
                "result.currentUser", result.currentUser?.toString())

        return when (result.status) {
            UserResult.Status.LOADING, UserResult.Status.ERROR -> {
                val status = if (result.status == UserResult.Status.LOADING) MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(error = result.error, status = status, lastResult = result)
            }
            UserResult.Status.SUCCESS -> {
                previousState.copy(error = null, status = MviViewState.Status.SUCCESS, lastResult = result)
            }
            else -> previousState
        }
    }
}

data class HomeViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: MviResult = NoResult(),
                         val userPlaylists: List<Playlist> = listOf(),
                         val featuredPlaylists: List<Playlist> = listOf(),
                         val quickCounts: QuickCounts? = null
) : MviViewState {

    override fun toString(): String {
        return "status: $status -- $lastResult -- user: ${userPlaylists.size} -- featured: ${featuredPlaylists.size}"
    }
}