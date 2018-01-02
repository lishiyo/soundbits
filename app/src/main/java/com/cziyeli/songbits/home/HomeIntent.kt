package com.cziyeli.songbits.home

import lishiyo.kotlin_arch.mvibase.MviIntent

/**
 * Created by connieli on 12/31/17.
 */
sealed class HomeIntent : MviIntent {

    // opened home + not logged in yet
    class Initial : HomeIntent() {
        companion object {
            fun create(): Initial {
                return Initial()
            }
        }
    }

    // opened home, already logged in -> load the playlists
    class LoadPlaylists(val limit: Int = 20, val offset: Int = 0) : HomeIntent() {
        companion object {
            fun create(limit: Int = 20, offset: Int = 0): LoadPlaylists {
                return LoadPlaylists(limit, offset)
            }
        }
    }

}