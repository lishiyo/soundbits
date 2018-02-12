package com.cziyeli.domain.playlists

import com.cziyeli.commons.mvibase.MviAction

/**
 * Marker for events on the home screen.
 */
interface HomeActionMarker : MviAction

/**
 * Events for playlists on the home screen.
 */
sealed class PlaylistsAction : HomeActionMarker {
    // fetch current user's playlists (default to recent, first page)
    class UserPlaylists(val limit: Int = 20, val offset: Int = 0) : PlaylistsAction()

    // get featured
    // https://developer.spotify.com/web-api/console/get-featured-playlists/
    class FeaturedPlaylists(val limit: Int = 20, val offset: Int = 0) : PlaylistsAction()

    // get recommendations by seed:
    // https://developer.spotify.com/web-api/console/get-recommendations/

    // get category's playlists
    // https://developer.spotify.com/web-api/console/get-category-playlists/

}