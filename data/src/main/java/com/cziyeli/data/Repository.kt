package com.cziyeli.data

import io.reactivex.Observable
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlaylistSimple

/**
 * Created by connieli on 12/31/17.
 */
interface Repository {

    // get current user's playlists
    fun fetchUserPlaylists(limit: Int = 20, offset: Int = 0): Observable<Pager<PlaylistSimple>>
}