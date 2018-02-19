package com.cziyeli.songbits.root

import com.cziyeli.commons.mvibase.MviIntent

sealed class RootIntent : MviIntent {

    // initWith just the counts, not the tracks
    class FetchUserQuickCounts : RootIntent()

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