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

    private val TAG = TrackActionProcessor::class.simpleName

    @field:[Inject Named("Native")] lateinit var player: PlayerInterface

    val combinedProcessor: ObservableTransformer<TrackAction, TrackResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<TrackResult>(
                    shared.ofType<TrackAction.LoadTrackCards>(TrackAction.LoadTrackCards::class.java).compose(mLoadTrackCardsProcessor),
                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java).compose(commandPlayerProcessor),
                    shared.ofType<TrackAction.ChangeTrackPref>(TrackAction.ChangeTrackPref::class.java).compose(changePrefProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v ->
                        (v !is TrackAction)
                    }.flatMap { w ->
                        Observable.error<TrackResult>(IllegalArgumentException("Unknown Action type: " + w))
                    }
            ).doOnNext {
                Utils.log(TAG, "commandPlayer processing --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    // ==== individual list of processors (action -> result) ====

    // load tracks for a playlist
    private val mLoadTrackCardsProcessor: ObservableTransformer<TrackAction.LoadTrackCards, TrackResult.LoadTrackCards> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
                .subscribeOn(schedulerProvider.io())
                .map { resp ->
                    var finalTracks = resp.items
                    if (resp.items.isNotEmpty() && act.onlyTrackIds.isNotEmpty()) {
                        finalTracks = resp.items.filter { playlistTrack -> act.onlyTrackIds.contains(playlistTrack.track.id) }
                    }
                    finalTracks
                }
            }
            .map { items -> items.map { it.track }}
            .map { tracks -> tracks.map { TrackModel.create(it) } }
            .observeOn(schedulerProvider.ui())
            .map { trackCards -> TrackResult.LoadTrackCards.createSuccess(trackCards) }
            .onErrorReturn { err -> TrackResult.LoadTrackCards.createError(err) }
            .startWith(TrackResult.LoadTrackCards.createLoading())
            .retry() // don't unsubscribe
    }

    // play, pause, stop the audio player
    private val commandPlayerProcessor : ObservableTransformer<TrackAction.CommandPlayer, TrackResult.CommandPlayerResult> = ObservableTransformer {
        action -> action.switchMap {
            act -> player.handlePlayerCommand(act.track, act.command)
        }.retry() // don't unsubscribe
    }

    // like, dislike, undo track
    private val changePrefProcessor : ObservableTransformer<TrackAction.ChangeTrackPref, TrackResult.ChangePrefResult> = ObservableTransformer {
        action -> action
            .map { act -> act.track.copy(pref = act.pref) } // return new for immutability
            .map { TrackResult.ChangePrefResult.createSuccess(it, it.pref) }
            .onErrorReturn { err -> TrackResult.ChangePrefResult.createError(err)}
            .retry()
    }
}
