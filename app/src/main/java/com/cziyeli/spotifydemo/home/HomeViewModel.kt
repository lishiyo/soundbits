package com.cziyeli.spotifydemo.home

import android.arch.lifecycle.*
import android.util.Log
import com.cziyeli.commons.Utils
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.playlists.PlaylistAction
import com.cziyeli.domain.playlists.PlaylistActionProcessor
import com.cziyeli.domain.playlists.PlaylistResult
import com.cziyeli.spotifydemo.di.App
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
class HomeViewModel : ViewModel(), LifecycleObserver, MviViewModel<HomeIntent, HomeViewState> {

    // Dagger
    @Inject lateinit var repository: RepositoryImpl
    @Inject lateinit var actionProcessor: PlaylistActionProcessor
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
        Log.i("connie", "reducer ++ result: ${result.javaClass.simpleName} with status: ${result.status}")
        when (result) {
            is PlaylistResult.UserPlaylists -> return@BiFunction processUserPlaylists(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    private val component by lazy { App.appComponent.plus(HomeModule()) }

    init {
        // inject repo, actionprocessor, scheduler
        initializeDagger()

        Utils.log("init ViewModel!")

        // create observable to push into states live data
        val observable: Observable<HomeViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .doOnSubscribe{ Log.i("connie", "subscribed!") }
                .doOnDispose{ Log.i("connie", "disposed!") }
                .doOnTerminate { Log.i("connie", "terminated!") }
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .doOnNext { intent -> Log.i("Connie", "ViewModel ++ intentsSubject hitActionProcessor: ${intent.javaClass.name}") }
                .compose(actionProcessor.combinedProcessor)
                .scan(HomeViewState.IDLE, reducer)
                // Emit the last one event of the stream on subscription
                // Useful when a View rebinds to the ViewModel after rotation.
                .replay(1)
                // Create the stream on creation without waiting for anyone to subscribe
                // This allows the stream to stay alive even when the UI disconnects and
                // match the stream's lifecycle to the ViewModel's one.
                .autoConnect(0) // automatically connect

        compositeDisposable.add(
                observable.subscribe({ viewState ->
                    Log.i("connie", "ViewModel ++ new viewState posted: $viewState")
                    liveViewState.postValue(viewState) // should be on main thread (if worker, use postValue)
                }, { err ->
                    Log.i("connie", "ViewModel ++ ERROR " + err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: MviIntent) : PlaylistAction {
        return when(intent) {
            is HomeIntent.LoadPlaylists -> PlaylistAction.UserPlaylists.create()
            else -> PlaylistAction.None.create()
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
        Log.i("connie", "ViewModel ++ unsubscribe, clear out disposables")
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
            }
            PlaylistResult.Status.FAILURE -> {
                newState.status = HomeViewState.Status.ERROR
                newState.error = result.error
            }
        }

        return newState
    }

    private fun initializeDagger() = component.inject(this)
}

data class HomeViewState(var status: Status = Status.NOT_LOGGED_IN,
                         var error: Throwable? = null,
                         val playlists: MutableLiveData<List<Playlist>> = MutableLiveData()) : MviViewState {
    enum class Status {
        NOT_LOGGED_IN, LOADING, SUCCESS, ERROR
    }

    companion object {
        // start with this!
        @JvmField val IDLE = HomeViewState()
    }
}