package com.cziyeli.songbits

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import com.cziyeli.commons.*
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.domain.playlists.UserResult
import com.cziyeli.domain.user.UserManager
import com.cziyeli.songbits.home.HomeActivity
import com.cziyeli.songbits.home.HomeIntent
import com.cziyeli.songbits.home.HomeViewModel
import com.cziyeli.songbits.home.HomeViewState
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.ConnectionStateCallback
import com.spotify.sdk.android.player.Error
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), ConnectionStateCallback, MviView<HomeIntent, HomeViewState> {
    private val TAG = MainActivity::class.simpleName

    // check if logged in by shared prefs and in-memory
    @Inject lateinit var api: SpotifyApi
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var userManager : UserManager

    // view models
    private lateinit var viewModel: HomeViewModel

    // intents
    private val mUserPublisher = PublishSubject.create<HomeIntent.FetchUser>()
    private val mLogoutPublisher = PublishSubject.create<HomeIntent.LogoutUser>()

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                nav_home_activity.setText(R.string.title_home)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                nav_home_activity.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                nav_home_activity.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val accessTokenValid = isAccessTokenValid()
        Utils.mLog(TAG, "onCreate", "isAccessTokenValid: $accessTokenValid")
        if (accessTokenValid) { // already logged in
            api.setAccessToken(userManager.accessToken)
        }
        Utils.setVisible(login_button, !accessTokenValid)
        Utils.setVisible(nav_home_activity, accessTokenValid)
        Utils.setVisible(nav_real_home_activity, accessTokenValid)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        nav_demo_activity.setOnClickListener{ _ ->
            val intent = Intent(this, DemoActivity::class.java)
            startActivity(intent)
        }

        nav_home_activity.setOnClickListener{ _ ->
            startActivity(Intent(this, HomeActivity::class.java))
        }

        nav_real_home_activity.setOnClickListener { _ ->
            // TODO
        }

        logout_button.setOnClickListener { _ ->
            mLogoutPublisher.onNext(HomeIntent.LogoutUser())
        }

        login_button.setOnClickListener { _ ->
            if (!isAccessTokenValid()) {
                openLoginWindow()
            }
        }

        // bind the view model after all views are done
        initViewModel()
    }

    override fun intents(): Observable<out HomeIntent> {
        return Observable.merge(
                Observable.just(HomeIntent.Initial()), // send out initial intent immediately
                mUserPublisher,
                mLogoutPublisher
        )
    }

    private fun isAccessTokenValid(): Boolean {
        return userManager.isAccessTokenValid()
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel::class.java)

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

    override fun render(state: HomeViewState) {
        Utils.setVisible(login_button, !isAccessTokenValid())
        Utils.setVisible(nav_home_activity, isAccessTokenValid())
        Utils.setVisible(nav_real_home_activity, isAccessTokenValid())

        when (state.loggedInStatus) {
            UserResult.Status.FAILURE -> Utils.mLog(TAG, "render", "failed to save/logout current user")
            UserResult.Status.SUCCESS -> Utils.mLog(TAG, "render", "saved/logged-out current user!")
        }
    }

    //   ____      _ _ _                _      __  __      _   _               _
    //  / ___|__ _| | | |__   __ _  ___| | __ |  \/  | ___| |_| |__   ___   __| |___
    // | |   / _` | | | '_ \ / _` |/ __| |/ / | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
    // | |__| (_| | | | |_) | (_| | (__|   <  | |  | |  __/ |_| | | | (_) | (_| \__ \
    //  \____\__,_|_|_|_.__/ \__,_|\___|_|\_\ |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
    //

    override fun onLoggedIn() {
        Utils.log(TAG, "Login complete")
    }

    override fun onLoggedOut() {
        Utils.log(TAG, "Logout complete")
    }

    override fun onLoginFailed(error: Error) {
        Utils.log(TAG, "Login error " + error)
    }

    override fun onTemporaryError() {
        Utils.log(TAG, "Temporary error occurred")
    }

    override fun onConnectionMessage(message: String) {
        Utils.log(TAG, "Incoming connection message: " + message)
    }

    //     _         _   _                _   _           _   _
    //    / \  _   _| |_| |__   ___ _ __ | |_(_) ___ __ _| |_(_) ___  _ __
    //   / _ \| | | | __| '_ \ / _ \ '_ \| __| |/ __/ _` | __| |/ _ \| '_ \
    //  / ___ \ |_| | |_| | | |  __/ | | | |_| | (_| (_| | |_| | (_) | | | |
    // /_/   \_\__,_|\__|_| |_|\___|_| |_|\__|_|\___\__,_|\__|_|\___/|_| |_|
    //

    private fun openLoginWindow() {
        val request = AuthenticationRequest.Builder(SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.TOKEN, SPOTIFY_REDIRECT_URI)
                .setScopes(SCOPES)
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
                else -> Utils.log(TAG, "Auth result: " + response.type)
            }
        }
    }

    private fun onAuthenticationComplete(authResponse: AuthenticationResponse) {
        // Set up user manager
        userManager.accessToken = authResponse.accessToken
        val nextExpirationSeconds = System.currentTimeMillis() / 1000 + authResponse.expiresIn // 1 hour
        userManager.nextExpirationSeconds = nextExpirationSeconds

        // Save the access token and expiration time in shared prefs
        api.setAccessToken(authResponse.accessToken)

        // fetch current user and save to UserManager
        mUserPublisher.onNext(HomeIntent.FetchUser())

        // rerender! TODO: do via MVI flow
        Utils.setVisible(login_button, false)
        Utils.setVisible(nav_home_activity, true)
        Utils.setVisible(nav_real_home_activity, true)

        Utils.log(TAG, "Got authentication token!")
    }

}
