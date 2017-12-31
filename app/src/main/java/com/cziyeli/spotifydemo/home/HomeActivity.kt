package com.cziyeli.spotifydemo.home

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.cziyeli.commons.*
import com.cziyeli.spotifydemo.R
import com.cziyeli.spotifydemo.di.App
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.ConnectionStateCallback
import com.spotify.sdk.android.player.Error
import kaaes.spotify.webapi.android.SpotifyApi
import kaaes.spotify.webapi.android.models.Album
import retrofit.Callback
import retrofit.RetrofitError
import javax.inject.Inject


/**
 * Main screen:
 * - Show user's playlists
 * - show current liked and discard piles
 *
 * Created by connieli on 12/31/17.
 */
class HomeActivity : AppCompatActivity(), ConnectionStateCallback {

    // Create an API instance. The default instance connects to https://api.spotify.com/.
    @Inject lateinit var api: SpotifyApi

    // in seconds
    private var expirationCutoff: Long by bindSharedPreference(this, LOGIN_EXPIRATION, 0)


    val component by lazy { App.appComponent.plus(HomeModule(this)) }

    private var isLoggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        component.inject(this) // init dagger
    }

    fun isLoggedIn(): Boolean {
        return isLoggedIn || (System.currentTimeMillis() / 1000) < expirationCutoff
    }

    fun onLoginButtonClicked(view: View) {
        if (!isLoggedIn()) {
            Utils.log("Logging in")
            openLoginWindow()
        } else {
            Utils.log("Already logged in")
        }
    }

    fun onTestButtonClicked(view: View) {
        if (!isLoggedIn()) {
            Utils.log("Logging in")
            openLoginWindow()
        } else {
            testApi(api)
        }
    }

    fun testApi(api: SpotifyApi) {
        val spotify = api.service

        spotify.getAlbum("2dIGnmEIy1WZIcZCFSj6i8", object : Callback<Album> {
            override fun success(album: Album?, response: retrofit.client.Response) {
                Utils.log("Album success: $album.name")
            }

            override fun failure(error: RetrofitError) {
                Utils.log("Album failure: $error.toString()")
            }
        })
    }

    //   ____      _ _ _                _      __  __      _   _               _
    //  / ___|__ _| | | |__   __ _  ___| | __ |  \/  | ___| |_| |__   ___   __| |___
    // | |   / _` | | | '_ \ / _` |/ __| |/ / | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
    // | |__| (_| | | | |_) | (_| | (__|   <  | |  | |  __/ |_| | | | (_) | (_| \__ \
    //  \____\__,_|_|_|_.__/ \__,_|\___|_|\_\ |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
    //

    override fun onLoggedIn() {
        Utils.log("Login complete")
        isLoggedIn = true
    }

    override fun onLoggedOut() {
        Utils.log("Logout complete")
        isLoggedIn = false
    }

    override fun onLoginFailed(error: Error) {
        Utils.log("Login error " + error)
        isLoggedIn = false
    }

    override fun onTemporaryError() {
        Utils.log("Temporary error occurred")
    }

    override fun onConnectionMessage(message: String) {
        Utils.log("Incoming connection message: " + message)
    }

    //     _         _   _                _   _           _   _
    //    / \  _   _| |_| |__   ___ _ __ | |_(_) ___ __ _| |_(_) ___  _ __
    //   / _ \| | | | __| '_ \ / _ \ '_ \| __| |/ __/ _` | __| |/ _ \| '_ \
    //  / ___ \ |_| | |_| | | |  __/ | | | |_| | (_| (_| | |_| | (_) | | | |
    // /_/   \_\__,_|\__|_| |_|\___|_| |_|\__|_|\___\__,_|\__|_|\___/|_| |_|
    //

    private fun openLoginWindow() {
        val request = AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID,
                AuthenticationResponse.Type.TOKEN, SPOTIFY_REDIRECT_URI)
                .setScopes(arrayOf("user-read-private", "playlist-read", "playlist-read-private", "streaming"))
                .build()

        AuthenticationClient.openLoginActivity(this, SPOTIFY_REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Check if result comes from the correct activity
        if (requestCode == SPOTIFY_REQUEST_CODE) {
            val response = AuthenticationClient.getResponse(resultCode, intent)
            when (response.type) {
                // Response was successful and contains auth token
                AuthenticationResponse.Type.TOKEN -> onAuthenticationComplete(response)

                // Auth flow returned an error
                AuthenticationResponse.Type.ERROR -> Utils.log("Auth error: " + response.error)

                // Most likely auth flow was cancelled
                else -> Utils.log("Auth result: " + response.type)
            }
        }
    }

    private fun onAuthenticationComplete(authResponse: AuthenticationResponse) {
        isLoggedIn = true
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        api.setAccessToken(authResponse.accessToken)

        // save in shared pref
        val nextExpirationTime = System.currentTimeMillis() / 1000 + authResponse.expiresIn // 1 hour
        expirationCutoff = nextExpirationTime
        Utils.log("Got authentication token: $authResponse.accessToken ++ nextExpirationTime: $nextExpirationTime")
    }

}