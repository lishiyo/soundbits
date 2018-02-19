package com.cziyeli.songbits.profile

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.Utils
import com.cziyeli.commons.actionFilter
import com.cziyeli.commons.mvibase.*
import com.cziyeli.commons.resultFilter
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.ProfileActionMarker
import com.cziyeli.domain.user.ProfileActionProcessor
import com.cziyeli.domain.user.ProfileResultMarker
import com.cziyeli.domain.user.UserResult
import com.cziyeli.songbits.root.RootViewState
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Created by connieli on 2/18/18.
 */
class ProfileViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: ProfileActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<ProfileIntent, ProfileViewModel.ViewState> {
    private val TAG = ProfileViewModel::class.java.simpleName
    private val compositeDisposable = CompositeDisposable()

    // Listener for home-specific events
    private val intentsSubject : PublishRelay<ProfileIntentMarker> by lazy { PublishRelay.create<ProfileIntentMarker>() }
    // Listener for root view states
    private val rootStatesSubject: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }
    // Publish events into the stream from VM, in background
    // These are NOT coming from UI events, these are programmatic
    private val programmaticEventsPublisher = PublishRelay.create<ProfileIntentMarker>()

    // Publisher for own view states
    private val viewStates: PublishRelay<ProfileViewModel.ViewState> by lazy { PublishRelay.create<ProfileViewModel.ViewState>() }

    private val intentFilter: ObservableTransformer<ProfileIntentMarker, ProfileIntentMarker> = ObservableTransformer { intents ->
        intents.publish { shared -> shared
            Observable.merge<ProfileIntentMarker>(
                    shared.ofType(ProfileIntent.LoadOriginalStats::class.java).take(1), // only take initial one time
                    shared.filter({ intent -> intent !is ProfileIntent.LoadOriginalStats })
            )
        }
    }
    // Root ViewState => result
    private val rootResultProcessor: ObservableTransformer<RootViewState, MviResult> = ObservableTransformer { acts ->
        acts.map { rootState ->
            when {
                rootState.status == MviViewState.Status.SUCCESS && rootState.lastResult is UserResult.LoadLikedTracks -> {
                    UserResult.LoadLikedTracks.createSuccess(rootState.likedTracks)
                }
                else -> NoResult()
            }
        }
    }

    private val reducer: BiFunction<ProfileViewModel.ViewState, ProfileResultMarker, ProfileViewModel.ViewState> = BiFunction {
        previousState, result -> when (result) {
            is UserResult.LoadLikedTracks -> return@BiFunction processLikedTracks(previousState, result)
            is StatsResult.FetchFullStats -> return@BiFunction processFetchOriginalStats(previousState, result)
            else -> return@BiFunction previousState
        }
    }

    init {
        // create observable to push into states live data
        val observable = intentsSubject
                .mergeWith(programmaticEventsPublisher) // programmatic events for ViewModel to call events
                .subscribeOn(schedulerProvider.io())
                .compose(intentFilter)
                .map{ it -> actionFromIntent(it)}
                .compose(actionFilter<ProfileActionMarker>())
                .compose(actionProcessor.combinedProcessor) // own action -> own result
                .mergeWith( // root viewstate -> own result
                        rootStatesSubject.compose(rootResultProcessor).compose(resultFilter<ProfileResultMarker>())
                )
                .observeOn(schedulerProvider.ui())
                .doOnNext { result -> Utils.log(TAG, "intentsSubject scanning result: ${result.javaClass.simpleName}") }
                .scan(ProfileViewModel.ViewState(), reducer)

        compositeDisposable.add(
                observable.distinctUntilChanged().subscribe({ viewState ->
                    viewStates.accept(viewState)
                }, { err ->
                    Utils.log(TAG, err.localizedMessage)
                })
        )
    }

    private fun actionFromIntent(intent: ProfileIntentMarker) : MviAction {
        return when (intent) {
            is ProfileIntent.LoadOriginalStats -> StatsAction.FetchFullStats(intent.trackModels, intent.pref)
            else -> None // no-op all other events
        }
    }

    override fun processIntents(intents: Observable<out ProfileIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::accept)
        )
    }

    /**
     * Bind to the root stream.
     */
    fun processRootViewStates(intents: Observable<RootViewState>) {
        compositeDisposable.add(
                intents.subscribe(rootStatesSubject::accept)
        )
    }

    override fun states(): Observable<ViewState> {
        return viewStates
    }

    // ===== Individual reducers ======

    private fun processLikedTracks(
            previousState: ProfileViewModel.ViewState,
            result: UserResult.LoadLikedTracks
    ) : ProfileViewModel.ViewState {
        return when (result.status) {
            UserResult.Status.LOADING, MviResult.Status.LOADING -> {
                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.LOADING
                )
            }
            UserResult.Status.SUCCESS, MviResult.Status.SUCCESS -> {
                // fetch the liked tracks stats and set as original
                programmaticEventsPublisher.accept(ProfileIntent.LoadOriginalStats(result.items))

                previousState.copy(
                        error = null,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS
                )
            }
            UserResult.Status.ERROR, MviResult.Status.ERROR -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.ERROR
                )
            }
            else -> previousState
        }
    }

    private fun processFetchOriginalStats(
            previousState: ProfileViewModel.ViewState,
            result: StatsResult.FetchFullStats
    ) : ProfileViewModel.ViewState {
        Utils.mLog(TAG, "processFetchOriginalStats! ${result.status}", "${result.trackStats}")

        return when (result.status) {
            StatsResultStatus.LOADING, StatsResultStatus.ERROR -> {
                val status = if (result.status == StatsResultStatus.LOADING)
                    MviViewState.Status.LOADING else MviViewState.Status.ERROR
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = status
                )
            }
            StatsResultStatus.SUCCESS -> {
                previousState.copy(
                        error = result.error,
                        lastResult = result,
                        status = MviViewState.Status.SUCCESS,
                        originalStats = result.trackStats
                )
            }
            else -> return previousState
        }
    }

    data class ViewState(val status: MviViewState.Status = MviViewState.Status.IDLE,
                         val error: Throwable? = null,
                         val lastResult: ProfileResultMarker? = null,
                         val originalStats: TrackListStats? = null, // initial recommendations
                         val currentStats: TrackListStats? = null, // stats to seed recommendations
                         val recommendedTracks: MutableList<TrackModel> = mutableListOf()) : MviViewState {

        fun isFetchStatsSuccess(): Boolean {
            return status == MviViewState.Status.SUCCESS && lastResult is StatsResult.FetchFullStats && originalStats != null
        }

        override fun toString(): String {
            return "status: $status -- lastResult: $lastResult -- recs: ${recommendedTracks.size} -- originalStats: $originalStats"
        }
    }
}