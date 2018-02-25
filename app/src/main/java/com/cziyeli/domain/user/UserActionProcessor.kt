package com.cziyeli.domain.user

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import com.cziyeli.domain.playlists.HomeActionMarker
import com.cziyeli.domain.playlists.HomeActionProcessor
import com.cziyeli.domain.playlists.HomeResult
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
class UserActionProcessor @Inject constructor(private val repository: Repository,
                                              private val schedulerProvider: BaseSchedulerProvider,
                                              private val userManager : UserManager) {
    private val TAG = HomeActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<HomeActionMarker, HomeResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<HomeResult>(
                    shared.ofType<UserAction.FetchUser>(UserAction.FetchUser::class.java).compose(fetchUserProcessor),
                    shared.ofType<UserAction.ClearUser>(UserAction.ClearUser::class.java).compose(clearUserProcessor),
                    shared.ofType<UserAction.FetchQuickCounts>(UserAction.FetchQuickCounts::class.java).compose(fetchQuickCountsProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    val fetchQuickCountsProcessor : ObservableTransformer<UserAction.FetchQuickCounts, UserResult.FetchQuickCounts> =
            ObservableTransformer { actions -> actions.switchMap { act ->
                repository.fetchUserQuickStats().toObservable()
                        .map { (total, liked, disliked) -> QuickCounts(total, liked, disliked) }
                        .map { quickCounts -> UserResult.FetchQuickCounts.createSuccess(quickCounts) }
                        .onErrorReturn { err -> UserResult.FetchQuickCounts.createError(err) }
                        .subscribeOn(schedulerProvider.io())
                        .startWith(UserResult.FetchQuickCounts.createLoading())
                }
            }

    // fetch and save the current user in UserManager
    val fetchUserProcessor : ObservableTransformer<UserAction.FetchUser, UserResult.FetchUser>
            = ObservableTransformer { actions -> actions.switchMap { _ ->
        repository.fetchCurrentUser()
                .toObservable()
                .map { User.create(it) }
                .doOnNext { user -> userManager.saveUser(user) }
                .map { user -> UserResult.FetchUser.createSuccess(currentUser = user) }
                .onErrorReturn { err -> UserResult.FetchUser.createError(err) }
                .subscribeOn(schedulerProvider.io())
                .startWith(UserResult.FetchUser.createLoading())
        }
    }

    // logout the user
    val clearUserProcessor : ObservableTransformer<UserAction.ClearUser, UserResult.ClearUser> = ObservableTransformer { actions ->
        actions.switchMap { action ->
            Observable.just(action)
                    .doOnNext { _ -> userManager.clearUser() }
                    .doOnNext { Utils.mLog(TAG, "clearUserProcessor", "user: ${it} ") }
                    .map { _ -> UserResult.ClearUser.createSuccess() }
                    .onErrorReturn { err -> UserResult.ClearUser.createError(err) }
                    .subscribeOn(schedulerProvider.io())
                    .startWith(UserResult.ClearUser.createLoading())
        }
    }
}