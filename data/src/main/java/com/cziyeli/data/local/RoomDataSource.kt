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
        version = 1)
abstract class RoomDataSource : RoomDatabase() {

    abstract fun tracksDao(): RoomDao

    companion object {
        fun buildPersistentDatabase(context: Context): RoomDataSource = Room.databaseBuilder(
                context.applicationContext,
                RoomDataSource::class.java,
                RoomContract.DATABASE_APPLICATION
        ).build()
    }
}