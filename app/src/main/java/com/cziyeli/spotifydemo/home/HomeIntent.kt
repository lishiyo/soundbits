package com.cziyeli.spotifydemo.home

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
    class LoadPlaylists : HomeIntent() {
        companion object {
            fun create(): LoadPlaylists {
                return LoadPlaylists()
            }
        }
    }

}