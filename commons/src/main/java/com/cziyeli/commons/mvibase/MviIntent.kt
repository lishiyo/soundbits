package com.cziyeli.commons.mvibase

/**
 * Immutable object which represent any event ('user clicked on view') or programmatic events ('screen opened').
 */
interface MviIntent

/**
 * Marks an [MviIntent] that should only be triggered once through the lifecycle of its owner, unless certain parameters change.
 */
interface SingleEventIntent : MviIntent {
    /**
     * Whether to allow the event to go through anyways (ex same intent used for pagination).
     */
    fun shouldRefresh() : Boolean = false
}