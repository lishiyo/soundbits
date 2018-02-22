package com.cziyeli.songbits.root

import com.cziyeli.commons.mvibase.MviIntent

/**
 * Events for teh global state.
 */
sealed class RootIntent : MviIntent {

    // initWith just the counts, not the tracks
    class FetchQuickCounts : RootIntent()

    // initWith all stashed tracks in database
    class LoadAllStashedTracks(val limit: Int = 50,
                               val offset: Int = 0,
                               val fields: String? = null
    ) : RootIntent()

    // initWith all liked stashed in database
    class LoadLikedTracks(val limit: Int = 50,
                          val offset: Int = 0,
                          val fields: String? = null
    ) : RootIntent()

    // initWith all liked stashed in database
    class LoadDislikedTracks(val limit: Int = 50,
                              val offset: Int = 0,
                              val fields: String? = null
    ) : RootIntent()
}