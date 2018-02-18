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
                    shared.ofType<TrackAction.SetTracks>(TrackAction.SetTracks::class.java).compose(setTracksProcessor),
                    shared.ofType<TrackAction.LoadTrackCards>(TrackAction.LoadTrackCards::class.java).compose(loadTrackCardsProcessor),
                    shared.ofType<TrackAction.CommandPlayer>(TrackAction.CommandPlayer::class.java).compose(commandPlayerProcessor),
                    shared.ofType<TrackAction.ChangeTrackPref>(TrackAction.ChangeTrackPref::class.java).compose(changePrefProcessor)
            ).doOnNext {
                Utils.log(TAG, "commandPlayer processing --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    // ==== individual list of processors (action -> result) ====

    private val setTracksProcessor:  ObservableTransformer<TrackAction.SetTracks, TrackResult.LoadTrackCards> = ObservableTransformer {
        actions -> actions.switchMap { act ->
            Observable.just(act.tracks)
                    .map { trackCards -> TrackResult.LoadTrackCards.createSuccess(trackCards) }
                    .onErrorReturn { err -> TrackResult.LoadTrackCards.createError(err) }
                    .observeOn(schedulerProvider.ui())
                    .startWith(TrackResult.LoadTrackCards.createLoading())
        }
    }

    // fetch tracks from REMOTE for a playlist
    private val loadTrackCardsProcessor: ObservableTransformer<TrackAction.LoadTrackCards, TrackResult.LoadTrackCards> = ObservableTransformer {
        actions -> actions.switchMap { act ->
            repository
                .fetchPlaylistTracks(Repository.Source.REMOTE, act.ownerId, act.playlistId, act.fields, act.limit, act.offset)
                .map { resp ->
                    var finalTracks = if (resp.items.isNotEmpty() && act.onlyTrackIds.isNotEmpty()) {
                        resp.items.filter { playlistTrack -> act.onlyTrackIds.contains(playlistTrack.track.id) }
                    } else {
                        resp.items
                    }
                    finalTracks
                }
                .map { tracks -> tracks.map { TrackModel.create(it.track) } }
                .map { trackCards -> TrackResult.LoadTrackCards.createSuccess(trackCards) }
                .onErrorReturn { err -> TrackResult.LoadTrackCards.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .startWith(TrackResult.LoadTrackCards.createLoading())
        }
    }

    // play, pause, stop the audio player
    val commandPlayerProcessor : ObservableTransformer<TrackAction.CommandPlayer, TrackResult.CommandPlayerResult> =
            ObservableTransformer {
        actions -> actions.switchMap { act ->
            player.handlePlayerCommand(act.track, act.command)
        }.retry() // don't unsubscribe
    }

    // like, dislike, undo track - no saving in db
    private val changePrefProcessor : ObservableTransformer<TrackAction.ChangeTrackPref, TrackResult.ChangePrefResult> = ObservableTransformer {
        actions -> actions.switchMap { act ->
            Observable.just(act.track.copy(pref = act.pref))
                    .map { TrackResult.ChangePrefResult.createSuccess(it, it.pref) }
                    .onErrorReturn { err -> TrackResult.ChangePrefResult.createError(err) }
                    .retry()
            }
    }
}
