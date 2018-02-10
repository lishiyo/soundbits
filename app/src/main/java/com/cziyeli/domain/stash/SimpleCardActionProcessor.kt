package com.cziyeli.domain.stash

import com.cziyeli.data.Repository
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.playlistcard.CardResultMarker
import com.cziyeli.domain.user.UserManager
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton


/**
 * Business logic to convert actions => results on the [StashFragment] screen.
 * Combines both Playlist + User actions.
 *
 * Created by connieli on 12/31/17.
 */
@Singleton
class SimpleCardActionProcessor @Inject constructor(private val repository: Repository,
                                                    private val schedulerProvider: BaseSchedulerProvider,
                                                    private val userManager : UserManager) {
    private val TAG = SimpleCardActionProcessor::class.java.simpleName

    val combinedProcessor: ObservableTransformer<CardActionMarker, CardResultMarker> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.empty<CardResultMarker>().retry() // don't unsubscribe ever
        }
    }

}
