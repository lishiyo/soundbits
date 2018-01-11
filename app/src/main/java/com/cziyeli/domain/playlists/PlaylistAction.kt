package com.cziyeli.domain.playlists

import lishiyo.kotlin_arch.mvibase.MviAction

/**
 * Created by connieli on 12/31/17.
 */
interface HomeAction : MviAction

sealed class PlaylistAction : HomeAction {
    // no-op
    object None : PlaylistAction()

    // fetch current user's playlists (default to first page)
    class UserPlaylists(val limit: Int = 20, val offset: Int = 0) : PlaylistAction()

    // get recommendations by seed:
    // https://developer.spotify.com/web-api/console/get-recommendations/

    // get category
    // https://developer.spotify.com/web-api/console/get-category-playlists/

    // get featured
    // https://developer.spotify.com/web-api/console/get-featured-playlists/
}