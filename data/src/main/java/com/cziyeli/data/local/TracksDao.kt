package com.cziyeli.data.local

import android.arch.persistence.room.*
import io.reactivex.Flowable
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider

/**
 * Dao to access the likes/dislikes database - includes all tracks ever seen.
 *
 * Created by connieli on 12/31/17.
 */
@Dao
abstract class TracksDao {

    @Query("SELECT * FROM Tracks WHERE track_id = :trackId")
    abstract fun getTrackByTrackId(trackId: String): Flowable<TrackEntity>

    @Query("SELECT * FROM Tracks WHERE track_id IN :trackIds")
    abstract fun getTracksForTrackIds(trackIds: List<String>): Flowable<TrackEntity>

    // includes non-visible (i.e. cleared) tracks
    @Query("SELECT * FROM Tracks WHERE playlist_id = :playlistId")
    abstract fun getTracksByPlaylistId(playlistId: String): Flowable<List<TrackEntity>>

    // only non-cleared tracks for playlist
    @Query("SELECT * FROM Tracks WHERE playlist_id = :playlistId AND cleared = 0")
    abstract fun getVisibleTracksByPlaylistId(playlistId: String): Flowable<List<TrackEntity>>

    // get all visible
    @Query("SELECT * FROM Tracks WHERE cleared = 0 LIMIT :limit")
    abstract fun getVisibleTracks(limit: Int): Flowable<List<TrackEntity>>

    @Query("SELECT COUNT(*) FROM Tracks WHERE cleared = 0")
    abstract fun getStashedTracksCount(): Flowable<Int>
    @Query("SELECT COUNT(*) FROM Tracks WHERE cleared = 0 AND liked = 1")
    abstract fun getStashedTracksLikedCount(): Flowable<Int>
    @Query("SELECT COUNT(*) FROM Tracks WHERE cleared = 0 AND liked = 0")
    abstract fun getStashedTracksDislikedCount(): Flowable<Int>

    // get all visible and liked
    @Query("SELECT * FROM Tracks WHERE cleared = 0 AND liked = 1 LIMIT :limit")
    abstract fun getLikedTracks(limit: Int): Flowable<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTracks(tracks: List<TrackEntity>)

    @Update()
    abstract fun updateTrack(track: TrackEntity)

    @Query("UPDATE Tracks SET liked = :liked  WHERE track_id = :trackId")
    abstract fun updateTrackPref(trackId: String, liked: Boolean)

    @Delete
    abstract fun deleteTrack(track: TrackEntity)

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

