package com.cziyeli.songbits.cards.summary

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.cziyeli.domain.playlists.Playlist
import com.cziyeli.domain.tracks.TrackModel
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import lishiyo.kotlin_arch.mvibase.MviViewModel
import lishiyo.kotlin_arch.mvibase.MviViewState

/**
 * Shown after you finish surfing all the cards.
 *
 * Created by connieli on 1/6/18.
 */

class SummaryViewModel : MviViewModel<SummaryIntent, SummaryViewState>, ViewModel(), LifecycleObserver {
    private val TAG = SummaryViewModel::class.simpleName

    private val compositeDisposable = CompositeDisposable()

    // LiveData-wrapped ViewState
    private val liveViewState: MutableLiveData<SummaryViewState> by lazy { MutableLiveData<SummaryViewState>() }

    // subject to publish ViewStates
    private val intentsSubject : PublishSubject<SummaryViewState> by lazy { PublishSubject.create<SummaryViewState>() }

    // created with - liked, disliked, current playlist

    // to calculate - PlaylistStats => PlaylistCard

    override fun processIntents(intents: Observable<out SummaryIntent>) {

    }

    override fun states(): LiveData<SummaryViewState> {
        return liveViewState
    }

}

data class SummaryViewState(var playlist: Playlist,
                            val likedTracks: List<TrackModel> = listOf(),
                            val discardTracks: List<TrackModel> = listOf()
) : MviViewState