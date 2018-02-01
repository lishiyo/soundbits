package com.cziyeli.domain.playlists

import com.cziyeli.commons.mvibase.MviAction

/**
 * Marker for events on the home screen.
 */
interface HomeAction : MviAction

/**
 * Events for playlists on the home screen.
 */
sealed class PlaylistsAction : HomeAction {
    // no-op
    object None : PlaylistsAction()

    // fetch current user's playlists (default to recent, first page)
    class UserPlaylists(val limit: Int = 20, val offset: Int = 0) : PlaylistsAction()

    // get recommendations by seed:
    // https://developer.spotify.com/web-api/console/get-recommendations/

    // get category
    // https://developer.spotify.com/web-api/console/get-category-playlists/

    // get featured
    // https://developer.spotify.com/web-api/console/get-featured-playlists/
}