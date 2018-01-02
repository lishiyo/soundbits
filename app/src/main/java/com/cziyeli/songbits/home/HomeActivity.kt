package com.cziyeli.songbits.home

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.cziyeli.commons.*
import com.cziyeli.songbits.R
import com.cziyeli.songbits.di.App
import com.cziyeli.songbits.home.di.HomeModule
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.ConnectionStateCallback
import com.spotify.sdk.android.player.Error
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.activity_home.*
import lishiyo.kotlin_arch.mvibase.MviView
import javax.inject.Inject


/**
 * Main screen:
 * - Show user's playlists
 * - show current liked and discard piles
 *
 * Created by connieli on 12/31/17.
 */
class HomeActivity : AppCompatActivity(), ConnectionStateCallback, MviView<HomeIntent, HomeViewState> {

    // dagger
    private val component by lazy { App.appComponent.plus(HomeModule()) }
    @Inject lateinit var api: SpotifyApi

    // check if logged in by shared prefs and in-memory
    private var expirationCutoff: Long by bindSharedPreference(this, LOGIN_EXPIRATION, 0)
    private var accessToken: String by bindSharedPreference(this, AUTH_TOKEN, "")
    private var isLoggedIn: Boolean = false

    // view models
    private lateinit var viewModel: HomeViewModel

    // subviews
    private lateinit var playlistsAdapter: InfinitePlaylistsAdapter

    // intents
    private val mLoadPublisher = PublishSubject.create<HomeIntent.LoadPlaylists>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        component.inject(this) // init dagger

        // init the playlists view
        playlistsAdapter = InfinitePlaylistsAdapter(playlists_container)
        playlists_container.setLoadMoreResolver(playlistsAdapter)

        // bind the view model after all views are done
        initViewModel()

        if (isLoggedIn()) {
            api.setAccessToken(accessToken)
            mLoadPublisher.onNext(HomeIntent.LoadPlaylists.create())
        }
    }

    override fun intents(): Observable<out HomeIntent> {
        return Observable.merge(
                Observable.just(HomeIntent.Initial.create()), // send out initial intent immediately
                mLoadPublisher, // own load intent
                playlistsAdapter.intents() // subviews intents
        )
    }

    override fun render(state: HomeViewState) {
        Utils.log("RENDER ++ state: ${state.status}")

        val loginVisibility= if (state.status == HomeViewState.Status.NOT_LOGGED_IN) View.VISIBLE else View.GONE
        login_button.visibility = loginVisibility

        // render subviews
        playlistsAdapter.render(state)
    }

    private fun isLoggedIn(): Boolean {
        return !accessToken.isEmpty() && (isLoggedIn || (System.currentTimeMillis() / 1000) < expirationCutoff)
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this).get(HomeViewModel::class.java)

        // add viewmodel as an observer of this fragment lifecycle
        viewModel.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states with LiveData, not Rx
        viewModel.states().observe(this, Observer { state ->
            state?.let {
                this.render(state)
            }
        })

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModel.processIntents(intents())

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
            Utils.log("Logging in first")
            openLoginWindow()
        } else {
            mLoadPublisher.onNext(HomeIntent.LoadPlaylists.create())
        }
    }

//    fun testUserPlaylists(authedApi: SpotifyApi) {
//        // get all the user's playlists
//        authedApi.service.getMyPlaylists(object : Callback<Pager<PlaylistSimple>> {
//            override fun success(pagedResponse: Pager<PlaylistSimple>?, response: Response?) {
//                Utils.log("got playlists! total: $pagedResponse.total")
////                pagedResponse?.items?.take(5)?.forEach {
////                    Utils.log("playlist id: ${it.id} ++ ${it.name} ++ ${it.owner}")
////                }
//
//                pagedResponse?.items?.get(0)?.let {
//                    testPlaylistTracks(authedApi, it.owner.id, it.id)
//                }
//            }
//
//            override fun failure(error: RetrofitError?) {
//                Utils.log("fetch playlists error: ${error?.localizedMessage}")
//            }
//        })
//    }
//
//    fun testPlaylistTracks(authedApi: SpotifyApi, ownerId: String, playlistId: String) {
//        // get all the com.cziyeli.domain.tracks in a playlist
//        authedApi.service.getPlaylistTracks(ownerId, playlistId, object : Callback<Pager<PlaylistTrack>> {
//            override fun failure(error: RetrofitError?) {
//                Utils.log("get playlist com.cziyeli.domain.tracks error: ${error?.localizedMessage}")
//            }
//
//            override fun success(pagedResponse: Pager<PlaylistTrack>?, response: Response?) {
//                Utils.log("got playlist com.cziyeli.domain.tracks! total: ${pagedResponse?.items?.size}")
////                Utils.log("first track: ${pagedResponse?.items?.get(0)?.track.toString()}")
//
//                pagedResponse?.items?.let {
//                    Utils.log("total num with previewUrls: ${countPreviewUrls(it)}")
//                }
//            }
//        })
//    }
//
//    private fun countPreviewUrls(com.cziyeli.domain.tracks: List<PlaylistTrack>): Int {
//        val previewUrls = com.cziyeli.domain.tracks.map { it.track?.preview_url }.filter { it != null && !it.isEmpty() }
//        return previewUrls.size
//    }
//
//    fun testApi(authedApi: SpotifyApi) {
//        val spotify = authedApi.service
//
//        spotify.getAlbum("2dIGnmEIy1WZIcZCFSj6i8", object : Callback<Album> {
//            override fun success(album: Album?, response: retrofit.client.Response) {
//                Utils.log("Album success: $album.name")
//            }
//
//            override fun failure(error: RetrofitError) {
//                Utils.log("Album failure: $error.toString()")
//            }
//        })
//    }

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
        // Save the access token and expiration time in shared prefs
        api.setAccessToken(authResponse.accessToken)
        accessToken = authResponse.accessToken
        val nextExpirationTime = System.currentTimeMillis() / 1000 + authResponse.expiresIn // 1 hour
        expirationCutoff = nextExpirationTime
        Utils.log("Got authentication token: $authResponse.accessToken ++ nextExpirationTime: $nextExpirationTime")
    }

}