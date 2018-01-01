package com.cziyeli.spotifydemo.di

import com.cziyeli.data.remote.RemoteDataSource
import dagger.Module
import dagger.Provides
import kaaes.spotify.webapi.android.SpotifyApi
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Singleton

/**
 * instantiate api modules
 *
 * Created by connieli on 12/31/17.
 */
@Module
class RemoteModule {

    @Provides
    @Singleton
    fun provideRemoteDataSource(api: SpotifyApi, schedulerProvider: BaseSchedulerProvider): RemoteDataSource
            = RemoteDataSource(api, schedulerProvider)

    @Provides
    @Singleton
    fun provideSpotifyApi(): SpotifyApi {
        // check if logged in
        return SpotifyApi()
    }

}