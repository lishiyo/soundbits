package com.cziyeli.songbits.root

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.UserAction
import com.cziyeli.domain.playlists.UserResult
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.QuickCounts
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Holds state for [RootActivity] and shared data between its tabs.
 */
class RootViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: RootActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<RootIntent, RootViewState> {
    private val TAG = RootViewModel::class.simpleName

    private val intentsSubject : PublishRelay<RootIntent> by lazy { PublishRelay.create<RootIntent>() }
    private val viewStates: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    private val compositeDisposable = CompositeDisposable()
    private val intentFilter: ObservableTransformer<RootIntent, RootIntent> = ObservableTransformer { intents ->
        intents.publish { shared ->
                    shared.ofType(RootIntent::class.java)
        }
    }

    // Previous ViewState + Result => New ViewState
    private val reducer: BiFunction<RootViewState, MviResult, RootViewState> = BiFunction { previousState, result ->
        when (result) {
            is UserResult.FetchQuickCounts -> return@BiFunction processUserQuickCounts(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable: Observable<RootViewState> = intentsSubject
                .subscribeOn(schedulerProvider.io())
                .doOnSubscribe{ Utils.log(TAG, "subscribed!") }
                .doOnDispose{ Utils.log( TAG, "disposed!") }
                .doOnTerminate { Utils.log( TAG, "terminated!") }
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<MviAction>())
                .compose(actionProcessor.combinedProcessor) // action -> result
                .observeOn(schedulerProvider.ui())
                .doOnNext { result -> Utils.log(TAG, "intentsSubject scanning result: ${result.javaClass.simpleName}") }
                .scan(RootViewState(), reducer)
                .replay(1)
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
            is RootIntent.FetchUserQuickCounts -> UserAction.FetchQuickCounts()
            else -> None // no-op all other events
        }
    }

    private fun processUserQuickCounts(previousState: RootViewState, result: UserResult.FetchQuickCounts) : RootViewState {
        Utils.mLog(TAG, "processUserQuickCounts", "status", result.status.toString())

        return when (result.status) {
            UserResult.FetchQuickCounts.Status.SUCCESS -> {
                previousState.copy(
                        error = null,
                        status = MviViewState.Status.SUCCESS,
                        quickCounts = result.quickCounts
                )
            }
            UserResult.FetchQuickCounts.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        status = MviViewState.Status.ERROR,
                        quickCounts = result.quickCounts
                )
            }
            else -> previousState
        }
    }

    override fun processIntents(intents: Observable<out RootIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    override fun states(): Observable<RootViewState> {
        return viewStates
    }

}

data class RootViewState(var status: MviViewState.Status = MviViewState.Status.IDLE,
                         var error: Throwable? = null,
                         var quickCounts: QuickCounts? = null,
                         var userStashedTracksList: MutableList<TrackModel> = mutableListOf() // ALL of my swiped tracks in db
) : MviViewState {
    val userLikedTracks: MutableList<TrackModel>
        get() = userStashedTracksList.filter { it.liked }.toMutableList()

    val userDislikedTracks: MutableList<TrackModel>
        get() = userStashedTracksList.filter { !it.liked }.toMutableList()

}