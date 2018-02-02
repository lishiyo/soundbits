package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.summary.TrackStatsResult
import com.cziyeli.domain.tracks.TrackModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistCardActionProcessor @Inject constructor(private val repository: Repository,
                                                      private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = PlaylistCardActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<PlaylistCardActionMarker, PlaylistCardResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<PlaylistCardResultMarker>(
                    shared.ofType<PlaylistCardAction.FetchQuickStats>(PlaylistCardAction.FetchQuickStats::class.java).compose(fetchQuickCountsProcessor),
                    shared.ofType<StatsAction.FetchStats>(StatsAction.FetchStats::class.java).compose(mFetchStatsProcessor),
                    shared.ofType<PlaylistCardAction.FetchPlaylistTracks>(PlaylistCardAction.FetchPlaylistTracks::class.java).compose(mFetchTracksProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v -> (v !is PlaylistCardActionMarker) }
                            .flatMap { w ->
                                Observable.error<PlaylistCardResultMarker>(IllegalArgumentException("Unknown Action type: " + w))
                            }
            ).doOnNext {
                Utils.log(TAG, "commandPlayer processing --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    // fetch quick stats - query in local database for counts
    private val fetchQuickCountsProcessor: ObservableTransformer<PlaylistCardAction.FetchQuickStats, PlaylistCardResult.FetchQuickStats> =
            ObservableTransformer { action ->
                action.switchMap { act -> repository
                            .fetchPlaylistStashedTracks(playlistId = act.playlistId)
                            .toObservable()
                            .doOnNext { Utils.mLog(TAG, "fetchPlaylistStashedTracks size: ${it.size}")}
                            .subscribeOn(schedulerProvider.io())
                }.map { list -> // return pair <liked, disliked>
                            val likedCount = list.count { it.liked }
                            val dislikedCount = list.count { !it.liked }
                            Pair(likedCount, dislikedCount)
                        }.observeOn(schedulerProvider.ui())
                        .map { pair -> PlaylistCardResult.FetchQuickStats.createSuccess(pair.first, pair.second) }
                        .onErrorReturn { err -> PlaylistCardResult.FetchQuickStats.createError(err) }
                        .startWith(PlaylistCardResult.FetchQuickStats.createLoading())
                        .retry() // don't unsubscribe
            }

    // fetch all tracks
    private val mFetchTracksProcessor: ObservableTransformer<PlaylistCardAction.FetchPlaylistTracks, PlaylistCardResult.FetchPlaylistTracks> =
            ObservableTransformer {
        action -> action.switchMap {
        act -> repository
            .fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
            .subscribeOn(schedulerProvider.io())
    }.filter { resp -> resp.total > 0 }
            .map { resp -> resp.items.map { it.track }}
            .map { tracks -> tracks.map { TrackModel.create(it) } }
            .observeOn(schedulerProvider.ui())
            .map { trackCards -> PlaylistCardResult.FetchPlaylistTracks.createSuccess(trackCards) }
            .onErrorReturn { err -> PlaylistCardResult.FetchPlaylistTracks.createError(err) }
            .startWith(PlaylistCardResult.FetchPlaylistTracks.createLoading())
            .retry() // don't unsubscribe
    }


    // fetch track stats
    private val mFetchStatsProcessor: ObservableTransformer<StatsAction.FetchStats, TrackStatsResult.FetchStats> = ObservableTransformer {
        action -> action.switchMap {
        act -> repository
                .fetchTracksStats(Repository.Source.REMOTE, act.trackIds)
                .subscribeOn(schedulerProvider.io())
            }.map { resp -> TrackListStats.create(resp) }
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> TrackStatsResult.FetchStats.createSuccess(trackStats) }
            .onErrorReturn { err -> TrackStatsResult.FetchStats.createError(err) }
            .startWith(TrackStatsResult.FetchStats.createLoading())
            .retry() // don't unsubscribe
    }

}