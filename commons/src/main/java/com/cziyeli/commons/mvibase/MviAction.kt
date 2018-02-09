package com.cziyeli.commons.mvibase

/**
 * Immutable object which contains all the required information for a business logic to process.
 */
interface MviAction

/**
 * General stand-in for 'no-op'.
 */
object None : MviAction