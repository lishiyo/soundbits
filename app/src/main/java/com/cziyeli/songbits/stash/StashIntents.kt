package com.cziyeli.songbits.stash

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.SingleEventIntent
import com.cziyeli.data.Repository
import com.cziyeli.songbits.playlistcard.CardIntentMarker


/**
 * Shared by opening screen [StashFragment].
 *
 * Created by connieli on 12/31/17.
 */
sealed class StashIntent : MviIntent, CardIntentMarker {

    // Swiped to first time
    class InitialLoad : StashIntent(), SingleEventIntent

    class LoadLikedTracks : StashIntent(), SingleEventIntent

    class LoadDislikedTracks : StashIntent(), SingleEventIntent

    // Clear all likes/dislikes from database
    class ClearTracks(val pref: Repository.Pref) : StashIntent()

    // Load /top tracks from remote
    class FetchUserTopTracks(val limit: Int = 50,
                             val offset: Int = 0,
                             val time_range: String? = "medium_term"
    ) : StashIntent(), SingleEventIntent
}