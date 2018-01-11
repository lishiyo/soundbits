package com.cziyeli.domain.playlists

import com.cziyeli.data.Repository
import com.cziyeli.domain.user.User
import com.cziyeli.domain.user.UserManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Business logic to convert actions => results on the [HomeActivity] screen.
 * Combines both Playlist + User actions.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class HomeActionProcessor @Inject constructor(private val repository: Repository,
                                              private val schedulerProvider: BaseSchedulerProvider,
                                              private val userManager : UserManager) {
    val combinedProcessor: ObservableTransformer<HomeAction, HomeResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<HomeResult>(
                    shared.ofType<PlaylistAction.UserPlaylists>(PlaylistAction.UserPlaylists::class.java).compose(userPlaylistsProcessor),
                    shared.ofType<UserAction.FetchUser>(UserAction.FetchUser::class.java).compose(fetchUserProcessor)
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v -> v !is HomeAction
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


    // fetch and save the current user in UserManager
    private val fetchUserProcessor : ObservableTransformer<UserAction.FetchUser, UserResult.FetchUser>
            = ObservableTransformer { action -> action.switchMap {
                _ -> repository
                    .fetchCurrentUser()
                    .subscribeOn(schedulerProvider.io())
                    .toObservable()
            }
            .map { User.create(it) }
            .doOnNext { user ->
                // instantiate user!
                userManager.CURRENT_USER = user
            }
            .map { user -> UserResult.FetchUser.createSuccess(currentUser = user)}
            .onErrorReturn { err -> UserResult.FetchUser.createError(err) }
            .startWith(UserResult.FetchUser.createLoading())
            .retry()
    }
}