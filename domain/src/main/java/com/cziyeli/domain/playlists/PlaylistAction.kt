package com.cziyeli.domain.playlists

import lishiyo.kotlin_arch.mvibase.MviAction

/**
 * Created by connieli on 12/31/17.
 */
sealed class PlaylistAction : MviAction {

    // fetch current user's playlists (default to first page)
    class UserPlaylists(val limit: Int = 20, val offset: Int = 0) : PlaylistAction() {
        // limit, page
        companion object {
            fun create(limit: Int = 20, offset: Int = 0): UserPlaylists {
                return UserPlaylists(limit, offset)
            }
        }
    }

    class None : PlaylistAction() {
        companion object {
            fun create(): None {
                return None()
            }
        }
    }

    // get recommendations by seed:
    // https://developer.spotify.com/web-api/console/get-recommendations/

    // get category
    // https://developer.spotify.com/web-api/console/get-category-playlists/

    // get featured
    // https://developer.spotify.com/web-api/console/get-featured-playlists/
}