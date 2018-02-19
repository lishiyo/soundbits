package com.cziyeli.domain.user

import com.cziyeli.domain.playlistcard.CardActionMarker

/**
 * Created by connieli on 2/18/18.
 */

interface ProfileActionMarker : CardActionMarker

sealed class ProfileAction : ProfileActionMarker {

    // load recommended tracks based on seeds
    // https://beta.developer.spotify.com/documentation/web-api/reference/browse/get-recommendations/
    class FetchRecommendedTracks(val limit: Int = 20,
                                 val offset: Int = 0,
                                 val attributes: Map<String, Float> // target_*, min_*, max_*
    ) : ProfileAction()
}
