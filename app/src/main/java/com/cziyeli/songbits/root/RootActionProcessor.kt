package com.cziyeli.songbits.root

import com.cziyeli.commons.Utils
import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import com.cziyeli.domain.playlists.UserAction
import com.cziyeli.domain.playlists.UserResult
import com.cziyeli.domain.user.QuickCounts
import com.cziyeli.domain.user.UserManager
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
                    Observable.empty()
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    private val fetchQuickCountsProcessor : ObservableTransformer<UserAction.FetchQuickCounts, UserResult.FetchQuickCounts> =
            ObservableTransformer { action ->
                action.switchMap { act ->
                    repository.fetchUserQuickStats().subscribeOn(schedulerProvider.io()).toObservable()
                }.map { (total, liked, disliked) -> QuickCounts(total, liked, disliked) }
                        .doOnNext { Utils.mLog(TAG, "fetchQuickCountsProcessor","got quickCounts: $it")}
                        .observeOn(schedulerProvider.ui())
                        .map { quickCounts -> UserResult.FetchQuickCounts.createSuccess(quickCounts) }
                        .onErrorReturn { err -> UserResult.FetchQuickCounts.createError(err) }
                        .startWith(UserResult.FetchQuickCounts.createLoading())
                        .retry()
            }

}