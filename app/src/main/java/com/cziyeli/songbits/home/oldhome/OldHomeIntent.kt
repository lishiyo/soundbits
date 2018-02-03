package com.cziyeli.songbits.home.oldhome

import com.cziyeli.commons.mvibase.MviIntent

/**
 * Created by connieli on 12/31/17.
 */
sealed class OldHomeIntent : MviIntent {

    // opened home + not logged in yet
    class Initial : OldHomeIntent()

    // opened home, already logged in -> load the items
    class LoadPlaylists(val limit: Int = 20, val offset: Int = 0) : OldHomeIntent()

    // get /me and save to UserManager
    class FetchUser : OldHomeIntent()

    // logout
    class LogoutUser : OldHomeIntent()
}