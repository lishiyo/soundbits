package com.cziyeli.songbits.root

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.domain.playlists.HomeActionProcessor
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.QuickCounts
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Holds state for [RootActivity] and shared data between its tabs.
 */
class RootViewModel @Inject constructor(
        val repository: RepositoryImpl,
        actionProcessor: HomeActionProcessor,
        schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<RootIntent, RootViewState> {

    private val viewStates: PublishRelay<RootViewState> by lazy { PublishRelay.create<RootViewState>() }

    override fun processIntents(intents: Observable<out RootIntent>) {

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