package com.cziyeli.data.local

import android.arch.persistence.room.*
import io.reactivex.Flowable
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider

/**
 * Dao to access the likes/dislikes database - includes all tracks ever seen.
 *
 * Note that for the methods returning Flowables, any update to the table triggers emission,
 * so use [distinctUntilChanged] (https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1).
 */
@Dao
abstract class TracksDao {

    @Query("SELECT * FROM Tracks WHERE track_id = :trackId")
    abstract fun getTrackByTrackId(trackId: String): Flowable<TrackEntity>

    @Query("SELECT * FROM Tracks WHERE track_id IN(:trackIds) AND cleared = 0 AND track_id > :offset LIMIT :limit")
    abstract fun getTracksForTrackIds(trackIds: List<String>, limit: Int, offset: Int): Flowable<List<TrackEntity>>

    // get non-cleared tracks for single playlist
    @Query("SELECT * FROM Tracks WHERE playlist_id = :playlistId AND cleared = 0 AND track_id > :offset LIMIT :limit")
    abstract fun getVisibleTracksByPlaylistId(playlistId: String, limit: Int, offset: Int): Flowable<List<TrackEntity>>

    // get total non-cleared tracks count
    @Query("SELECT COUNT(*) FROM Tracks WHERE cleared = 0")
    abstract fun getStashedTracksCount(): Flowable<Int>

    @Query("SELECT COUNT(*) FROM Tracks WHERE cleared = 0 AND liked = 1")
    abstract fun getStashedTracksLikedCount(): Flowable<Int>

    @Query("SELECT COUNT(*) FROM Tracks WHERE cleared = 0 AND liked = 0")
    abstract fun getStashedTracksDislikedCount(): Flowable<Int>

    // get all non-cleared in database
    @Query("SELECT * FROM Tracks WHERE cleared = 0 AND track_id > :offset LIMIT :limit")
    abstract fun getVisibleTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>>

    // get all visible and liked
    @Query("SELECT * FROM Tracks WHERE cleared = 0 AND liked = 1 AND track_id > :offset LIMIT :limit")
    abstract fun getLikedTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>>

    // get all visible and disliked
    @Query("SELECT * FROM Tracks WHERE cleared = 0 AND liked = 0  AND track_id > :offset LIMIT :limit")
    abstract fun getDislikedTracks(limit: Int, offset: Int): Flowable<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTracks(tracks: List<TrackEntity>)

    @Update()
    abstract fun updateTrack(track: TrackEntity)

    @Query("UPDATE Tracks SET liked = :liked WHERE track_id = :trackId")
    abstract fun updateTrackPref(trackId: String, liked: Boolean): Int

    @Delete
    abstract fun deleteTrack(track: TrackEntity)

    // "wipe" out by changing visibility
    @Query("UPDATE Tracks SET cleared = 1 WHERE liked = :liked")
    abstract fun clearTracks(liked: Boolean)

    @Query("UPDATE Tracks SET cleared = 1")
    abstract fun clearAllTracks()

    /////////////////////////////////////
    /////////////// DEBUGGING ///////////
    /////////////////////////////////////

    @Query("DELETE FROM Tracks")
    abstract fun nuke() // wipe out the database!

    @Query("SELECT * FROM Tracks")
    protected abstract fun queryAll(): Flowable<List<TrackEntity>>

    fun queryAllDistinct(): Flowable<List<TrackEntity>> {
        return queryAll().distinctUntilChanged().subscribeOn(SchedulerProvider.io())
    }
}

