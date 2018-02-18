package com.cziyeli.songbits.stash

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.data.Repository
import com.cziyeli.songbits.playlistcard.CardIntentMarker


/**
 * Shared by opening screen [StashFragment].
 *
 * Created by connieli on 12/31/17.
 */
sealed class StashIntent : MviIntent, CardIntentMarker {

    // Swiped to first time
    class InitialLoad : StashIntent()

    class ClearTracks(val pref: Repository.Pref) : StashIntent()

    // load /top tracks
    class FetchUserTopTracks(val limit: Int = 50,
                             val offset: Int = 0,
                             val time_range: String? = "medium_term"
    ) : StashIntent()

    // load recommended tracks based on seeds
    // https://developer.spotify.com/web-api/console/get-recommendations/#complete
    class FetchRecommendedTracks(val limit: Int = 20,
                                 val offset: Int = 0
    ) : StashIntent()
}