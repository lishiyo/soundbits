package com.cziyeli.songbits.stash

import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.ViewModel
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.commons.mvibase.MviViewModel
import com.cziyeli.commons.mvibase.MviViewState
import com.cziyeli.domain.summary.StatsResultStatus
import com.cziyeli.domain.summary.SummaryActionProcessor
import com.cziyeli.domain.summary.SummaryResult
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.SinglePlaylistIntent
import io.reactivex.Observable
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject


class SimpleCardViewModel @Inject constructor(
        val actionProcessor: SummaryActionProcessor,
        val schedulerProvider: BaseSchedulerProvider
) : ViewModel(), LifecycleObserver, MviViewModel<SinglePlaylistIntent, SimpleCardViewModel.ViewState> {

    override fun processIntents(intents: Observable<out SinglePlaylistIntent>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun states(): LiveData<ViewState> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private val TAG = SimpleCardViewModel::class.java.simpleName


    data class ViewState(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null,
                         var pendingTracks: List<TrackModel>,
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

        // make sure the tracks are there!
        fun copy() : ViewState {
            return ViewState(status, error, pendingTracks, trackStats, carouselHeaderUrl)
        }
    }
}