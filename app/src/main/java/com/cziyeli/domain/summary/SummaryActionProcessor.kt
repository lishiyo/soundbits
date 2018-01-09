package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
import com.cziyeli.data.Repository
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by connieli on 1/7/18.
 */
@Singleton
class SummaryActionProcessor @Inject constructor(private val repository: Repository,
                                                 private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = SummaryActionProcessor::class.simpleName

    val combinedProcessor: ObservableTransformer<SummaryAction, SummaryResult> = ObservableTransformer { acts ->
        acts.publish { shared ->
            Observable.merge<SummaryResult>(
                    shared.ofType<SummaryAction.LoadStats>(SummaryAction.LoadStats::class.java).compose(mLoadStatsProcessor),
                    Observable.empty()
            ).mergeWith(
                    // Error for not implemented actions
                    shared.filter { v -> (v !is SummaryAction)
                    }.flatMap { w ->
                        Observable.error<SummaryResult>(IllegalArgumentException("Unknown Action type: " + w))
                    }
            ).doOnNext {
                Utils.log(TAG, "commandPlayer processing --- ${it::class.simpleName}")
            }.retry() // don't ever unsubscribe
        }
    }

    private val mLoadStatsProcessor: ObservableTransformer<SummaryAction.LoadStats, SummaryResult.LoadStatsResult> = ObservableTransformer {
        action -> action.switchMap {
            act -> repository
                .fetchTracksStats(Repository.Source.REMOTE, act.trackIds)
                .subscribeOn(schedulerProvider.io())
            }.map { resp -> TrackListStats.create(resp) }
            .observeOn(schedulerProvider.ui())
            .map { trackStats -> SummaryResult.LoadStatsResult.createSuccess(trackStats) }
            .onErrorReturn { err -> SummaryResult.LoadStatsResult.createError(err) }
            .startWith(SummaryResult.LoadStatsResult.createLoading())
            .retry() // don't unsubscribe
    }
}
