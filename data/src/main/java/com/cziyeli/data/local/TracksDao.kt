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

    @Query("SELECT * FROM Tracks WHERE id = :id")
    abstract fun getTrackById(id: String): Flowable<TrackEntity> // local id

    @Query("SELECT * FROM Tracks WHERE track_id = :trackId")
    abstract fun getTrackByTrackId(trackId: String): Flowable<TrackEntity>

    @Query("SELECT * FROM Tracks WHERE track_id = :playlistId")
    abstract fun getTracksByPlaylistId(playlistId: String): Flowable<List<TrackEntity>>

    @Query("SELECT * FROM Tracks WHERE cleared = 0 LIMIT :limit")
    abstract fun getVisibleTracks(limit: Int): Flowable<List<TrackEntity>>

    @Query("SELECT * FROM Tracks WHERE cleared = 0 AND liked = 1 LIMIT :limit")
    abstract fun getLikedTracks(limit: Int): Flowable<List<TrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveTrack(track: TrackEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract fun saveTracks(tracks: List<TrackEntity>)

    @Update()
    abstract fun updateTrack(track: TrackEntity)

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

