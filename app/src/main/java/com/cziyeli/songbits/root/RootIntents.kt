package com.cziyeli.songbits.root

import com.cziyeli.commons.mvibase.MviIntent

sealed class RootIntent : MviIntent {
    // fetch all stashed tracks in database
    class FetchAllStashedTracks : RootIntent()
}