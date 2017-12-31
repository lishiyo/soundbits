package com.cziyeli.spotifydemo.home

import android.content.Intent
import android.os.Bundle
import android.os.StrictMode
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.cziyeli.commons.*
import com.cziyeli.spotifydemo.R
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.ConnectionStateCallback
import com.spotify.sdk.android.player.Error
import com.wrapper.spotify.Api
import com.wrapper.spotify.models.Album
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers






/**
 * Main screen:
 * - Show user's playlists
 * - show current liked and discard piles
 *
 * Created by connieli on 12/31/17.
 */
class HomeActivity : AppCompatActivity(), ConnectionStateCallback {

    // Create an API instance. The default instance connects to https://api.spotify.com/.
    private var api: Api? = null

    private var isLoggedIn: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
    }

    fun onLoginButtonClicked(view: View) {
        if (!isLoggedIn) {
            Utils.log("Logging in")
            openLoginWindow()
        } else {
            Utils.log("Already logged in")
        }
    }

    fun onTestButtonClicked(view: View) {
        api?.let{
            testApi(api!!)
        }
    }

    fun testApi(api: Api) {
       Observable.fromCallable { api.getAlbum(TEST_ALBUM_ID).build().async }
               .subscribeOn(Schedulers.io())
               .observeOn(AndroidSchedulers.mainThread())
               .subscribe({ albumFuture ->
                   // Create callbacks in case of success or failure
                   Futures.addCallback(albumFuture, object : FutureCallback<Album> {

                       // Print the genres of the album call is successful
                       override fun onSuccess(album: Album?) {
                          Utils.log("got album: $album")
                       }

                       // In case of failure
                       override fun onFailure(thrown: Throwable) {
                           Utils.log("could not get album")
                       }
                   })
               }, { thrown ->
                   Log.i(TAG, "Could not get albums. ${thrown.localizedMessage}")
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
                AuthenticationResponse.Type.TOKEN,
                SPOTIFY_REDIRECT_URI)
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
        Utils.log("Got authentication token: $authResponse.accessToken")
        api = Api.builder()
                .accessToken(authResponse.accessToken)
                .build()
        api?.let {
            testApi(it)
        }
    }

}