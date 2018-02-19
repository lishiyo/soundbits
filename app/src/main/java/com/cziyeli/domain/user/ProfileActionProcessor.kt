package com.cziyeli.domain.user

import com.cziyeli.data.Repository
import com.cziyeli.domain.stash.SimpleCardActionProcessor
import com.cziyeli.domain.summary.StatsAction
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
                                                 private val simpleCardActionProcessor: SimpleCardActionProcessor
) {
    private val TAG = ProfileActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<ProfileActionMarker, ProfileResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<ProfileResultMarker>(
                    shared.ofType<StatsAction.FetchFullStats>(StatsAction.FetchFullStats::class.java)
                            .compose(simpleCardActionProcessor.fetchStatsProcessor),
                    Observable.empty()
//                    shared.ofType<SummaryAction.CreatePlaylistWithTracks>(SummaryAction.CreatePlaylistWithTracks::class.java)
//                            .compose(playlistCardCreateActionProcessor.createPlaylistProcessor),
//                    shared.ofType<TrackAction.ChangeTrackPref>(TrackAction.ChangeTrackPref::class.java)
//                            .compose(playlistCardActionProcessor.changePrefAndSaveProcessor),
//                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java)
//                            .compose(trackActionProcessor.commandPlayerProcessor)
            ).retry() // don't unsubscribe ever
        }
    }
}