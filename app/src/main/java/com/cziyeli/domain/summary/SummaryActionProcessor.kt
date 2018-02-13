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
                    shared.ofType<SummaryAction.SaveTracks>(SummaryAction.SaveTracks::class.java).compose(mSaveTracksProcessor),
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
            }.retry() // don't ever unsubscribe
        }
    }

    // fetch full for liked tracks
    private val mFetchLikedStatsProcessor: ObservableTransformer<StatsAction.FetchFullStats, SummaryResult.FetchLikedStats> =
            ObservableTransformer {
        action -> action.compose(fetchStatsIntermediary)
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> SummaryResult.FetchLikedStats.createSuccess(trackStats) }
            .onErrorReturn { err -> SummaryResult.FetchLikedStats.createError(err) }
            .startWith(SummaryResult.FetchLikedStats.createLoading())
            .retry() // don't unsubscribe
    }

    private val mFetchDislikedStatsProcessor: ObservableTransformer<StatsAction.FetchFullStats, SummaryResult.FetchDislikedStats> =
            ObservableTransformer {
                action -> action.compose(fetchStatsIntermediary)
                    .observeOn(schedulerProvider.ui())
                    .map { trackStats -> SummaryResult.FetchDislikedStats.createSuccess(trackStats) }
                    .onErrorReturn { err -> SummaryResult.FetchDislikedStats.createError(err) }
                    .startWith(SummaryResult.FetchDislikedStats.createLoading())
                    .retry() // don't unsubscribe
            }

    private val mCreatePlaylistProcessor: ObservableTransformer<SummaryAction.CreatePlaylistWithTracks,
            SummaryResult.CreatePlaylistWithTracks> = ObservableTransformer {
                action -> action.switchMap {
                    act -> repository
                            .createPlaylist(act.ownerId, act.name, act.description, act.public)
                            .subscribeOn(schedulerProvider.io())
                            .flatMapObservable { playlist ->
                                Utils.mLog(TAG, "createPlaylistProcessor", "doAfterSuccess!", playlist.toString())
                                repository.addTracksToPlaylist(act.ownerId, playlist.id, act.tracks.map { it.uri }).subscribeOn(schedulerProvider.io())
                            }
                }
                .map { pair -> SummaryResult.CreatePlaylistWithTracks.createSuccess(pair.first, pair.second) }
                .onErrorReturn { SummaryResult.CreatePlaylistWithTracks.createError(it) }
                .startWith(SummaryResult.CreatePlaylistWithTracks.createLoading())
                .retry() // don't unsubscribe
    }

    private val mSaveTracksProcessor: ObservableTransformer<SummaryAction.SaveTracks, SummaryResult.SaveTracks> = ObservableTransformer {
        action -> action
            .doOnNext { repository.saveTracksLocal(mapNewTracks(it)) }
            .map { act -> SummaryResult.SaveTracks.createSuccess(act.tracks, act.playlistId) }
            .observeOn(schedulerProvider.ui())
            .onErrorReturn { err -> SummaryResult.SaveTracks.createError(err) }
            .startWith(SummaryResult.SaveTracks.createLoading())
            .retry() // don't unsubscribe
    }

    private val fetchStatsIntermediary: ObservableTransformer<StatsAction.FetchFullStats, TrackListStats> = ObservableTransformer {
        action -> action.switchMap {
        act -> repository
            .fetchTracksStats(Repository.Source.REMOTE, act.trackModels.map {it.id} )
            .map { Pair(it, act.trackModels)}
            .subscribeOn(schedulerProvider.io())
        }.map { resp -> TrackListStats.create(resp.first, resp.second) }
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
