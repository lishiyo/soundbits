package com.cziyeli.domain

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import com.cziyeli.commons.SPOTIFY_CLIENT_ID
import com.cziyeli.commons.Utils
import com.cziyeli.commons.di.PerActivity
import com.spotify.sdk.android.player.*

/**
 * Created by connieli on 1/2/18.
 */
//@PerActivity
class SpotifyPlayerManager(val activity: Activity,
                           private val accessToken: String) : Player.NotificationCallback {

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
//                    mPlayer!!.addConnectionStateCallback(this@DemoActivity)
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
        // Set up the broadcast receiver for network events. Note that we also unregister
        // this receiver again in onPause().
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        activity.registerReceiver(mNetworkStateReceiver, filter)

        mPlayer.apply {
            login(accessToken)
            this.addNotificationCallback(this@SpotifyPlayerManager)
//            mPlayer!!.addConnectionStateCallback(callback)
        }
    }

    fun handleTrack(uri: String, command: Command = Command.PLAY) {
        Utils.log("Starting playback for $uri with command: $command")
        when (command) {
            Command.PLAY -> mPlayer.playUri(mOperationCallback, uri, 0, 0)
            Command.PAUSE_OR_RESUME, Command.STOP -> if (shouldPausePlayer())
                mPlayer.pause(mOperationCallback) else mPlayer.resume(mOperationCallback)

        }
    }

    private fun shouldPausePlayer() : Boolean {
        return mCurrentPlaybackState != null && mCurrentPlaybackState!!.isPlaying
    }

    fun onPause() {
        activity.unregisterReceiver(mNetworkStateReceiver)
        mPlayer.apply {
            removeNotificationCallback(this@SpotifyPlayerManager)
//            removeConnectionStateCallback(listener)
        }
    }

    fun onDestroy() {
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
        // Remember kids, always use the English locale when changing case for non-UI strings!
        Utils.log("onPlaybackEvent ++ Event: " + event)
        mCurrentPlaybackState = mPlayer.playbackState
        mMetadata = mPlayer.metadata
        Utils.log("Player state: " + mCurrentPlaybackState!!)
        Utils.log("Metadata: " + mMetadata!!)

//        updateView()
    }


    /**
     * Registering for connectivity changes in Android does not actually deliver them to
     * us in the delivered intent.
     *
     * @param context Android activity
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

    enum class Command {
        PAUSE_OR_RESUME, PLAY, STOP
    }
}