package com.cziyeli.songbits.cards.summary

import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Created by connieli on 1/6/18.
 */
sealed class SummaryIntent : MviIntent {


    // calculate stats for a bunch of tracks => transform to domain model TrackListStats
    class LoadStats(val trackIds: List<String>) : SummaryIntent() {
        companion object {
            fun create(trackIds: List<String>) : LoadStats {
                return LoadStats(trackIds)
            }
        }
    }

    // create pending PlaylistCard on open <- return PlaylistStats

    // create playlist out of liked -> hit db and spotify api

    // add to an existing playlist -> hit db and spotify

    // save liked and keep surfing other playlists -> hit db

    // review Discard pile

}