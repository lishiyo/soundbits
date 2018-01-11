package com.cziyeli.data

import com.cziyeli.data.local.TrackEntity
import io.reactivex.Observable
import io.reactivex.Single
import kaaes.spotify.webapi.android.models.*

/**
 * Contract for api requests.
 *
 * Created by connieli on 12/31/17.
 */
interface Repository {
    enum class Source {
        LOCAL, REMOTE
    }

    fun debug(limit: Int = -1)

    fun fetchCurrentUser() : Single<UserPrivate>

    // get current user's playlists
    fun fetchUserPlaylists(source: Repository.Source = Source.REMOTE,
                           limit: Int = 20, offset: Int = 0
    ): Observable<Pager<PlaylistSimple>>

    // get a playlist's com.cziyeli.domain.tracks
    fun fetchPlaylistTracks(source: Repository.Source = Source.REMOTE,
                            ownerId: String,
                            playlistId: String,
                            fields: String?,
                            limit: Int = 100,
                            offset: Int = 0
    ): Observable<Pager<PlaylistTrack>>

    // fetch stats for a list of tracks
    fun fetchTracksStats(source: Repository.Source = Source.REMOTE,
                         trackIds: List<String>
    ) : Observable<AudioFeaturesTracks>

    // Save tracks to database (local only)
    fun saveTracksLocal(tracks: List<TrackEntity>)
}