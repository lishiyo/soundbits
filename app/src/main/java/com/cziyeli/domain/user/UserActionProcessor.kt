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
                    shared.ofType<UserAction.ClearUser>(UserAction.ClearUser::class.java).compose(clearUserProcessor)
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    // fetch and save the current user in UserManager
    val fetchUserProcessor : ObservableTransformer<UserAction.FetchUser, UserResult.FetchUser>
            = ObservableTransformer { action -> action.switchMap {
        _ -> repository
            .fetchCurrentUser()
            .subscribeOn(schedulerProvider.io())
            .toObservable()
    }
            .map { User.create(it) }
            .doOnNext { user ->
                userManager.saveUser(user)
            }
            .map { user -> UserResult.FetchUser.createSuccess(currentUser = user)}
            .onErrorReturn { err -> UserResult.FetchUser.createError(err) }
            .startWith(UserResult.FetchUser.createLoading())
            .retry()
    }

    // logout the user
    val clearUserProcessor : ObservableTransformer<UserAction.ClearUser, UserResult.ClearUser> = ObservableTransformer { action ->
        action.doOnNext { _ -> userManager.clearUser() }
                .doOnNext { Utils.mLog(TAG, "clearUserProcessor", "user: ${it} ")}
                .map { _ -> UserResult.ClearUser.createSuccess() }
                .onErrorReturn { err -> UserResult.ClearUser.createError(err) }
                .startWith(UserResult.ClearUser.createLoading())
                .retry()
    }
}