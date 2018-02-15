package com.cziyeli.songbits.root

import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.user.QuickCounts
import com.cziyeli.domain.user.UserAction
import com.cziyeli.domain.user.UserManager
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
                                              private val userManager : UserManager) {
    private val TAG = RootActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<MviAction, MviResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<MviResult>(
                    shared.ofType<UserAction.FetchQuickCounts>(UserAction.FetchQuickCounts::class.java).compose(fetchQuickCountsProcessor),
                    shared.ofType<UserAction.LoadLikedTracks>(UserAction.LoadLikedTracks::class.java).compose(likedTracksProcessor),
                    shared.ofType<UserAction.LoadDislikedTracks>(UserAction.LoadDislikedTracks::class.java).compose(dislikedTracksProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    private val fetchQuickCountsProcessor : ObservableTransformer<UserAction.FetchQuickCounts, UserResult.FetchQuickCounts> =
            ObservableTransformer { actions -> actions.switchMap { act ->
                repository.fetchUserQuickStats().toObservable()
                        .map { (total, liked, disliked) -> QuickCounts(total, liked, disliked) }
                        .doOnNext { Utils.mLog(TAG, "fetchQuickCountsProcessor", "got quickCounts: $it") }
                        .map { quickCounts -> UserResult.FetchQuickCounts.createSuccess(quickCounts) }
                        .onErrorReturn { err -> UserResult.FetchQuickCounts.createError(err) }
                        .subscribeOn(schedulerProvider.io())
                        .observeOn(schedulerProvider.ui())
                        .startWith(UserResult.FetchQuickCounts.createLoading())
                }
            }


    private val likedTracksProcessor : ObservableTransformer<UserAction.LoadLikedTracks, UserResult.LoadLikesCard>
            = ObservableTransformer { actions -> actions.switchMap { act ->
        repository.fetchUserTracks(Repository.Pref.LIKED, act.limit, act.offset).toObservable()
                .map { resp -> resp.map { TrackModel.createFromLocal(it) } }
                .map { tracks -> UserResult.LoadLikesCard.createSuccess(tracks) }
                .onErrorReturn { err -> UserResult.LoadLikesCard.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .startWith(UserResult.LoadLikesCard.createLoading())
        }
    }

    private val dislikedTracksProcessor : ObservableTransformer<UserAction.LoadDislikedTracks, UserResult.LoadDislikesCard>
            = ObservableTransformer { actions -> actions.switchMap { act ->
        repository.fetchUserTracks(Repository.Pref.DISLIKED, act.limit, act.offset).toObservable()
                .map { resp -> resp.map { TrackModel.createFromLocal(it) } }
                .map { tracks -> UserResult.LoadDislikesCard.createSuccess(tracks) }
                .onErrorReturn { err -> UserResult.LoadDislikesCard.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .observeOn(schedulerProvider.ui())
                .startWith(UserResult.LoadDislikesCard.createLoading())
            }
        }
}