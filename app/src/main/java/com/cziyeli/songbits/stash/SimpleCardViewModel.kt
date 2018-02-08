package com.cziyeli.songbits.stash

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.SummaryActionProcessor
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.CardIntentMarker
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.Observable
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject


class SimpleCardViewModel @Inject constructor(
        val actionProcessor: SummaryActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<CardIntentMarker, SimpleCardViewModel.ViewState> {
    private val TAG = SimpleCardViewModel::class.java.simpleName

    private val viewStates: PublishRelay<SimpleCardViewModel.ViewState> by lazy {
        PublishRelay.create<SimpleCardViewModel.ViewState>() }

    override fun processIntents(intents: Observable<out CardIntentMarker>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun states(): Observable<SimpleCardViewModel.ViewState> {
        return viewStates
    }

    data class ViewState(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null,
                         var tracks: List<TrackModel>,
                         var trackStats: TrackListStats? = null, // stats for ALL tracks
                         var carouselHeaderUrl: String? = null
    ) : MviViewState {
        fun isFetchStatsSuccess(): Boolean {
            return status == StatsResultStatus.SUCCESS && trackStats != null
        }
        fun isCreateLoading(): Boolean {
            return status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.LOADING
        }
        fun isCreateFinished(): Boolean {
            return status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR ||
                    status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.SUCCESS
        }
        fun isError(): Boolean {
            return status == MviResult.Status.ERROR || status == StatsResultStatus.ERROR
                    || status == SummaryResult.CreatePlaylistWithTracks.CreateStatus.ERROR
        }
    }
}
