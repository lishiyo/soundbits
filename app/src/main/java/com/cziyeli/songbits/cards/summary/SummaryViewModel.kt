package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.*
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.stats.SummaryActionProcessor
import com.cziyeli.domain.stats.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.cards.TrackViewState
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.mvibase.MviViewModel
import lishiyo.kotlin_arch.mvibase.MviViewState
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Shown after you finish surfing all the cards.
 *
 * Created by connieli on 1/6/18.
 */

class SummaryViewModel @Inject constructor(
        private val repository: RepositoryImpl,
        actionProcessor: SummaryActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<SummaryIntent, SummaryViewState> {
    private val TAG = SummaryViewModel::class.simpleName

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<SummaryViewState> by lazy { MutableLiveData<SummaryViewState>() }

    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<SummaryIntent> by lazy { PublishSubject.create<SummaryIntent>() }

    // reducer fn: result -> state

    // intent -> action

    fun setUp(state: SummaryViewState) {
        // initial state
        liveViewState.value = state
    }

    // ===== Individual reducers ======

    override fun processIntents(intents: Observable<out SummaryIntent>) {
        compositeDisposable.add(
                intents.subscribe(intentsSubject::onNext)
        )
    }

    override fun states(): LiveData<SummaryViewState> {
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
}

data class SummaryViewState(var status: MviViewState.Status = MviViewState.Status.IDLE,
                            var error: Throwable? = null,
                            val allTracks: MutableList<TrackModel> = mutableListOf(),
                            var playlist: Playlist? = null, // relevant playlist if coming from one
                            var stats: TrackListStats? = null // main model
) : MviViewState {
    val currentLikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.LIKED }.toMutableList()

    val currentLikeIds: List<String>
        get() = currentLikes.map { it.id }

    val currentDislikes: MutableList<TrackModel>
        get() = allTracks.filter { it.pref == TrackModel.Pref.DISLIKED }.toMutableList()

    val unseen: MutableList<TrackModel>
        get() = (allTracks - (currentLikes + currentDislikes)).toMutableList()

    companion object {
        fun create(state: TrackViewState) : SummaryViewState {
            return SummaryViewState(
                    allTracks = state.allTracks,
                    playlist = state.playlist
            )
        }
    }
}