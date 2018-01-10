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
 * Created by connieli on 1/7/18.
 */
@Singleton
class SummaryActionProcessor @Inject constructor(private val repository: Repository,
                                                 private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = SummaryActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<SummaryAction, SummaryResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<SummaryResult>(
                    shared.ofType<SummaryAction.LoadStats>(SummaryAction.LoadStats::class.java).compose(mLoadStatsProcessor),
                    shared.ofType<SummaryAction.SaveTracks>(SummaryAction.SaveTracks::class.java).compose(mSaveTracksProcessor)
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

    private val mLoadStatsProcessor: ObservableTransformer<SummaryAction.LoadStats, SummaryResult.LoadStatsResult> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .fetchTracksStats(Repository.Source.REMOTE, act.trackIds)
                .subscribeOn(schedulerProvider.io())
            }.map { resp -> TrackListStats.create(resp) }
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> SummaryResult.LoadStatsResult.createSuccess(trackStats) }
            .onErrorReturn { err -> SummaryResult.LoadStatsResult.createError(err) }
            .startWith(SummaryResult.LoadStatsResult.createLoading())
            .retry() // don't unsubscribe
    }

    private val mSaveTracksProcessor: ObservableTransformer<SummaryAction.SaveTracks, SummaryResult.SaveTracks> = ObservableTransformer {
        action -> action
            .doOnNext { repository.saveTracksLocal(mapNewTracks(it)) }
            .map { act -> SummaryResult.SaveTracks.createSuccess(act.tracks, act.playlistId) }
            .observeOn(schedulerProvider.ui())
            .doOnNext { repository.debug() }
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
                    artistName = it.artist?.name,
                    popularity = it.popularity,
                    coverImageUrl = it.coverImage?.url
            )
        }
    }
}
