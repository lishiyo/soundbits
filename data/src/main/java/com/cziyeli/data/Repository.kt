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
    enum class Pref {
        LIKED, DISLIKED, ALL
    }
    enum class Source {
        LOCAL, REMOTE
    }

    fun debug(limit: Int = -1)

    // ============ ROOT, HOME ============

    fun fetchCurrentUser() : Single<UserPrivate>

    // get current user's playlists by default sort order
    fun fetchUserPlaylists(source: Repository.Source = Source.REMOTE,
                           limit: Int = 20, offset: Int = 0
    ): Observable<Pager<PlaylistSimple>>

    fun fetchFeaturedPlaylists(source: Repository.Source = Source.REMOTE,
                               limit: Int = 20, offset: Int = 0
    ): Observable<Pager<PlaylistSimple>>

    fun fetchUserQuickStats() : Flowable<Triple<Int, Int, Int>>

    // ========= STASH ==========

    // fetch all liked or disliked tracks from stash
    fun fetchUserTracks(pref: Pref = Pref.ALL, limit: Int = 20, offset: Int = 0) : Flowable<List<TrackEntity>>

    // clear all liked or disliked tracks from stash
    fun clearStashedTracks(pref: Pref = Pref.ALL)

    // get user's top tracks from remote
    fun fetchUserTopTracks(source: Repository.Source = Source.REMOTE,
                           time_range: String? = "medium_term",
                           limit: Int = 20, offset: Int = 0
    ): Observable<Pager<Track>>

    // ============ SINGLE PLAYLIST ACTIONS ============

    // get a playlist's tracks
    fun fetchPlaylistTracks(source: Repository.Source = Source.REMOTE,
                            ownerId: String,
                            playlistId: String,
                            fields: String? = null,
                            limit: Int = 40,
                            offset: Int = 0
    ): Observable<Pager<PlaylistTrack>>

    // get all a playlist's tracks from database
    fun fetchPlaylistStashedTracks(source: Repository.Source = Source.LOCAL,
                                   playlistId: String,
                                   fields: String? = null,
                                   limit: Int = 40,
                                   offset: Int = 0
    ): Flowable<List<TrackEntity>>

    fun fetchStashedTracksByIds(source: Repository.Source = Source.LOCAL,
                                trackIds: List<String>,
                                fields: String? = null,
                                limit: Int = 40,
                                offset: Int = 0
    ): Flowable<List<TrackEntity>>

    // ============ TRACKS ============

    // update a track's liked/disliked status in db - returns number of rows affected
    fun updateTrackPref(track: TrackEntity) : Int

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

    // Save all swiped tracks to database (local only)
    fun saveTracksLocal(tracks: List<TrackEntity>)

    // Save single trakc
    fun saveTrackLocal(track: TrackEntity)
}