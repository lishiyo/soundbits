package com.cziyeli.songbits.home

import com.cziyeli.commons.mvibase.MviIntent

/**
 * Shared by opening screen [MainActivity] and home tab.
 *
 * Created by connieli on 12/31/17.
 */
sealed class HomeIntent : MviIntent {

    // opened app
    class Initial : HomeIntent()

    // get /me and save to UserManager
    class FetchUser : HomeIntent()

    // logout
    class LogoutUser : HomeIntent()

    // opened home, already logged in -> load the playlists
    class LoadUserPlaylists(val limit: Int = 20, val offset: Int = 0) : HomeIntent()

    class LoadFeaturedPlaylists(val limit: Int = 20, val offset: Int = 0) : HomeIntent()
}