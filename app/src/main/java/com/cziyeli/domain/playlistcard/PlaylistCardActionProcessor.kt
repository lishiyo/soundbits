package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.data.local.TrackEntity
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.TrackListStats
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
                    // fetch playlist tracks either from database (if swiped only) or from remote (all)
                    shared.ofType<PlaylistCardAction.FetchPlaylistTracks>(PlaylistCardAction.FetchPlaylistTracks::class.java)
                            .groupBy { it.onlySwiped }
                            .flatMap { grouped ->
                                if (grouped.key) {
                                    Utils.mLog(TAG, "FetchPlaylistTracks!", "onlySwiped -- fetching stashed")
                                    grouped.compose(fetchStashedTracksForPlaylist).compose(mapStashedTracksProcessor)
                                } else {
                                    Utils.mLog(TAG, "FetchPlaylistTracks!", "NOT onlySwiped -- fetching remote")
                                    grouped.compose(fetchAllTracksProcessor)
                                }
                            },
                    // given tracks list -> calculate count
                    shared.ofType<PlaylistCardAction.CalculateQuickCounts>(PlaylistCardAction.CalculateQuickCounts::class.java)
                            .compose(calculateQuickCountsProcessor),
                    // given playlist id -> grab all tracks (from remote) as well as stats
                    shared.ofType<StatsAction.FetchAllTracksWithStats>(StatsAction.FetchAllTracksWithStats::class.java)
                            .compose(fetchAllTracksWithStatsProcessor),
                    // given tracks list -> grab stats
                    shared.ofType<StatsAction.FetchStats>(StatsAction.FetchStats::class.java)
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

    // fetch quick stats - query in local database for counts
    private val calculateQuickCountsProcessor: ObservableTransformer<PlaylistCardAction.CalculateQuickCounts, PlaylistCardResult.CalculateQuickCounts> =
            ObservableTransformer { action -> action
                    .map { act -> act.tracks }
                    .map { list -> // return pair <liked, disliked>
                        val likedCount = list.count { it.liked }
                        val dislikedCount = list.count { it.disliked }
                        Pair(likedCount, dislikedCount)
                    }.observeOn(schedulerProvider.ui())
                    .map { pair -> PlaylistCardResult.CalculateQuickCounts.createSuccess(pair.first, pair.second) }
                    .onErrorReturn { err -> PlaylistCardResult.CalculateQuickCounts.createError(err) }
                    .startWith(PlaylistCardResult.CalculateQuickCounts.createLoading())
                    .retry() // don't unsubscribe
            }

    // fetch TrackEntities from database => return as domain TrackModels
    private val mapStashedTracksProcessor: ObservableTransformer<List<TrackEntity>, PlaylistCardResult.FetchPlaylistTracks> =
            ObservableTransformer { list ->
                    list.map { tracks -> tracks.map { TrackModel.createFromLocal(it) } }
                        .observeOn(schedulerProvider.ui())
                        .doOnNext { Utils.mLog(TAG, "mapping stashed tracks! count: ${it.size}")}
                        .map { trackCards -> PlaylistCardResult.FetchPlaylistTracks.createSuccess(trackCards, fromLocal = true) }
                        .onErrorReturn { err -> PlaylistCardResult.FetchPlaylistTracks.createError(err, true) }
                        .startWith(PlaylistCardResult.FetchPlaylistTracks.createLoading(true))
                        .retry() // don't unsubscribe
            }

    // fetch all tracks - hits remote
    private val fetchAllTracksProcessor: ObservableTransformer<PlaylistCardAction.FetchPlaylistTracks, PlaylistCardResult.FetchPlaylistTracks> =
            ObservableTransformer { action -> action.switchMap {
        act -> repository
            .fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
            .subscribeOn(schedulerProvider.io())
        }.filter { resp -> resp.total > 0 }
            .map { resp -> resp.items.map { it.track }}
            .map { tracks -> tracks.map { TrackModel.create(it) } }
            .observeOn(schedulerProvider.ui())
            .map { trackCards -> PlaylistCardResult.FetchPlaylistTracks.createSuccess(trackCards, fromLocal = false) }
            .onErrorReturn { err -> PlaylistCardResult.FetchPlaylistTracks.createError(err, false) }
            .startWith(PlaylistCardResult.FetchPlaylistTracks.createLoading(false))
            .retry() // don't unsubscribe
    }

    // playlist id => fetch all tracks and calculate full stats
    private val fetchAllTracksWithStatsProcessor: ObservableTransformer<StatsAction.FetchAllTracksWithStats,
            StatsResult.FetchAllTracksWithStats> = ObservableTransformer { action ->
        action.switchMap {
            act -> repository
                .fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
                .subscribeOn(schedulerProvider.io())
            }.map { resp -> resp.items.map { it.track }}
            .map { tracks -> tracks.map { TrackModel.create(it) } }
            .flatMap { tracks -> repository
                    .fetchTracksStats(Repository.Source.REMOTE, tracks.map { it.id })
                    .subscribeOn(schedulerProvider.io())
                    .map { resp -> Pair(tracks, TrackListStats.create(resp))}
            }
            .observeOn(schedulerProvider.ui())
            .map { pair ->
                StatsResult.FetchAllTracksWithStats.createSuccess(pair.first, pair.second)
            }
            .onErrorReturn { err -> StatsResult.FetchAllTracksWithStats.createError(err) }
            .startWith(StatsResult.FetchAllTracksWithStats.createLoading())
            .retry() // don't unsubscribe
    }

    // given list of tracks -> fetch track stats
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

    // given playlist id => track entities in db
    private val fetchStashedTracksForPlaylist: ObservableTransformer<PlaylistCardAction, List<TrackEntity>> = ObservableTransformer { action ->
        action.switchMap { act ->
            repository
                    .fetchPlaylistStashedTracks(playlistId = act.playlistId)
                    .toObservable()
                    .doOnNext { Utils.mLog(TAG, "fetchPlaylistStashedTracks size: ${it.size}") }
                    .subscribeOn(schedulerProvider.io())
        }.share().doOnNext { Utils.mLog(TAG, "fetchStashedTracks! count: ${it.size} ")}
    }


}