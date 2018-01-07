package com.cziyeli.domain.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import android.os.PowerManager
import com.cziyeli.commons.Utils
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.domain.tracks.TrackResult
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Singleton


/**
 * Created by connieli on 1/3/18.
 */
@Singleton
class NativePlayerManager(@ForApplication val context: Context) :
        MediaPlayer.OnCompletionListener,
        MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener,
        PlayerInterface {
    private val TAG = NativePlayerManager::class.simpleName

    // the actual player
    private var mMediaPlayer: MediaPlayer? = null
    private val audioConfig by lazy {
        AudioAttributes.Builder()
                .setContentType(CONTENT_TYPE_MUSIC)
                .setLegacyStreamType(STREAM_MUSIC)
                .build()
    }

    // subject to publish results to
    private val mPlayerResultsPublisher: PublishSubject<TrackResult.CommandPlayerResult> by lazy {
        PublishSubject.create<TrackResult.CommandPlayerResult>()
    }

    // The current track to play.
    private var currentTrack: TrackModel? = null
    private val previewUrl: String?
            get() = currentTrack?.preview_url

    // If false, prepare/prepareAsync must be called again to get to the PreparedState
    private var playerPrepared = false

    override fun handlePlayerCommand(track: TrackModel, command: PlayerInterface.Command)
            : Observable<TrackResult.CommandPlayerResult> {
        Utils.log(TAG,"handlePlayerCommand: $command for ${track.name}")

        createMediaPlayerIfNeeded()

        // set the new track
        currentTrack = track

        when (command) {
            PlayerInterface.Command.PLAY_NEW -> {
                playNewTrack(previewUrl!!)
            }
            PlayerInterface.Command.PAUSE_OR_RESUME -> {
                if (mMediaPlayer!!.isPlaying) {
                    pauseAudio() // pause
                } else { // play!
                    mMediaPlayer!!.start() // not playing, resume
                }
            }
            PlayerInterface.Command.END_TRACK -> relaxResources(false)
            PlayerInterface.Command.STOP -> {
                mMediaPlayer!!.stop()
                playerPrepared = false
            }
        }

        notifyLoading()

        return mPlayerResultsPublisher
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private fun createMediaPlayerIfNeeded() {
        if (mMediaPlayer == null) {
            mMediaPlayer = MediaPlayer()

            mMediaPlayer!!.apply {
                // Make sure the media player will acquire a wake-lock while
                // playing. If we don't do that, the CPU might go to sleep while the
                // song is playing, causing playback to stop.
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(audioConfig)
                isLooping = true

                // we want the media player to notify us when it's ready preparing,
                // and when it's done playing:
                setOnPreparedListener(this@NativePlayerManager)
                setOnCompletionListener(this@NativePlayerManager)
                setOnErrorListener(this@NativePlayerManager)
            }

            Utils.log(TAG,"init MediaPlayer!")
        }
    }

    override fun currentState(): PlayerInterface.State {
        return when {
            // was released or not init'ed yet
            mMediaPlayer == null -> PlayerInterface.State.INVALID
            // definitely playing
            mMediaPlayer!!.isPlaying -> PlayerInterface.State.PLAYING
            // no track means not initialized so not prepared
            (currentTrack == null || !playerPrepared) -> PlayerInterface.State.NOT_PREPARED
            // player is prepared, but not playing
            (!mMediaPlayer!!.isPlaying) -> PlayerInterface.State.PAUSED
            // shouldn't get here
            else -> PlayerInterface.State.INVALID
        }
    }

    override fun onPause() {
        Utils.log(TAG, "onPause")
        if (mMediaPlayer?.isPlaying == true) {
            pauseAudio()
        }
    }

    override fun onDestroy() { // backpressed, finished activity etc
        Utils.log(TAG, "MEDIAPLAYER ++ onDestroy")
        if (mMediaPlayer?.isPlaying == true) {
            mMediaPlayer?.stop()
        }

        relaxResources(true)

        mPlayerResultsPublisher.onComplete()
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Utils.log(TAG, "MEDIAPLAYER ++ onCompletion")
        playerPrepared = false
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Utils.log(TAG, "MEDIAPLAYER ++ onError ++ $what")
        notifyError("onError $what")
        relaxResources(false)
        return false
    }

    override fun onPrepared(mp: MediaPlayer?) {
        if (currentTrack != null && mMediaPlayer != null) {
            Utils.log(TAG, "onPrepared SUCCESS ++ starting: $previewUrl")
            playerPrepared = true
            mMediaPlayer!!.start() // starts with preview url
            notifySuccess()
        } else {
            notifyError("onPrepared FAILED -- called with null track or player")
        }
    }

    override fun onResume() {
        Utils.log(TAG, "onResume ++ ${currentTrack?.name} still prepared? $playerPrepared")
        mMediaPlayer?.apply {
            if (currentTrack != null && playerPrepared) {
                start()
            }
        }
    }

    private fun playNewTrack(uri: String) {
        mMediaPlayer?.apply {
            if (isPlaying) {
                Utils.log(TAG,"playNewTrack -- already playing, resetting first")
                refreshPlayer()
            }

            setDataSource(uri) // if not idle, this will throw exception
            prepareAsync()
        }
    }

    private fun pauseAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.pause()
            notifySuccess()
        } else {
            notifyError("pauseAudio failed ++ mediaPlayer is NULL")
        }
    }

    /**
     * Resets the mediaPlayer and nullifies the track.
     *
     * If releaseMediaPlayer = Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     * *            be released or not
     */
    private fun relaxResources(releaseMediaPlayer: Boolean) {
        Utils.log(TAG, "relaxResources -- releaseMediaPlayer= $releaseMediaPlayer")

        currentTrack = null

        refreshPlayer()

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer) {
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    private fun refreshPlayer() {
        // don't nullify out the track, but reset the player with args
        mMediaPlayer?.apply {
            reset()
            playerPrepared = false
            isLooping = true
        }
    }

    // ======== PUBLISH TRACK RESULTS=====

    private fun notifyLoading() {
        mPlayerResultsPublisher.onNext(TrackResult.CommandPlayerResult.createLoading(currentTrack!!, currentState()))
    }

    private fun notifySuccess() {
        mPlayerResultsPublisher.onNext(TrackResult.CommandPlayerResult.createSuccess(currentTrack!!, currentState()))
    }

    private fun notifyError(message: String) {
        mPlayerResultsPublisher.onNext(TrackResult.CommandPlayerResult.createError(
                Error(message),
                currentTrack,
                currentState())
        )
    }
}