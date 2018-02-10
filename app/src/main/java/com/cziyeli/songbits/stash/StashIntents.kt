package com.cziyeli.songbits.stash

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.songbits.playlistcard.CardIntentMarker


/**
 * Shared by opening screen [StashFragment].
 *
 * Created by connieli on 12/31/17.
 */
sealed class StashIntent : MviIntent, CardIntentMarker {

    // Swiped to first time
    class InitialLoad : StashIntent()

    // initWith /top tracks
    class FetchTopTracks(val limit: Int = 20,
                         val offset: Int = 0,
                         val fields: String? = null
    ) : StashIntent()

    // initWith recommended tracks based on seeds
    // https://developer.spotify.com/web-api/console/get-recommendations/#complete
    class FetchRecommendedTracks(val limit: Int = 20,
                                 val offset: Int = 0
    ) : StashIntent()
}