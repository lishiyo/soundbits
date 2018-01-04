package com.cziyeli.domain.tracks

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.domain.player.PlayerInterface
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Business logic to convert actions => results.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class TrackActionProcessor @Inject constructor(private val repository: Repository,
                                               private val schedulerProvider: BaseSchedulerProvider) {

    @field:[Inject Named("Native")] lateinit var player: PlayerInterface

    val combinedProcessor: ObservableTransformer<TrackAction, TrackResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<TrackResult>(
                    shared.ofType<TrackAction.LoadTrackCards>(TrackAction.LoadTrackCards::class.java).compose(trackCardsProcessor),
                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java).compose(commandPlayerProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v ->
                        (v !is TrackAction)
                    }.flatMap { w ->
                        Observable.error<TrackResult>(IllegalArgumentException("Unknown Action type: " + w))
                    }
            ).retry() // don't ever unsubscribe
        }
    }

    // ==== individual list of processors (action -> result) ====
    val trackCardsProcessor : ObservableTransformer<TrackAction.LoadTrackCards, TrackResult.TrackCards> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .fetchPlaylistTracks(act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
                .subscribeOn(schedulerProvider.io())
                .onErrorResumeNext(Observable.empty()) // swallow error
        }.doOnNext { _ -> Utils.log("fetching com.cziyeli.domain.tracks")}
            .filter { resp -> resp.total > 0 }
            .map { resp -> resp.items.map { it.track }}
            .map { tracks -> tracks.map { TrackCard.create(it) } }
            .observeOn(schedulerProvider.ui())
            .map { trackCards -> TrackResult.TrackCards.createSuccess(trackCards) }
            .onErrorReturn { err -> TrackResult.TrackCards.createError(err) }
            .startWith(TrackResult.TrackCards.createLoading())
            .retry() // don't unsubscribe
    }

    private val commandPlayerProcessor : ObservableTransformer<TrackAction.CommandPlayer, TrackResult.CommandPlayerResult> = ObservableTransformer {
        action -> action.switchMap {
            act -> player.handleTrack(act.track, act.command)
            .retry() // don't unsubscribe
        }.doOnNext {
            act -> Utils.log("commandPlayer processing --- $act ")
        }.retry() // don't unsubscribe
    }
}
