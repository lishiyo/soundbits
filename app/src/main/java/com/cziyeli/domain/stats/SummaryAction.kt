package com.cziyeli.domain.stats

import lishiyo.kotlin_arch.mvibase.MviAction

/**
 * Created by connieli on 1/7/18.
 */
sealed class SummaryAction : MviAction {
    object None : SummaryAction()

    class LoadStats(val trackIds: List<String>) : SummaryAction() {
        companion object {
            fun create(trackIds: List<String>) : LoadStats {
                return LoadStats(trackIds)
            }
        }
    }
}