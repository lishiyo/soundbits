package com.cziyeli.domain.stash

import com.cziyeli.data.Repository
import com.cziyeli.domain.user.UserManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StashActionProcessor @Inject constructor(private val repository: Repository,
                                               private val schedulerProvider: BaseSchedulerProvider,
                                               private val userManager : UserManager) {
    private val TAG = StashActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<StashActionMarker, StashResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<StashResultMarker>(
                    shared.ofType<StashAction.InitialLoad>(StashAction.InitialLoad::class.java).compose(initialLoadProcessor),
                    Observable.empty()
            ).retry() // don't unsubscribe ever
        }
    }

    // ==== individual list of processors (action -> result) ====

    private val initialLoadProcessor: ObservableTransformer<StashAction.InitialLoad, StashResult.InitialLoad> = ObservableTransformer {
        action -> action.map {
            StashResult.InitialLoad.createSuccess()
        }
    }

}