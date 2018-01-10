package com.cziyeli.data.local

import android.arch.persistence.room.*
import io.reactivex.Flowable

/**
 * Dao to access the likes/dislikes database - includes all tracks ever seen.
 *
 * Created by connieli on 12/31/17.
 */
@Dao
interface TracksDao {

    @Query("SELECT * FROM Tracks WHERE id = :id")
    fun getTrackById(id: String): Flowable<TrackEntity> // local id

    @Query("SELECT * FROM Tracks WHERE track_id = :trackId")
    fun getTrackByTrackId(trackId: String): Flowable<TrackEntity>

    @Query("SELECT * FROM Tracks WHERE track_id = :playlistId")
    fun getTracksByPlaylistId(playlistId: String): Flowable<List<TrackEntity>>

    @Query("SELECT * FROM Tracks WHERE cleared = 0 LIMIT :limit")
    fun getVisibleTracks(limit: Int): Flowable<List<TrackEntity>>

    @Query("SELECT * FROM Tracks WHERE cleared = 0 AND liked = 1 LIMIT :limit")
    fun getLikedTracks(limit: Int): Flowable<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveTrack(track: TrackEntity): Long // return row of the insertion

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun saveTracks(tracks: List<TrackEntity>): List<Long>

    @Update()
    fun updateTrack(track: TrackEntity): Int // returns total num affected

    @Delete
    fun deleteTrack(track: TrackEntity): Int // shouldn't need this


    /////////////////////////////////////
    /////////////// DEBUGGING ///////////
    /////////////////////////////////////

    @Query("DELETE FROM Tracks")
    fun nuke(): Int // wipe out the database!

    @Query("SELECT * FROM Tracks")
    fun queryAll(): Flowable<List<TrackEntity>>
}

