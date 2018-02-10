package com.cziyeli.songbits.stash

import com.cziyeli.commons.mvibase.MviIntent


/**
 * Shared by opening screen [StashFragment].
 *
 * Created by connieli on 12/31/17.
 */
sealed class StashIntent : MviIntent {

    // load likes SimpleCard
    class LoadLikesCard : StashIntent()

    // load dislikes SimpleCard
    class LoadDisLikesCard : StashIntent()

    // load /top tracks
    class LoadTopTracksCard : StashIntent()

    // load recommended tracks based on seeds
    // https://developer.spotify.com/web-api/console/get-recommendations/#complete
    class LoadRecommendedCard(val limit: Int = 20, val offset: Int = 0) : StashIntent()

}