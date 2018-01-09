package com.cziyeli.domain.playlists

import com.cziyeli.data.Repository
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject

/**
 * Business logic to convert actions => results.
 *
 * Created by connieli on 12/31/17.
 */
class PlaylistActionProcessor @Inject constructor(private val repository: Repository,
                                                  private val schedulerProvider: BaseSchedulerProvider) {
    val combinedProcessor: ObservableTransformer<PlaylistAction, PlaylistResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<PlaylistResult>(
                    shared.ofType<PlaylistAction.UserPlaylists>(PlaylistAction.UserPlaylists::class.java).compose(userPlaylistsProcessor),
                    Observable.empty()
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v ->
                        (v !is PlaylistAction.UserPlaylists && v !is PlaylistAction.None)
                    }.flatMap { w -> Observable.error<PlaylistResult>(IllegalArgumentException("Unknown Action type: " + w)) }
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====
    private val userPlaylistsProcessor : ObservableTransformer<PlaylistAction.UserPlaylists, PlaylistResult.UserPlaylists>
            = ObservableTransformer { action -> action.switchMap {
                    act -> repository
                                .fetchUserPlaylists(Repository.Source.REMOTE, act.limit, act.offset)
                                .subscribeOn(schedulerProvider.io())
                }.filter { resp -> resp.total > 0 } // check if we have playlists
                    .map { resp ->
                        resp.items.map { Playlist.create(it) }
                    }
                    .observeOn(schedulerProvider.ui())
                    .map { playlists -> PlaylistResult.UserPlaylists.createSuccess(playlists) }
                    .onErrorReturn { err -> PlaylistResult.UserPlaylists.createError(err) }
                    .startWith(PlaylistResult.UserPlaylists.createLoading())
                    .retry()
            }

}