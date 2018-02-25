package com.cziyeli.domain.user

import com.cziyeli.domain.base.ChipsActionMarker
import com.cziyeli.domain.playlistcard.CardActionMarker
import com.cziyeli.domain.summary.TrackStatsData

/**
 * Created by connieli on 2/18/18.
 */

interface ProfileActionMarker : CardActionMarker

sealed class ProfileAction : ProfileActionMarker {

    // Reset back to original stats
    class Reset : ProfileAction(), ChipsActionMarker

    // changed a single stat ("tempo" => ("target_tempo", 0.4)
    class StatChanged(val currentMap: TrackStatsData, val stat: Pair<String, Pair<String, Double>>) : ProfileAction()

    // load recommended tracks based on seeds
    // https://beta.developer.spotify.com/documentation/web-api/reference/browse/get-recommendations/
    class FetchRecommendedTracks(val limit: Int = 20,
                                 val seedGenres: List<String>,
                                 val attributes: Map<String, Number> // target_*, min_*, max_*
    ) : ProfileAction()
}
