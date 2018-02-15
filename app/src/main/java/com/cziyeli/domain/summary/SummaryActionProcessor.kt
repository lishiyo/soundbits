package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.data.local.TrackEntity
import com.cziyeli.domain.tracks.TrackModel
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
                                                 private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = SummaryActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<SummaryActionMarker, SummaryResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<SummaryResultMarker>(
                    shared.ofType<SummaryAction.SaveTracks>(SummaryAction.SaveTracks::class.java).compose(saveTracksProcessor),
                    shared.ofType<SummaryAction.CreatePlaylistWithTracks>(SummaryAction.CreatePlaylistWithTracks::class.java).compose
                    (mCreatePlaylistProcessor),
                    shared.ofType<StatsAction.FetchFullStats>(StatsAction.FetchFullStats::class.java)
                            .groupBy { it.pref }
                            .flatMap { grouped ->
                                val compose: Observable<SummaryResultMarker> = when {
                                    grouped.key == Repository.Pref.LIKED -> {
                                        Utils.mLog(TAG, "FetchFullStats!", "fetching liked")
                                        grouped.compose(mFetchLikedStatsProcessor)
                                    }
                                    grouped.key == Repository.Pref.DISLIKED -> {
                                        Utils.mLog(TAG, "FetchFullStats!", "fetching disliked")
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
                    .observeOn(schedulerProvider.ui())
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
                    .observeOn(schedulerProvider.ui())
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
                        .observeOn(schedulerProvider.ui())
                        .startWith(SummaryResult.CreatePlaylistWithTracks.createLoading())
            })
    }

    /**
     * Stash tracks in database.
     */
    private val saveTracksProcessor: ObservableTransformer<SummaryAction.SaveTracks, SummaryResult.SaveTracks> = ObservableTransformer {
        actions -> actions.switchMap({ action ->
            Observable.just(action)
                    .doOnNext { repository.saveTracksLocal(mapNewTracks(action)) }
                    .map { act -> SummaryResult.SaveTracks.createSuccess(act.tracks, act.playlistId) }
                    .onErrorReturn { err -> SummaryResult.SaveTracks.createError(err) }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .startWith(SummaryResult.SaveTracks.createLoading())
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

    private fun mapNewTracks(act: SummaryAction.SaveTracks) : List<TrackEntity> {
        return act.tracks.map {
            TrackEntity(
                    trackId = it.id,
                    name = it.name,
                    uri = it.uri,
                    previewUrl = it.preview_url,
                    liked = it.pref == TrackModel.Pref.LIKED,
                    cleared = false,
                    playlistId = act.playlistId,
                    artistName = it.artistName,
                    popularity = it.popularity,
                    coverImageUrl = it.imageUrl
            )
        }
    }
}
