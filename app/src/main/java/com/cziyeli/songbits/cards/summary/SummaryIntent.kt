package com.cziyeli.songbits.cards.summary

import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Events in the summary layout.
 *
 * Created by connieli on 1/6/18.
 */
sealed class SummaryIntent : MviIntent {


    // TODO: what do I need to create pending PlaylistCard on open
    // fetch audio features for a bunch of tracks => transform to domain model TrackListStats
    class LoadStats(val trackIds: List<String>) : SummaryIntent() {
        companion object {
            fun create(trackIds: List<String>) : LoadStats {
                return LoadStats(trackIds)
            }
        }
    }

    // create playlist out of liked -> hit db and spotify api

    // add to an existing playlist -> hit db and spotify

    // save liked and keep surfing other playlists -> hit db

    // review Disliked pile ->

}