package com.cziyeli.songbits.di

import android.content.Context
import com.cziyeli.data.Repository
import com.cziyeli.data.RepositoryImpl
import com.cziyeli.data.local.RoomDataSource
import com.cziyeli.data.remote.RemoteDataSource
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by connieli on 12/31/17.
 */
@Module
class AppModule(private val application: App) {

    @Provides
    @Singleton
    fun provideApp(): App = application

    @Provides
    @Singleton
    fun provideContext(): Context = application

    @Provides
    @Singleton
    fun provideRepository(local: RoomDataSource, remote: RemoteDataSource): Repository {
        return RepositoryImpl(local, remote)
    }


}

