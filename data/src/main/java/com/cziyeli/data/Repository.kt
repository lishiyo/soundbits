package com.cziyeli.data

import io.reactivex.Flowable
import kaaes.spotify.webapi.android.models.PlaylistSimple

/**
 * Created by connieli on 12/31/17.
 */
interface Repository {

    // get user's playlists
    fun getPlaylists(): Flowable<List<PlaylistSimple>>
}