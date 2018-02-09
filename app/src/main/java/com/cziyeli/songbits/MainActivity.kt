package com.cziyeli.songbits

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import com.cziyeli.commons.*
import com.cziyeli.commons.mvibase.MviView
import com.cziyeli.domain.user.UserManager
import com.cziyeli.songbits.home.HomeIntent
import com.cziyeli.songbits.home.HomeViewModel
import com.cziyeli.songbits.home.oldhome.OldHomeActivity
import com.cziyeli.songbits.root.RootActivity
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.ConnectionStateCallback
import com.spotify.sdk.android.player.Error
import dagger.android.AndroidInjection
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kaaes.spotify.webapi.android.SpotifyApi
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject


class MainActivity : AppCompatActivity(), ConnectionStateCallback, MviView<HomeIntent, com.cziyeli.songbits.home.HomeViewState> {
    private val TAG = MainActivity::class.simpleName

    // check if logged in by shared prefs and in-memory
    @Inject lateinit var api: SpotifyApi
    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var userManager : UserManager

    // view models
    private lateinit var viewModelOld: HomeViewModel

    // intents
    private val mUserPublisher = PublishSubject.create<HomeIntent.FetchUser>()
    private val mLogoutPublisher = PublishSubject.create<HomeIntent.LogoutUser>()
    private val compositeDisposable = CompositeDisposable()

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                nav_oldhome_activity.setText(R.string.title_oldhome)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                nav_oldhome_activity.setText(R.string.title_dashboard)
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                nav_oldhome_activity.setText(R.string.title_notifications)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // bind the view model before events
        initViewModel()

        val accessTokenValid = isAccessTokenValid()
        if (accessTokenValid) { // already logged in
            // fetch current user and save to UserManager
            api.setAccessToken(userManager.accessToken)
            mUserPublisher.onNext(HomeIntent.FetchUser())
        }
        Utils.setVisible(login_button, !accessTokenValid)
        Utils.setVisible(nav_oldhome_activity, accessTokenValid)
        Utils.setVisible(nav_root_activity, accessTokenValid)

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        nav_demo_activity.setOnClickListener{ _ ->
            val intent = Intent(this, DemoActivity::class.java)
            startActivity(intent)
        }

        nav_oldhome_activity.setOnClickListener{ _ ->
            startActivity(Intent(this, OldHomeActivity::class.java))
        }

        nav_root_activity.setOnClickListener { _ ->
            startActivity(Intent(this, RootActivity::class.java))
        }

        logout_button.setOnClickListener { _ ->
            mLogoutPublisher.onNext(HomeIntent.LogoutUser())
        }

        login_button.setOnClickListener { _ ->
            if (!isAccessTokenValid()) {
                openLoginWindow()
            }
        }

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
        viewModelOld = ViewModelProviders.of(this, viewModelFactory).get(HomeViewModel::class.java)

        // add viewmodel as an observer of this fragment lifecycle
        viewModelOld.let { lifecycle.addObserver(it) }

        // Subscribe to the viewmodel states
        compositeDisposable.add(
                viewModelOld.states().subscribe({ state ->
                    state?.let {
                        this.render(state)
                    }
                })
        )

        // Bind ViewModel to merged intents stream - will send off INIT intent to seed the db
        viewModelOld.processIntents(intents())
    }

    override fun render(state: com.cziyeli.songbits.home.HomeViewState) {
        Utils.setVisible(login_button, !isAccessTokenValid())
        Utils.setVisible(nav_oldhome_activity, isAccessTokenValid())
        Utils.setVisible(nav_root_activity, isAccessTokenValid())
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
        Utils.setVisible(nav_oldhome_activity, true)
        Utils.setVisible(nav_root_activity, true)

        Utils.log(TAG, "Got authentication token!")
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        super.onDestroy()
    }
}
