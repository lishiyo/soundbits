package com.cziyeli.songbits.di

import android.content.Context
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.data.local.TracksDao
import com.cziyeli.data.local.TracksDatabase
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Local data sources.
 *
 * Created by connieli on 12/31/17.
 */
@Module
class RoomModule {

    @Provides
    @Singleton
    fun provideTracksDatabase(@ForApplication context: Context) = TracksDatabase.buildPersistentDatabase(context)

    @Provides
    @Singleton
    fun providesTracksDao(database: TracksDatabase) : TracksDao {
        return database.tracksDao()
    }

}