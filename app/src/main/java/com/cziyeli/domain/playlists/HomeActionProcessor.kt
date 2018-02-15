package com.cziyeli.domain.playlists

import com.cziyeli.data.Repository
import com.cziyeli.domain.user.UserAction
import com.cziyeli.domain.user.UserActionProcessor
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
                                              private val userActionProcessor: UserActionProcessor) {
    private val TAG = HomeActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<HomeActionMarker, HomeResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<HomeResult>(
                    shared.ofType<PlaylistsAction.UserPlaylists>(PlaylistsAction.UserPlaylists::class.java).compose(userPlaylistsProcessor),
                    shared.ofType<PlaylistsAction.FeaturedPlaylists>(PlaylistsAction.FeaturedPlaylists::class.java).compose(featuredPlaylistsProcessor),
                    shared.ofType<UserAction.FetchUser>(UserAction.FetchUser::class.java).compose(userActionProcessor.fetchUserProcessor),
                    shared.ofType<UserAction.ClearUser>(UserAction.ClearUser::class.java).compose(userActionProcessor.clearUserProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    /**
     * Fetch user's playlists.
     */
    private val userPlaylistsProcessor : ObservableTransformer<PlaylistsAction.UserPlaylists, PlaylistsResult.UserPlaylists>
            = ObservableTransformer { action -> action.switchMap { act ->
                    repository.fetchUserPlaylists(Repository.Source.REMOTE, act.limit, act.offset)
                            .map { resp -> resp.items.map { Playlist.create(it) } }
                            .map { playlists -> PlaylistsResult.UserPlaylists.createSuccess(playlists) }
                            .onErrorReturn { err -> PlaylistsResult.UserPlaylists.createError(err) }
                            .subscribeOn(schedulerProvider.io())
                            .observeOn(schedulerProvider.ui())
                            .startWith(PlaylistsResult.UserPlaylists.createLoading())
        }
    }

    /**
     * Fetch featured playlists.
     */
    private val featuredPlaylistsProcessor : ObservableTransformer<PlaylistsAction.FeaturedPlaylists, PlaylistsResult.FeaturedPlaylists>
            = ObservableTransformer { action -> action.switchMap { act ->
        repository.fetchFeaturedPlaylists(Repository.Source.REMOTE, act.limit, act.offset)
                .map { resp -> resp.items.map { Playlist.create(it) } }
                .map { playlists -> PlaylistsResult.FeaturedPlaylists.createSuccess(playlists) }
                .onErrorReturn { err -> PlaylistsResult.FeaturedPlaylists.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .startWith(PlaylistsResult.FeaturedPlaylists.createLoading())
        }
    }
}