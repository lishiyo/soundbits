package com.cziyeli.songbits

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.ConnectivityManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.*
import com.cziyeli.commons.TAG
import com.spotify.sdk.android.authentication.AuthenticationClient
import com.spotify.sdk.android.authentication.AuthenticationRequest
import com.spotify.sdk.android.authentication.AuthenticationResponse
import com.spotify.sdk.android.player.*
import com.squareup.picasso.Picasso
import com.squareup.picasso.Transformation


/**
 * Created by connieli on 12/29/17.
 */
class DemoActivity : Activity(), Player.NotificationCallback, ConnectionStateCallback {

    //  _____ _      _     _
    // |  ___(_) ___| | __| |___
    // | |_  | |/ _ \ |/ _` / __|
    // |  _| | |  __/ | (_| \__ \
    // |_|   |_|\___|_|\__,_|___/
    //

    /**
     * The player used by this activity. There is only ever one instance of the player,
     * which is owned by the [com.spotify.sdk.android.player.Spotify] class and refcounted.
     * This means that you may use the Player from as many Fragments as you want, and be
     * assured that state remains consistent between them.
     *
     *
     * However, each fragment, activity, or helper class **must** call
     * [com.spotify.sdk.android.player.Spotify.destroyPlayer] when they are no longer
     * need that player. Failing to do so will result in leaked resources.
     */
    private var mPlayer: SpotifyPlayer? = null

    private var mCurrentPlaybackState: PlaybackState? = null

    /**
     * Used to get notifications from the system about the current network state in order
     * to pass them along to
     * [SpotifyPlayer.setConnectivityStatus]
     * Note that this implies <pre>android.permission.ACCESS_NETWORK_STATE</pre> must be
     * declared in the manifest. Not setting the correct network state in the SDK may
     * result in strange behavior.
     */
    private var mNetworkStateReceiver: BroadcastReceiver? = null

    /**
     * Used to log messages to a [android.widget.TextView] in this activity.
     */
    private var mStatusText: TextView? = null

    private var mMetadataText: TextView? = null

    private var mSeekEditText: EditText? = null

    /**
     * Used to scroll the [.mStatusText] to the bottom after updating text.
     */
    private var mStatusTextScrollView: ScrollView? = null
    private var mMetadata: Metadata? = null

    private val mOperationCallback = object : Player.OperationCallback {
        override fun onSuccess() {
            logStatus("OK!")
        }

        override fun onError(error: Error) {
            logStatus("ERROR:" + error)
        }
    }

    private val isLoggedIn: Boolean
        get() = mPlayer != null && mPlayer!!.isLoggedIn

    //  ___       _ _   _       _ _          _   _
    // |_ _|_ __ (_) |_(_) __ _| (_)______ _| |_(_) ___  _ __
    //  | || '_ \| | __| |/ _` | | |_  / _` | __| |/ _ \| '_ \
    //  | || | | | | |_| | (_| | | |/ / (_| | |_| | (_) | | | |
    // |___|_| |_|_|\__|_|\__,_|_|_/___\__,_|\__|_|\___/|_| |_|
    //

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demo)

        // Get a reference to any UI widgets that we'll need to use later
        mStatusText = findViewById(R.id.status_text)
        mMetadataText = findViewById(R.id.metadata) as TextView
        mSeekEditText = findViewById(R.id.seek_edittext) as EditText
        mStatusTextScrollView = findViewById(R.id.status_text_container) as ScrollView

        updateView()
        logStatus("Ready")
    }

    override fun onResume() {
        super.onResume()

        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        mNetworkStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (mPlayer != null) {
                    val connectivity = getNetworkConnectivity(baseContext)
                    logStatus("Network state changed: " + connectivity.toString())
                    mPlayer!!.setConnectivityStatus(mOperationCallback, connectivity)
                }
            }
        }

        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(mNetworkStateReceiver, filter)

        if (mPlayer != null) {
            mPlayer!!.addNotificationCallback(this@DemoActivity)
            mPlayer!!.addConnectionStateCallback(this@DemoActivity)
        }
    }

    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     *
     * @param context Android context
     * @return Connectivity state to be passed to the SDK
     */
    private fun getNetworkConnectivity(context: Context): Connectivity {
        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return if (activeNetwork != null && activeNetwork.isConnected) {
            Connectivity.fromNetworkType(activeNetwork.type)
        } else {
            Connectivity.OFFLINE
        }
    }

    //     _         _   _                _   _           _   _
    //    / \  _   _| |_| |__   ___ _ __ | |_(_) ___ __ _| |_(_) ___  _ __
    //   / _ \| | | | __| '_ \ / _ \ '_ \| __| |/ __/ _` | __| |/ _ \| '_ \
    //  / ___ \ |_| | |_| | | |  __/ | | | |_| | (_| (_| | |_| | (_) | | | |
    // /_/   \_\__,_|\__|_| |_|\___|_| |_|\__|_|\___\__,_|\__|_|\___/|_| |_|
    //

    private fun openLoginWindow() {
        val request = AuthenticationRequest.Builder(CLIENT_ID, AuthenticationResponse.Type.TOKEN, REDIRECT_URI)
                .setScopes(arrayOf("user-read-private", "playlist-read", "playlist-read-private", "streaming"))
                .build()

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
        super.onActivityResult(requestCode, resultCode, intent)

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            val response = AuthenticationClient.getResponse(resultCode, intent)
            when (response.type) {
            // Response was successful and contains auth token
                AuthenticationResponse.Type.TOKEN -> onAuthenticationComplete(response)

            // Auth flow returned an error
                AuthenticationResponse.Type.ERROR -> logStatus("Auth error: " + response.error)

            // Most likely auth flow was cancelled
                else -> logStatus("Auth result: " + response.type)
            }
        }
    }

    private fun onAuthenticationComplete(authResponse: AuthenticationResponse) {
        // Once we have obtained an authorization token, we can proceed with creating a Player.
        logStatus("Got authentication token")
        if (mPlayer == null) {
            val playerConfig = Config(applicationContext, authResponse.accessToken, CLIENT_ID)
            // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
            // the second argument in order to refcount it properly. Note that the method
            // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
            // one passed in here. If you pass different instances to Spotify.getPlayer() and
            // Spotify.destroyPlayer(), that will definitely result in resource leaks.
            mPlayer = Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
                override fun onInitialized(player: SpotifyPlayer) {
                    logStatus("-- Player initialized --")
                    mPlayer!!.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(this@DemoActivity))
                    mPlayer!!.addNotificationCallback(this@DemoActivity)
                    mPlayer!!.addConnectionStateCallback(this@DemoActivity)
                    // Trigger UI refresh
                    updateView()
                }

                override fun onError(error: Throwable) {
                    logStatus("Error in initialization: " + error.message)
                }
            })
        } else {
            mPlayer!!.login(authResponse.accessToken)
        }
    }

    //  _   _ ___   _____                 _
    // | | | |_ _| | ____|_   _____ _ __ | |_ ___
    // | | | || |  |  _| \ \ / / _ \ '_ \| __/ __|
    // | |_| || |  | |___ \ V /  __/ | | | |_\__ \
    //  \___/|___| |_____| \_/ \___|_| |_|\__|___/
    //

    private fun updateView() {
        val loggedIn = isLoggedIn

        // Login button should be the inverse of the logged in state
        val loginButton = findViewById(R.id.login_button) as Button
        loginButton.setText(if (loggedIn) R.string.logout_button_label else R.string.login_button_label)

        // Set enabled for all widgets which depend on initialized state
        for (id in REQUIRES_INITIALIZED_STATE) {
            findViewById<View>(id).isEnabled = loggedIn
        }

        // Same goes for the playing state
        val playing = loggedIn && mCurrentPlaybackState != null && mCurrentPlaybackState!!.isPlaying
        for (id in REQUIRES_PLAYING_STATE) {
            findViewById<View>(id).isEnabled = playing
        }

        if (mMetadata != null) {
            findViewById<Button>(R.id.skip_next_button).setEnabled(mMetadata!!.nextTrack != null)
            findViewById<Button>(R.id.skip_prev_button).setEnabled(mMetadata!!.prevTrack != null)
            findViewById<Button>(R.id.pause_button).setEnabled(mMetadata!!.currentTrack != null)
        }

        val coverArtView = findViewById(R.id.cover_art) as ImageView
        if (mMetadata != null && mMetadata!!.currentTrack != null) {
            val durationStr = String.format(" (%dms)", mMetadata!!.currentTrack.durationMs)
            mMetadataText!!.text = mMetadata!!.contextName + "\n" + mMetadata!!.currentTrack.name + " - " + mMetadata!!.currentTrack.artistName + durationStr

            Picasso.with(this)
                    .load(mMetadata!!.currentTrack.albumCoverWebUrl)
                    .transform(object : Transformation {
                        override fun transform(source: Bitmap): Bitmap {
                            // really ugly darkening trick
                            val copy = source.copy(source.config, true)
                            source.recycle()
                            val canvas = Canvas(copy)
                            canvas.drawColor(-0x45000000)
                            return copy
                        }

                        override fun key(): String {
                            return "darken"
                        }
                    })
                    .into(coverArtView)
        } else {
            mMetadataText!!.text = "<nothing is playing>"
            coverArtView.background = null
        }

    }

    fun onLoginButtonClicked(view: View) {
        if (!isLoggedIn) {
            logStatus("Logging in")
            openLoginWindow()
        } else {
            mPlayer!!.logout()
        }
    }

    fun onPlayButtonClicked(view: View) {

        val uri: String
        when (view.id) {
            R.id.play_track_button -> uri = TEST_SONG_URI
            R.id.play_mono_track_button -> uri = TEST_SONG_MONO_URI
            R.id.play_48khz_track_button -> uri = TEST_SONG_48kHz_URI
            R.id.play_playlist_button -> uri = TEST_PLAYLIST_URI
            R.id.play_album_button -> uri = TEST_ALBUM_URI
            else -> throw IllegalArgumentException("View ID does not have an associated URI to play")
        }

        logStatus("Starting playback for " + uri)
        mPlayer!!.playUri(mOperationCallback, uri, 0, 0)
    }

    fun onPauseButtonClicked(view: View) {
        if (mCurrentPlaybackState != null && mCurrentPlaybackState!!.isPlaying) {
            mPlayer!!.pause(mOperationCallback)
        } else {
            mPlayer!!.resume(mOperationCallback)
        }
    }

    fun onSkipToPreviousButtonClicked(view: View) {
        mPlayer!!.skipToPrevious(mOperationCallback)
    }

    fun onSkipToNextButtonClicked(view: View) {
        mPlayer!!.skipToNext(mOperationCallback)
    }

    fun onQueueSongButtonClicked(view: View) {
        mPlayer!!.queue(mOperationCallback, TEST_QUEUE_SONG_URI)
        val toast = Toast.makeText(this, R.string.song_queued_toast, Toast.LENGTH_SHORT)
        toast.show()
    }

    fun onToggleShuffleButtonClicked(view: View) {
        mPlayer!!.setShuffle(mOperationCallback, !mCurrentPlaybackState!!.isShuffling)
    }

    fun onToggleRepeatButtonClicked(view: View) {
        mPlayer!!.setRepeat(mOperationCallback, !mCurrentPlaybackState!!.isRepeating)
    }

    fun onSeekButtonClicked(view: View) {
        val seek = Integer.valueOf(mSeekEditText!!.text.toString())
        mPlayer!!.seekToPosition(mOperationCallback, seek!!)
    }

    fun onLowBitrateButtonPressed(view: View) {
        mPlayer!!.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_LOW)
    }

    fun onNormalBitrateButtonPressed(view: View) {
        mPlayer!!.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_NORMAL)
    }

    fun onHighBitrateButtonPressed(view: View) {
        mPlayer!!.setPlaybackBitrate(mOperationCallback, PlaybackBitrate.BITRATE_HIGH)
    }

    //   ____      _ _ _                _      __  __      _   _               _
    //  / ___|__ _| | | |__   __ _  ___| | __ |  \/  | ___| |_| |__   ___   __| |___
    // | |   / _` | | | '_ \ / _` |/ __| |/ / | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
    // | |__| (_| | | | |_) | (_| | (__|   <  | |  | |  __/ |_| | | | (_) | (_| \__ \
    //  \____\__,_|_|_|_.__/ \__,_|\___|_|\_\ |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
    //

    override fun onLoggedIn() {
        logStatus("Login complete")
        updateView()
    }

    override fun onLoggedOut() {
        logStatus("Logout complete")
        updateView()
    }

    override fun onLoginFailed(error: Error) {
        logStatus("Login error " + error)
    }

    override fun onTemporaryError() {
        logStatus("Temporary error occurred")
    }

    override fun onConnectionMessage(message: String) {
        logStatus("Incoming connection message: " + message)
    }

    //  _____                       _   _                 _ _ _
    // | ____|_ __ _ __ ___  _ __  | | | | __ _ _ __   __| | (_)_ __   __ _
    // |  _| | '__| '__/ _ \| '__| | |_| |/ _` | '_ \ / _` | | | '_ \ / _` |
    // | |___| |  | | | (_) | |    |  _  | (_| | | | | (_| | | | | | | (_| |
    // |_____|_|  |_|  \___/|_|    |_| |_|\__,_|_| |_|\__,_|_|_|_| |_|\__, |
    //                                                                 |___/

    /**
     * Print a status message from a callback (or some other place) to the TextView in this
     * activity
     *
     * @param status Status message
     */
    private fun logStatus(status: String) {
        Log.i(TAG, status)
        if (!TextUtils.isEmpty(mStatusText!!.text)) {
            mStatusText!!.append("\n")
        }
        mStatusText!!.append(">>>" + status)
        mStatusTextScrollView!!.post {
            // Scroll to the bottom
            mStatusTextScrollView!!.fullScroll(View.FOCUS_DOWN)
        }
    }

    //  ____            _                   _   _
    // |  _ \  ___  ___| |_ _ __ _   _  ___| |_(_) ___  _ __
    // | | | |/ _ \/ __| __| '__| | | |/ __| __| |/ _ \| '_ \
    // | |_| |  __/\__ \ |_| |  | |_| | (__| |_| | (_) | | | |
    // |____/ \___||___/\__|_|   \__,_|\___|\__|_|\___/|_| |_|
    //

    override fun onPause() {
        super.onPause()
        unregisterReceiver(mNetworkStateReceiver)

        // Note that calling Spotify.destroyPlayer() will also remove any callbacks on whatever
        // instance was passed as the refcounted owner. So in the case of this particular example,
        // it's not strictly necessary to call these methods, however it is generally good practice
        // and also will prevent your application from doing extra work in the background when
        // paused.
        if (mPlayer != null) {
            mPlayer!!.removeNotificationCallback(this@DemoActivity)
            mPlayer!!.removeConnectionStateCallback(this@DemoActivity)
        }
    }

    override fun onDestroy() {
        // *** ULTRA-IMPORTANT ***
        // ALWAYS call this in your onDestroy() method, otherwise you will leak native resources!
        // This is an unfortunate necessity due to the different memory management models of
        // Java's garbage collector and C++ RAII.
        // For more information, see the documentation on Spotify.destroyPlayer().
        Spotify.destroyPlayer(this)
        super.onDestroy()
    }

    override fun onPlaybackEvent(event: PlayerEvent) {
        // Remember kids, always use the English locale when changing case for non-UI strings!
        // Otherwise you'll end up with mysterious errors when running in the Turkish locale.
        // See: http://java.sys-con.com/node/46241
        logStatus("Event: " + event)
        mCurrentPlaybackState = mPlayer!!.playbackState
        mMetadata = mPlayer!!.metadata
        Log.i(TAG, "Player state: " + mCurrentPlaybackState!!)
        Log.i(TAG, "Metadata: " + mMetadata!!)
        updateView()
    }

    override fun onPlaybackError(error: Error) {
        logStatus("Err: " + error)
    }

    companion object {
        //   ____                _              _
        //  / ___|___  _ __  ___| |_ __ _ _ __ | |_ ___
        // | |   / _ \| '_ \/ __| __/ _` | '_ \| __/ __|
        // | |__| (_) | | | \__ \ || (_| | | | | |_\__ \
        //  \____\___/|_| |_|___/\__\__,_|_| |_|\__|___/
        //

        // from songbits app
        private const val CLIENT_ID = "7943ec6271944a349bea91696be9b8ec"
        private const val CLIENT_SECRET = "ec5a09e5d0ad46bc8bd21d7a4e7bdb3d"
        private val REDIRECT_URI = "songbits://callback"

        private val TEST_SONG_URI = "spotify:track:6KywfgRqvgvfJc3JRwaZdZ"
        private val TEST_SONG_MONO_URI = "spotify:track:1FqY3uJypma5wkYw66QOUi"
        private val TEST_SONG_48kHz_URI = "spotify:track:3wxTNS3aqb9RbBLZgJdZgH"
        private val TEST_PLAYLIST_URI = "spotify:user:spotify:playlist:2yLXxKhhziG2xzy7eyD4TD"
        private val TEST_ALBUM_URI = "spotify:album:2lYmxilk8cXJlxxXmns1IU"
        private val TEST_QUEUE_SONG_URI = "spotify:track:5EEOjaJyWvfMglmEwf9bG3"

        /**
         * Request code that will be passed together with authentication result to the onAuthenticationResult
         */
        private val REQUEST_CODE = 1337

        /**
         * UI controls which may only be enabled after the player has been initialized,
         * (or effectively, after the user has logged in).
         */
        private val REQUIRES_INITIALIZED_STATE = intArrayOf(R.id.play_track_button, R.id.play_mono_track_button, R.id.play_48khz_track_button, R.id.play_album_button, R.id.play_playlist_button, R.id.pause_button, R.id.seek_button, R.id.low_bitrate_button, R.id.normal_bitrate_button, R.id.high_bitrate_button, R.id.seek_edittext)

        /**
         * UI controls which should only be enabled if the player is actively playing.
         */
        private val REQUIRES_PLAYING_STATE = intArrayOf(R.id.skip_next_button, R.id.skip_prev_button, R.id.queue_song_button, R.id.toggle_shuffle_button, R.id.toggle_repeat_button)

    }
}
