package com.cziyeli.domain.tracks

import com.cziyeli.commons.Utils
import com.cziyeli.domain.summary.SummaryAction
import com.cziyeli.domain.summary.SwipeActionMarker
import com.cziyeli.domain.summary.SwipeResultMarker
import com.cziyeli.songbits.root.RootActionProcessor
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Action processor for the swipe UI - combines several action processors.
 *
 * Created by connieli on 2/25/18.
 */
@Singleton
class CardsActionProcessor @Inject constructor(private val rootActionProcessor: RootActionProcessor,
                                               private val tracksActionProcessor: TrackActionProcessor
) {
    private val TAG = CardsActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<SwipeActionMarker, SwipeResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<SwipeResultMarker>(
                    shared.ofType<TrackAction.SetTracks>(TrackAction.SetTracks::class.java)
                            .compose(tracksActionProcessor.setTracksProcessor),
                    shared.ofType<TrackAction.LoadTrackCards>(TrackAction.LoadTrackCards::class.java)
                            .compose(tracksActionProcessor.loadTrackCardsProcessor),
                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java)
                            .compose(tracksActionProcessor.commandPlayerProcessor),
                    shared.ofType<TrackAction.ChangeTrackPref>(TrackAction.ChangeTrackPref::class.java)
                            .compose(tracksActionProcessor.changePrefProcessor)
            ).mergeWith(
                    shared.ofType<SummaryAction.SaveTracks>(SummaryAction.SaveTracks::class.java)
                            .compose(rootActionProcessor.saveTracksProcessor)
            ).doOnNext {
                Utils.log(TAG, "commandPlayer processing --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }
}