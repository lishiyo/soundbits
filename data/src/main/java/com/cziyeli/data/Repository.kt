package com.cziyeli.data

import com.cziyeli.data.local.TrackEntity
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
    enum class Source {
        LOCAL, REMOTE
    }

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

    // 1 - save track entities to db as liked
    fun saveTracksLocal(tracks: List<TrackEntity>) : List<Long>
}