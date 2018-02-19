package com.cziyeli.domain.stash

import com.cziyeli.data.Repository
import com.cziyeli.domain.tracks.TrackModel
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StashActionProcessor @Inject constructor(private val repository: Repository,
                                               private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = StashActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<StashActionMarker, StashResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<StashResultMarker>(
                    shared.ofType<StashAction.InitialLoad>(StashAction.InitialLoad::class.java).compose(initialLoadProcessor),
                    shared.ofType<StashAction.ClearTracks>(StashAction.ClearTracks::class.java).compose(clearTracksProcessor),
                    shared.ofType<StashAction.FetchUserTopTracks>(StashAction.FetchUserTopTracks::class.java).compose(fetchUserTopTracks)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    private val initialLoadProcessor: ObservableTransformer<StashAction.InitialLoad, StashResult.InitialLoad> = ObservableTransformer {
        action -> action.map {
            StashResult.InitialLoad.createSuccess()
        }
    }

    /**
     * "Clear" all swiped tracks from database to reset.
     */
    private val clearTracksProcessor: ObservableTransformer<StashAction.ClearTracks, StashResult.ClearTracks> = ObservableTransformer {
        action -> action.switchMap { act ->
            Observable.just(act)
                    .doOnNext { repository.clearStashedTracks(act.pref) }
                    .map { _ -> StashResult.ClearTracks.createSuccess(act.pref) }
                    .onErrorReturn { err -> StashResult.ClearTracks.createError(err) }
                    .startWith(StashResult.ClearTracks.createLoading())
        }
    }

    private val fetchUserTopTracks: ObservableTransformer<StashAction.FetchUserTopTracks, StashResult.FetchUserTopTracks> = ObservableTransformer {
        action -> action.switchMap { act ->
            repository.fetchUserTopTracks(time_range = act.time_range, limit = act.limit, offset = act.offset)
                    .subscribeOn(schedulerProvider.io())
                    .map { resp -> resp.items }
                    .map { tracks -> tracks.map { TrackModel.create(it) } }
                    .map { tracks ->
                        // combine with list of track entities
                        val stashedList = repository.fetchStashedTracksByIds(trackIds = tracks.map { it.id })
                                .observeOn(schedulerProvider.io())
                                .map { list -> list.map { TrackModel.createFromLocal(it) } }
                                .blockingFirst()

                        tracks.toMutableList().map { track ->
                            stashedList.findLast { it.id == track.id } ?: track
                        }
                    }
                    .map { tracks -> StashResult.FetchUserTopTracks.createSuccess(tracks) }
                    .onErrorReturn { err -> StashResult.FetchUserTopTracks.createError(err) }
                    .observeOn(schedulerProvider.ui())
                    .startWith(StashResult.FetchUserTopTracks.createLoading())
        }
    }
}