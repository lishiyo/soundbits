package com.cziyeli.data.local

import android.arch.persistence.room.Dao

/**
 * Dao to access the likes/dislikes database - includes all tracks ever seen.
 *
 * Created by connieli on 12/31/17.
 */
@Dao
interface RoomDao {

    @Query("SELECT * FROM Tracks WHERE id = :id")
    fun getTrackById(id: String): Flowable<TrackEntity> // local id

    @Query("SELECT * FROM Tracks WHERE track_id = :trackId")
    fun getTrackByTrackId(trackId: String): Flowable<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTrack(track: TrackEntity): Long // return row of the insertion

    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertTracks(tracks: List<TrackEntity>): List<Long>

    @Update()
    fun updateTrack(track: TrackEntity): Int // returns total num affected

    @Delete
    fun deleteTrack(track: TrackEntity): Int // shouldn't need this

//    @Query(RoomContract.SELECT_CURRENCIES_COUNT)
//    fun getCurrenciesTotal(): Flowable<Int>
//
//    @Insert
//    fun insertAll(currencies: List<CurrencyEntity>)
//
//    @Query(RoomContract.SELECT_CURRENCIES)
//    fun getAllCurrencies(): Flowable<List<CurrencyEntity>>

}

