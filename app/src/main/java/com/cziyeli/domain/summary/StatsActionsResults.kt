package com.cziyeli.domain.summary

import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.PlaylistCardActionMarker
import com.cziyeli.domain.playlistcard.PlaylistCardResultMarker


/**
 * Marker interface for stats.
 */
interface StatsActionMarker : MviAction
interface StatsResultMarker : MviResult

/**
 * Actions related to track stats.
 */
sealed class StatsAction : StatsActionMarker, SummaryActionMarker, PlaylistCardActionMarker {

    // generic fetch stats for a list of tracks
    class FetchStats(val trackIds: List<String>) : StatsAction()
}

/**
 * Results for the track stats widget
 */
sealed class TrackStatsResult : StatsResultMarker, PlaylistCardResultMarker {
    
    class FetchStats(val trackIds: List<String>) : TrackStatsResult()
}