package com.cziyeli.domain.user

import com.cziyeli.data.Repository
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.summary.StatsAction
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.soundbits.root.RootActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Processes actions for the Profile tab.
 */
@Singleton
class ProfileActionProcessor @Inject constructor(private val repository: Repository,
                                                 private val schedulerProvider: BaseSchedulerProvider,
                                                 private val userActionProcessor: UserActionProcessor,
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
            ).mergeWith(
                    shared.ofType<ProfileAction.Reset>(ProfileAction.Reset::class.java)
                            .compose(resetProcessor)
            ).mergeWith(
                    shared.ofType<UserAction.ClearUser>(UserAction.ClearUser::class.java).compose(userActionProcessor.clearUserProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    private val resetProcessor: ObservableTransformer<ProfileAction.Reset, ProfileResult.Reset> = ObservableTransformer {
        action -> action.map { ProfileResult.Reset() }
    }

    private val fetchStatsProcessor: ObservableTransformer<ProfileAction.StatChanged, ProfileResult.StatChanged> = ObservableTransformer {
        action -> action.switchMap { act ->
            Observable.just(act)
                    .map { act.currentMap.updateWithStat(act.stat) }
                    .subscribeOn(schedulerProvider.io())
                    .map { ProfileResult.StatChanged(statsMap = it)}
        }
    }

    private val fetchRecommendedTracksProcessor: ObservableTransformer<ProfileAction.FetchRecommendedTracks,
            ProfileResult.FetchRecommendedTracks> = ObservableTransformer { action -> action.switchMap { act ->
            repository
                    .fetchRecommendedTracks(limit = act.limit,
                            seedGenres = act.seedGenres.joinToString(","),
                            attributes = act.attributes)
                    .subscribeOn(schedulerProvider.io())
                    .map { it.tracks.map { TrackModel.create(it) }.filter { it.isSwipeable } }
                    .map { ProfileResult.FetchRecommendedTracks.createSuccess(it) }
                    .onErrorReturn { ProfileResult.FetchRecommendedTracks.createError(it) }
                    .startWith(ProfileResult.FetchRecommendedTracks.createLoading())
        }
    }
}