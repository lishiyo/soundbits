package com.cziyeli.spotifydemo.di

import dagger.Module
import dagger.Provides
import kaaes.spotify.webapi.android.SpotifyApi
import javax.inject.Singleton

/**
 * instantiate api modules
 *
 * Created by connieli on 12/31/17.
 */
@Module
class RemoteModule {

    // provide spotify api (log in)

    @Provides
    @Singleton
    fun provideSpotifyApi(): SpotifyApi {
        // check if logged in
        return SpotifyApi()
    }


//    private fun isLoggedIn(): Boolean {
//        var currentlyLoggedIn = isLoggedIn
//        api.service.getMe(object: Callback<UserPrivate> {
//            override fun failure(error: RetrofitError?) {
//                Utils.log("failure: ${error?.localizedMessage}")
//                currentlyLoggedIn = false
//            }
//
//            override fun success(t: UserPrivate?, response: Response?) {
//                currentlyLoggedIn = true
//            }
//
//        })
//    }


    // provide RemoteSpotifyService (wrapper around spotify web api)
}