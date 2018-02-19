package com.cziyeli.domain.stash

import com.cziyeli.data.Repository
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.playlistcard.PlaylistCardActionProcessor
import com.cziyeli.domain.playlistcard.PlaylistCardCreateActionProcessor
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.summary.StatsResult
import com.cziyeli.domain.summary.SummaryAction
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.domain.tracks.TrackAction
import com.cziyeli.domain.tracks.TrackActionProcessor
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
                                                    private val playlistCardActionProcessor: PlaylistCardActionProcessor,
                                                    private val playlistCardCreateActionProcessor: PlaylistCardCreateActionProcessor,
                                                    private val trackActionProcessor: TrackActionProcessor
) {
    private val TAG = SimpleCardActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<CardActionMarker, CardResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<CardResultMarker>(
                    shared.ofType<StatsAction.FetchFullStats>(StatsAction.FetchFullStats::class.java).compose(fetchStatsProcessor),
                    shared.ofType<SummaryAction.CreatePlaylistWithTracks>(SummaryAction.CreatePlaylistWithTracks::class.java)
                            .compose(playlistCardCreateActionProcessor.createPlaylistProcessor),
                    shared.ofType<TrackAction.ChangeTrackPref>(TrackAction.ChangeTrackPref::class.java)
                            .compose(playlistCardActionProcessor.changePrefAndSaveProcessor),
                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java)
                            .compose(trackActionProcessor.commandPlayerProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // given list of tracks -> fetch FULL track stats
    val fetchStatsProcessor: ObservableTransformer<StatsAction.FetchFullStats, StatsResult.FetchFullStats> = ObservableTransformer {
        action -> action.switchMap { act ->
        repository.fetchTracksStats(Repository.Source.REMOTE, act.trackModels.map {it.id} )
                .map { Pair(it, act.trackModels)}
                .map { resp -> TrackListStats.create(resp.first, resp.second) }
                .map { trackStats -> StatsResult.FetchFullStats.createSuccess(trackStats) }
                .onErrorReturn { err -> StatsResult.FetchFullStats.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .startWith(StatsResult.FetchFullStats.createLoading())
        }
    }

}
