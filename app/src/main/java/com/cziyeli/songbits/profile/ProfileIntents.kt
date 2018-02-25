package com.cziyeli.songbits.profile

import com.cziyeli.commons.mvibase.SingleEventIntent
import com.cziyeli.data.Repository
import com.cziyeli.domain.summary.TrackStatsData
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.songbits.base.ChipsIntentMarker
import com.cziyeli.songbits.playlistcard.CardIntentMarker

/**
 * Created by connieli on 2/18/18.
 */

interface ProfileIntentMarker : CardIntentMarker

sealed class ProfileIntent : ProfileIntentMarker, ChipsIntentMarker {

    // grab likes from root
    class LoadTracksForOriginalStats: ProfileIntent(), SingleEventIntent

    // fetch initial liked stats
    class LoadOriginalStats(val trackModels: List<TrackModel>,  val pref: Repository.Pref = Repository.Pref.LIKED)
        : ProfileIntent(), SingleEventIntent

    // changed a single stat ("tempo" => ("target_tempo", 0.4)
    class StatChanged(val currentMap: TrackStatsData, val stat: Pair<String, Pair<String, Double>>) : ProfileIntent()

    // load recommended tracks based on stats
    // https://beta.developer.spotify.com/documentation/web-api/reference/browse/get-recommendations/
    class FetchRecommendedTracks(val limit: Int = 20,
                                 val seedGenres: List<String>,
                                 val attributes: Map<String, Number> // target_*, min_*, max_*
    ) : ProfileIntent()

    // Reset back to original stats
    class Reset : ProfileIntent()

    // logout
    class LogoutUser : ProfileIntent()
}