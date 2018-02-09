package com.cziyeli.songbits.root

import com.cziyeli.commons.mvibase.MviIntent

sealed class RootIntent : MviIntent {

    // just the counts, not the tracks
    class FetchUserQuickCounts : RootIntent()

    // fetch all stashed tracks in database
    class FetchAllStashedTracks : RootIntent()
}