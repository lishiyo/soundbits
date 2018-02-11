package com.cziyeli.domain.stash

import com.cziyeli.data.Repository
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.user.UserManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Business logic to convert actions => results on the [StashFragment] screen.
 * Combines both Playlist + User actions.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class SimpleCardActionProcessor @Inject constructor(private val repository: Repository,
                                                    private val schedulerProvider: BaseSchedulerProvider,
                                                    private val userManager : UserManager) {
    private val TAG = SimpleCardActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<CardActionMarker, CardResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<CardResultMarker>(
                    shared.ofType<StatsAction.FetchFullStats>(StatsAction.FetchFullStats::class.java).compose(fetchStatsProcessor),
                    Observable.empty()
            ).retry() // don't unsubscribe ever
        }
    }

    // given list of tracks -> fetch track stats
    private val fetchStatsProcessor: ObservableTransformer<StatsAction.FetchFullStats, StatsResult.FetchFullStats> = ObservableTransformer {
        action -> action.switchMap {
        act -> repository
            .fetchTracksStats(Repository.Source.REMOTE, act.trackModels.map {it.id} )
            .map { Pair(it, act.trackModels)}
            .subscribeOn(schedulerProvider.io())
        }.map { resp -> TrackListStats.create(resp.first, resp.second) }
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> StatsResult.FetchFullStats.createSuccess(trackStats) }
            .onErrorReturn { err -> StatsResult.FetchFullStats.createError(err) }
            .startWith(StatsResult.FetchFullStats.createLoading())
            .retry() // don't unsubscribe
    }

}
