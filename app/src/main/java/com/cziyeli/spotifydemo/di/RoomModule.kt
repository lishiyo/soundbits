package com.cziyeli.spotifydemo.di

import android.content.Context
import com.cziyeli.data.local.RoomDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by connieli on 12/31/17.
 */
@Module
class RoomModule {
    // provide room datasource (room db)
    @Provides @Singleton fun provideRoomDataSource(context: Context) =
            RoomDataSource.buildPersistentDatabase(context)
}