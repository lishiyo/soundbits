package com.cziyeli.soundbits.home

import com.cziyeli.commons.mvibase.MviIntent
import com.cziyeli.commons.mvibase.SingleEventIntent

/**
 * Shared by opening screen [MainActivity] and home tab.
 *
 * Created by connieli on 12/31/17.
 */
sealed class HomeIntent : MviIntent {

    // opened app
    class Initial : HomeIntent(), SingleEventIntent

    // get /me and save to UserManager
    class FetchUser : HomeIntent(), SingleEventIntent

    // fetch the quick counts for the user mini card
    class FetchQuickCounts : HomeIntent(), SingleEventIntent

    // logout
    class LogoutUser : HomeIntent()

    // opened home, already logged in -> load the playlists
    class LoadUserPlaylists(val limit: Int = 20, val offset: Int = 0) : HomeIntent(), SingleEventIntent {
        override fun shouldRefresh() : Boolean {
            return offset != 0
        }
    }

    // load featured
    class LoadFeaturedPlaylists(val limit: Int = 20, val offset: Int = 0) : HomeIntent(), SingleEventIntent {
        override fun shouldRefresh() : Boolean {
            return offset != 0
        }
    }
}