package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.songbits.root.RootActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process summary screen from actions to results.
 */
@Singleton
class SummaryActionProcessor @Inject constructor(private val repository: Repository,
                                                 private val schedulerProvider: BaseSchedulerProvider,
                                                 private val rootActionProcessor: RootActionProcessor
) {
    private val TAG = SummaryActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<SummaryActionMarker, SummaryResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<SummaryResultMarker>(
                    shared.ofType<SummaryAction.SaveTracks>(SummaryAction.SaveTracks::class.java)
                            .compose(rootActionProcessor.saveTracksProcessor),
                    shared.ofType<SummaryAction.CreatePlaylistWithTracks>(SummaryAction.CreatePlaylistWithTracks::class.java).compose
                    (mCreatePlaylistProcessor),
                    shared.ofType<StatsAction.FetchFullStats>(StatsAction.FetchFullStats::class.java)
                            .groupBy { it.pref }
                            .flatMap { grouped ->
                                val compose: Observable<SummaryResultMarker> = when {
                                    grouped.key == Repository.Pref.LIKED -> {
                                        grouped.compose(mFetchLikedStatsProcessor)
                                    }
                                    grouped.key == Repository.Pref.DISLIKED -> {
                                        grouped.compose(mFetchDislikedStatsProcessor)
                                    }
                                    else -> Observable.empty() // not handling the 'all' case
                                }
                                compose
                            }
            ).doOnNext {
                Utils.mLog(TAG, "processing --- ${it::class.simpleName}")
            }.retry() // don't unsubscribe
        }
    }

    /**
     * Fetch full stats for liked track ids.
     */
    private val mFetchLikedStatsProcessor: ObservableTransformer<StatsAction.FetchFullStats, SummaryResult.FetchLikedStats> =
            ObservableTransformer {
                actions -> actions.compose(fetchStatsIntermediary)
                    .map { trackStats -> SummaryResult.FetchLikedStats.createSuccess(trackStats) }
                    .onErrorReturn { err -> SummaryResult.FetchLikedStats.createError(err) }
                    .subscribeOn(schedulerProvider.io())
                    .startWith(SummaryResult.FetchLikedStats.createLoading())
            }

    /**
     * Fetch full stats for disliked track ids.
     */
    private val mFetchDislikedStatsProcessor: ObservableTransformer<StatsAction.FetchFullStats, SummaryResult.FetchDislikedStats> =
            ObservableTransformer {
                actions -> actions.compose(fetchStatsIntermediary)
                    .map { trackStats -> SummaryResult.FetchDislikedStats.createSuccess(trackStats) }
                    .onErrorReturn { err -> SummaryResult.FetchDislikedStats.createError(err) }
                    .subscribeOn(schedulerProvider.io())
                    .startWith(SummaryResult.FetchDislikedStats.createLoading())
            }

    /**
     * Create playlist from tracks.
     */
    private val mCreatePlaylistProcessor: ObservableTransformer<SummaryAction.CreatePlaylistWithTracks,
            SummaryResult.CreatePlaylistWithTracks> = ObservableTransformer {
                actions -> actions.switchMap({ action ->
                repository.createPlaylist(action.ownerId, action.name, action.description, action.public)
                        .flatMapObservable { playlist ->
                            repository.addTracksToPlaylist(
                                    action.ownerId,
                                    playlist.id,
                                    action.tracks.map { it.uri }
                            ).subscribeOn(schedulerProvider.io())
                        }
                        .map { pair -> SummaryResult.CreatePlaylistWithTracks.createSuccess(pair.first, pair.second) }
                        .onErrorReturn { SummaryResult.CreatePlaylistWithTracks.createError(it) }
                        .subscribeOn(schedulerProvider.io())
                        .startWith(SummaryResult.CreatePlaylistWithTracks.createLoading())
            })
    }

    // ==== INTERMEDIARY ====

    private val fetchStatsIntermediary: ObservableTransformer<StatsAction.FetchFullStats, TrackListStats> = ObservableTransformer {
        actions -> actions.switchMap { action ->
            repository
                    .fetchTracksStats(Repository.Source.REMOTE, action.trackModels.map { it.id })
                .map { Pair(it, action.trackModels) }
                .subscribeOn(schedulerProvider.io())
                .map { resp -> TrackListStats.create(resp.first, resp.second) }
        }
    }

}
