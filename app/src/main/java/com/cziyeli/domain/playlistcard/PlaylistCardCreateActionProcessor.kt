package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.TrackListStats
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class PlaylistCardCreateActionProcessor @Inject constructor(private val repository: Repository,
                                                            private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = PlaylistCardCreateActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<PlaylistCardActionMarker, PlaylistCardResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<PlaylistCardResultMarker>(
                    // given tracks list -> grab stats
                    shared.ofType<StatsAction.FetchStats>(StatsAction.FetchStats::class.java)
                            .compose(fetchStatsProcessor),
                    // given tracks list -> grab stats TODO remove
                    shared.ofType<StatsAction.FetchStats>(StatsAction.FetchStats::class.java)
                            .take(0)
                            .compose(fetchStatsProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v -> (v !is PlaylistCardActionMarker) }
                            .flatMap { w ->
                                Observable.error<PlaylistCardResultMarker>(IllegalArgumentException("Unknown Action type: " + w))
                            }
            ).doOnNext {
                Utils.log(TAG, "PlaylistCardActionProcessor: --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    // given list of track ids -> fetch track stats
    private val fetchStatsProcessor: ObservableTransformer<StatsAction.FetchStats, StatsResult.FetchStats> = ObservableTransformer {
        action -> action.switchMap {
        act -> repository
            .fetchTracksStats(Repository.Source.REMOTE, act.trackIds)
            .subscribeOn(schedulerProvider.io())
    }.map { resp -> TrackListStats.create(resp) }
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> StatsResult.FetchStats.createSuccess(trackStats) }
            .onErrorReturn { err -> StatsResult.FetchStats.createError(err) }
            .startWith(StatsResult.FetchStats.createLoading())
            .retry() // don't unsubscribe
    }

}
