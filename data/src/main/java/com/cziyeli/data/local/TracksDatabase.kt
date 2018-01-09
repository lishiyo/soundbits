package com.cziyeli.data.local

import android.arch.persistence.room.Database
import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context

/**
 * Created by connieli on 12/31/17.
 */
@Database(
        entities = arrayOf(TrackEntity::class),
        version = 1,
        exportSchema = false)
abstract class TracksDatabase : RoomDatabase() {

    abstract fun tracksDao(): TracksDao

    companion object {
        fun buildPersistentDatabase(context: Context): TracksDatabase = Room.databaseBuilder(
                context.applicationContext,
                TracksDatabase::class.java,
                TracksContract.DATABASE_APPLICATION
        ).build()
    }
}