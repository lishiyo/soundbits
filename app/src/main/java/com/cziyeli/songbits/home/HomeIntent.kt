package com.cziyeli.songbits.home

import com.cziyeli.commons.mvibase.MviIntent

/**
 * Created by connieli on 12/31/17.
 */
sealed class HomeIntent : MviIntent {

    // opened home + not logged in yet
    class Initial : HomeIntent()

    // opened home, already logged in -> load the items
    class LoadPlaylists(val limit: Int = 20, val offset: Int = 0) : HomeIntent()

    // get /me and save to UserManager
    class FetchUser : HomeIntent()
}