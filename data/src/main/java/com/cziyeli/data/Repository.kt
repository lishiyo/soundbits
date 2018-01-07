package com.cziyeli.data

import io.reactivex.Observable
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks
import kaaes.spotify.webapi.android.models.Pager
import kaaes.spotify.webapi.android.models.PlaylistSimple
import kaaes.spotify.webapi.android.models.PlaylistTrack

/**
 * Contract for api requests.
 *
 * Created by connieli on 12/31/17.
 */
interface Repository {

    // get current user's playlists
    fun fetchUserPlaylists(limit: Int = 20, offset: Int = 0): Observable<Pager<PlaylistSimple>>

    // get a playlist's com.cziyeli.domain.tracks
    fun fetchPlaylistTracks(ownerId: String,
                            playlistId: String,
                            fields: String?,
                            limit: Int = 100,
                            offset: Int = 0): Observable<Pager<PlaylistTrack>>

    // fetch stats for a list of tracks
    fun fetchTracksData(trackIds: List<String>) : Observable<AudioFeaturesTracks>
}