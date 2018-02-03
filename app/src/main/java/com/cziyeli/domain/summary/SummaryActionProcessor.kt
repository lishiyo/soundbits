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
                    shared.ofType<StatsAction.FetchStats>(StatsAction.FetchStats::class.java).compose(mFetchLikedStatsProcessor),
                    shared.ofType<SummaryAction.SaveTracks>(SummaryAction.SaveTracks::class.java).compose(mSaveTracksProcessor),
                    shared.ofType<SummaryAction.CreatePlaylistWithTracks>(SummaryAction.CreatePlaylistWithTracks::class.java).compose(mCreatePlaylistProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v -> (v !is SummaryAction)
                    }.flatMap { w ->
                        Observable.error<SummaryResult>(IllegalArgumentException("Unknown Action type: " + w))
                    }
            ).doOnNext {
                Utils.log(TAG, "commandPlayer processing --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    private val mFetchLikedStatsProcessor: ObservableTransformer<StatsAction.FetchStats, SummaryResult.FetchLikedStats> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .fetchTracksStats(Repository.Source.REMOTE, act.trackIds)
                .subscribeOn(schedulerProvider.io())
            }.map { resp -> TrackListStats.create(resp) }
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> SummaryResult.FetchLikedStats.createSuccess(trackStats) }
            .onErrorReturn { err -> SummaryResult.FetchLikedStats.createError(err) }
            .startWith(SummaryResult.FetchLikedStats.createLoading())
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
