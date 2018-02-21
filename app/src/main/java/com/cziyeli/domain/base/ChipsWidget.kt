package com.cziyeli.domain.base

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.data.Repository
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Action processor for a chips widget.
 * Created by connieli on 2/20/18.
 */
@Singleton
class ChipsActionProcessor @Inject constructor(private val repository: Repository,
                                               private val schedulerProvider: BaseSchedulerProvider) {
    private val TAG = ChipsActionProcessor::class.simpleName
}

interface ChipsActionMarker : MviAction

sealed class ChipsAction : ChipsActionMarker {

}

interface ChipsResultMarker : MviResult

sealed class ChipsResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                         var error: Throwable? = null) : ChipsResultMarker {

}