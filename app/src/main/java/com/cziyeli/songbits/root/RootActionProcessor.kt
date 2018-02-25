package com.cziyeli.songbits.root

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.UserAction
import com.cziyeli.domain.user.UserActionProcessor
import com.cziyeli.domain.user.UserResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Business logic to convert actions => results on the [RootActivity] screen.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class RootActionProcessor @Inject constructor(private val repository: Repository,
                                              private val schedulerProvider: BaseSchedulerProvider,
                                              private val userActionProcessor: UserActionProcessor
) {
    private val TAG = RootActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<MviAction, MviResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<MviResult>(
                    shared.ofType<UserAction.FetchQuickCounts>(UserAction.FetchQuickCounts::class.java)
                            .compose(userActionProcessor.fetchQuickCountsProcessor),
                    shared.ofType<UserAction.LoadLikedTracks>(UserAction.LoadLikedTracks::class.java).compose(likedTracksProcessor),
                    shared.ofType<UserAction.LoadDislikedTracks>(UserAction.LoadDislikedTracks::class.java).compose(dislikedTracksProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    val likedTracksProcessor : ObservableTransformer<UserAction.LoadLikedTracks, UserResult.LoadLikedTracks>
            = ObservableTransformer { actions -> actions.switchMap { act ->
        repository.fetchUserTracks(Repository.Pref.LIKED, act.limit, act.offset).toObservable()
                .map { resp -> resp.map { TrackModel.createFromLocal(it) } }
                .map { tracks -> UserResult.LoadLikedTracks.createSuccess(tracks) }
                .onErrorReturn { err -> UserResult.LoadLikedTracks.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .startWith(UserResult.LoadLikedTracks.createLoading())
        }
    }

    val dislikedTracksProcessor : ObservableTransformer<UserAction.LoadDislikedTracks, UserResult.LoadDislikedTracks>
            = ObservableTransformer { actions -> actions.switchMap { act ->
        repository.fetchUserTracks(Repository.Pref.DISLIKED, act.limit, act.offset).toObservable()
                .map { resp -> resp.map { TrackModel.createFromLocal(it) } }
                .map { tracks -> UserResult.LoadDislikedTracks.createSuccess(tracks) }
                .onErrorReturn { err -> UserResult.LoadDislikedTracks.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .startWith(UserResult.LoadDislikedTracks.createLoading())
            }
        }
}