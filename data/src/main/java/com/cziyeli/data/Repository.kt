package com.cziyeli.data

import com.cziyeli.data.local.TrackEntity
import io.reactivex.Flowable
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

    // ============ ROOT and HOME ============

    fun fetchCurrentUser() : Single<UserPrivate>

    // get current user's playlists by default sort order
    fun fetchUserPlaylists(source: Repository.Source = Source.REMOTE,
                           limit: Int = 20, offset: Int = 0
    ): Observable<Pager<PlaylistSimple>>

    // ============ SINGLE PLAYLIST ACTIONS ============

    // get a playlist's tracks
    fun fetchPlaylistTracks(source: Repository.Source = Source.REMOTE,
                            ownerId: String,
                            playlistId: String,
                            fields: String?,
                            limit: Int = 100,
                            offset: Int = 0
    ): Observable<Pager<PlaylistTrack>>

    // get all a playlist's tracks from database
    fun fetchPlaylistStashedTracks(source: Repository.Source = Source.LOCAL,
                                   playlistId: String,
                                   fields: String? = null,
                                   limit: Int = 100,
                                   offset: Int = 0
    ): Flowable<List<TrackEntity>>

    // ============ STATS ============

    // fetch stats for a list of tracks
    fun fetchTracksStats(source: Repository.Source = Source.REMOTE,
                         trackIds: List<String>
    ) : Observable<AudioFeaturesTracks>

    // ============ SUMMARY ============

    // @POST("/users/{user_id}/playlists")
    fun createPlaylist(ownerId: String,
                       name: String,
                       description: String?, // optional description
                       public: Boolean = false) : Single<Playlist>

    // @POST("/users/{user_id}/playlists/{playlist_id}/tracks")
    fun addTracksToPlaylist(ownerId: String, playlistId: String, trackUris: List<String>) : Observable<Pair<String, SnapshotId>>

    // Save tracks to database (local only)
    fun saveTracksLocal(tracks: List<TrackEntity>)
}