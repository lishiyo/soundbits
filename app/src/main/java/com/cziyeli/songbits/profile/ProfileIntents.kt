package com.cziyeli.songbits.profile

import com.cziyeli.data.Repository
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.playlistcard.CardIntentMarker

/**
 * Created by connieli on 2/18/18.
 */

interface ProfileIntentMarker : CardIntentMarker

sealed class ProfileIntent : ProfileIntentMarker {

    // fetch initial liked stats
    class LoadOriginalStats(val trackModels: List<TrackModel>,  val pref: Repository.Pref = Repository.Pref.LIKED) : ProfileIntent()

    // load recommended tracks based on seeds
    // https://beta.developer.spotify.com/documentation/web-api/reference/browse/get-recommendations/
    class FetchRecommendedTracks(val limit: Int = 20,
                                 val offset: Int = 0,
                                 val attributes: Map<String, Float> // target_*, min_*, max_*
    ) : ProfileIntent()
}