package com.cziyeli.domain.tracks

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.domain.player.SpotifyPlayerManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic to convert actions => results.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class TrackActionProcessor @Inject constructor(private val repository: Repository,
                                               private val schedulerProvider: BaseSchedulerProvider,
                                               private val cardsViewModel: TrackViewModel) {

    val combinedProcessor: ObservableTransformer<TrackAction, TrackResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<TrackResult>(
                    shared.ofType<TrackAction.LoadTrackCards>(TrackAction.LoadTrackCards::class.java).compose(trackCardsProcessor),
                    Observable.empty()
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v ->
                        (v !is TrackAction)
                    }.flatMap { w -> Observable.error<TrackResult>(IllegalArgumentException("Unknown Action type: " + w)) }
            )
        }
    }


    // ==== individual list of processors (action -> result) ====
    val trackCardsProcessor : ObservableTransformer<TrackAction.LoadTrackCards, TrackResult.TrackCards> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .fetchPlaylistTracks(act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
                .subscribeOn(schedulerProvider.io())
        }.doOnNext { _ -> Utils.log("fetching com.cziyeli.domain.tracks")}
            .filter { resp -> resp.total > 0 }
            .map { resp -> resp.items.map { it.track }}
            .map { tracks -> tracks.map { TrackCard.create(it) } }
            .observeOn(schedulerProvider.ui())
            .map { trackCards -> TrackResult.TrackCards.createSuccess(trackCards) }
            .onErrorReturn { err -> TrackResult.TrackCards.createError(err) }
            .startWith(TrackResult.TrackCards.createLoading())
    }

    val createPlayerProcessor : ObservableTransformer<TrackAction.CommandPlayer, TrackResult.CommandPlayerResult> = ObservableTransformer {
        action -> action
            .switchMap
    }
}
