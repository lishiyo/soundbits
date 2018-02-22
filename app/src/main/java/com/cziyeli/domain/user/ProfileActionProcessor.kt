package com.cziyeli.domain.user

import com.cziyeli.data.Repository
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.root.RootActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by connieli on 2/18/18.
 */
@Singleton
class ProfileActionProcessor @Inject constructor(private val repository: Repository,
                                                 private val schedulerProvider: BaseSchedulerProvider,
                                                 private val userManager: UserManager,
                                                 private val simpleCardActionProcessor: SimpleCardActionProcessor,
                                                 private val rootActionProcessor: RootActionProcessor
) {
    private val TAG = ProfileActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<ProfileActionMarker, ProfileResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<ProfileResultMarker>(
                    shared.ofType<UserAction.LoadLikedTracks>(UserAction.LoadLikedTracks::class.java)
                            .compose(rootActionProcessor.likedTracksProcessor),
                    shared.ofType<StatsAction.FetchFullStats>(StatsAction.FetchFullStats::class.java)
                            .compose(simpleCardActionProcessor.fetchStatsProcessor),
                    shared.ofType<ProfileAction.StatChanged>(ProfileAction.StatChanged::class.java)
                           .compose(fetchStatsProcessor),
                    shared.ofType<ProfileAction.FetchRecommendedTracks>(ProfileAction.FetchRecommendedTracks::class.java)
                            .compose(fetchRecommendedTracksProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    private val fetchStatsProcessor: ObservableTransformer<ProfileAction.StatChanged, ProfileResult.StatChanged> = ObservableTransformer {
        action -> action.switchMap { act ->
            Observable.just(act)
                    .map { act.currentMap.updateWithStat(act.stat) }
                    .subscribeOn(schedulerProvider.io())
                    .observeOn(schedulerProvider.ui())
                    .map { ProfileResult.StatChanged(statsMap = it)}
        }
    }

    private val fetchRecommendedTracksProcessor: ObservableTransformer<ProfileAction.FetchRecommendedTracks,
            ProfileResult.FetchRecommendedTracks> = ObservableTransformer { action -> action.switchMap { act ->
            repository
                    .fetchRecommendedTracks(limit = act.limit, attributes = act.attributes)
                    .subscribeOn(schedulerProvider.io())
                    .map { it.tracks.map { TrackModel.create(it) }.filter { it.isSwipeable } }
                    .map { ProfileResult.FetchRecommendedTracks.createSuccess(it) }
                    .onErrorReturn { ProfileResult.FetchRecommendedTracks.createError(it) }
                    .observeOn(schedulerProvider.ui())
                    .startWith(ProfileResult.FetchRecommendedTracks.createLoading())
        }
    }
}