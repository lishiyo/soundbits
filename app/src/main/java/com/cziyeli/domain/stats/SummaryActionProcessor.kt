package com.cziyeli.domain.stats

import com.cziyeli.data.Repository
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
}
