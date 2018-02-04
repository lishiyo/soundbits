package com.cziyeli.domain.playlistcard

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.domain.summary.*
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
                    // create new playlist from tracks
                    shared.ofType<SummaryAction.CreatePlaylistWithTracks>(SummaryAction.CreatePlaylistWithTracks::class.java)
                            .compose(mCreatePlaylistProcessor),
                    shared.ofType<PlaylistCardAction.CreateHeaderSet>(PlaylistCardAction.CreateHeaderSet::class.java)
                            .compose(mSetHeaderProcessor)
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

    private val mCreatePlaylistProcessor: ObservableTransformer<SummaryAction.CreatePlaylistWithTracks,
            SummaryResult.CreatePlaylistWithTracks> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .createPlaylist(act.ownerId, act.name, act.description, act.public)
                .subscribeOn(schedulerProvider.io())
                .flatMapObservable { playlist ->
                Utils.mLog(TAG, "createPlaylistProcessor", "doAfterSuccess!", playlist.toString())
                repository
                        .addTracksToPlaylist(act.ownerId, playlist.id, act.tracks.map { it.uri })
                        .subscribeOn(schedulerProvider.io())
                }
            }
            .observeOn(schedulerProvider.ui())
            .map { pair -> SummaryResult.CreatePlaylistWithTracks.createSuccess(pair.first, pair.second) }
            .onErrorReturn { SummaryResult.CreatePlaylistWithTracks.createError(it) }
            .startWith(SummaryResult.CreatePlaylistWithTracks.createLoading())
            .retry() // don't unsubscribe
    }

    private val mSetHeaderProcessor: ObservableTransformer<PlaylistCardAction.CreateHeaderSet,
            PlaylistCardResult.CreateHeaderSet> = ObservableTransformer {
        action -> action
            .map { it.headerImageUrl }
            .map { PlaylistCardResult.CreateHeaderSet(headerImageUrl = it)}
    }

}
