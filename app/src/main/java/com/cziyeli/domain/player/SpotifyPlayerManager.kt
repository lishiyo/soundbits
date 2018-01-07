package com.cziyeli.domain.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.widget.Toast
import com.cziyeli.commons.SPOTIFY_CLIENT_ID
import com.cziyeli.commons.Utils
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import com.spotify.sdk.android.player.*
import io.reactivex.Observable

/**
 * Created by connieli on 1/2/18.
 */
class SpotifyPlayerManager(val activity: Activity,
                           private val accessToken: String)
    : Player.NotificationCallback, ConnectionStateCallback, PlayerInterface {

    override fun currentState(): PlayerInterface.State {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onLoggedOut() {
        Utils.log("---- logged out ----")
    }

    override fun onLoggedIn() {
        Utils.log("---- logged in ----")
    }

    override fun onConnectionMessage(message: String?) {
        Utils.log("---- onConnectionMessage: $message ----")
    }

    override fun onLoginFailed(error: Error?) {
        Utils.log("---- onLoginFailed $error ----")
    }

    override fun onTemporaryError() {
        Utils.log("---- onTemporaryError ----")
    }

    // player
    private val mPlayer: SpotifyPlayer by lazy {
        val playerConfig = Config(activity as Context, accessToken, SPOTIFY_CLIENT_ID)
        // Since the Player is a static singleton owned by the Spotify class, we pass "this" as
        // the second argument in order to refcount it properly. Note that the method
        // Spotify.destroyPlayer() also takes an Object argument, which must be the same as the
        // one passed in here. If you pass different instances to Spotify.getPlayer() and
        // Spotify.destroyPlayer(), that will definitely result in resource leaks.
        Spotify.getPlayer(playerConfig, this, object : SpotifyPlayer.InitializationObserver {
            override fun onInitialized(player: SpotifyPlayer) {
                Utils.log("-- Player initialized --")
                mPlayer.setConnectivityStatus(mOperationCallback, getNetworkConnectivity(activity))
                mPlayer.addNotificationCallback(this@SpotifyPlayerManager)
                mPlayer.addConnectionStateCallback(this@SpotifyPlayerManager)

                // Trigger UI refresh
            }

            override fun onError(error: Throwable) {
                Utils.log("Error in initialization: " + error.message)
            }
        })
    }

    private var mCurrentPlaybackState: PlaybackState? = null
    private var mNetworkStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            mPlayer.let {
                val connectivity = getNetworkConnectivity(context)
                Utils.log("Network state changed: " + connectivity.toString())
                mPlayer.setConnectivityStatus(mOperationCallback, connectivity)
            }
        }
    }
    private var mMetadata: Metadata? = null
    private val mOperationCallback = object : Player.OperationCallback {
        override fun onSuccess() {
            Utils.log("mOperationCallback OK!")
        }

        override fun onError(error: Error) {
            Utils.log("mOperationCallback ERROR:" + error)
        }
    }

    val isLoggedIn: Boolean
        get() = mPlayer.isLoggedIn

    init {
        mPlayer.apply {
            Utils.log("SpotifyPlayerManager init ++ loggedIn: $isLoggedIn accessToken: $accessToken")
            if (!isLoggedIn) {
                login(accessToken)
            }
        }
    }

    override fun handlePlayerCommand(track: TrackModel, command: PlayerInterface.Command) : Observable<TrackResult.CommandPlayerResult> {
        Utils.log("Starting playback for $track with command: $command")
        when (command) {
            PlayerInterface.Command.PLAY_NEW -> mPlayer.playUri(mOperationCallback, track.preview_url, 0, 0)
            PlayerInterface.Command.PAUSE_OR_RESUME, PlayerInterface.Command.STOP -> if (shouldPausePlayer())
                mPlayer.pause(mOperationCallback) else mPlayer.resume(mOperationCallback)

        }

        return Observable.empty()
    }

    private fun shouldPausePlayer() : Boolean {
        return mCurrentPlaybackState != null && mCurrentPlaybackState!!.isPlaying
    }

    override fun onResume() {
        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        activity.registerReceiver(mNetworkStateReceiver, filter)

        mPlayer.apply {
//            if (!isLoggedIn) {
//                login(accessToken)
//            }
            this.addNotificationCallback(this@SpotifyPlayerManager)
//            mPlayer!!.addConnectionStateCallback(callback)
        }
    }

    override fun onPause() {
        activity.unregisterReceiver(mNetworkStateReceiver)
        mPlayer.apply {
            removeNotificationCallback(this@SpotifyPlayerManager)
            removeConnectionStateCallback(this@SpotifyPlayerManager)
        }
    }

    override fun onDestroy() {
        // *** ULTRA-IMPORTANT ***
        // ALWAYS call this in your onDestroy() method, otherwise you will leak native resources!
        // This is an unfortunate necessity due to the different memory management models of
        // Java's garbage collector and C++ RAII.
        // For more information, see the documentation on Spotify.destroyPlayer().
        Spotify.destroyPlayer(this)
    }

    override fun onPlaybackError(error: Error?) {
        Utils.log("onPlaybackError ++ Err: " + error)
    }

    override fun onPlaybackEvent(event: PlayerEvent?) {
        if (!mPlayer.isLoggedIn) {
            Toast.makeText(activity, "onPlaybackEvent not logged in yet", Toast.LENGTH_LONG).show()
            return
        }

        mCurrentPlaybackState = mPlayer.playbackState
        mMetadata = mPlayer.metadata
        Utils.log("onPlaybackEvent got event: $event with current state now: $mCurrentPlaybackState")

//        updateView()
    }


    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     *
     * @param context Android activity
     * @return Connectivity state to be passed to the SDK
     */
    @SuppressLint("MissingPermission")
    private fun getNetworkConnectivity(context: Context): Connectivity {
        val connectivityManager: ConnectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return if (activeNetwork != null && activeNetwork.isConnected) {
            Connectivity.fromNetworkType(activeNetwork.type)
        } else {
            Connectivity.OFFLINE
        }
    }

}