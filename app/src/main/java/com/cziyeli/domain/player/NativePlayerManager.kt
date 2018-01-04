package com.cziyeli.domain.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioAttributes.CONTENT_TYPE_MUSIC
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaPlayer
import android.os.PowerManager
import com.cziyeli.commons.Utils
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.domain.tracks.TrackCard
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

    // the actual player
    var mMediaPlayer: MediaPlayer? = null

    // subject to publish results to
    private val resultsSubject : PublishSubject<TrackResult.CommandPlayerResult> by lazy {
        PublishSubject.create<TrackResult.CommandPlayerResult>()
    }

    // The current track to play.
    private var currentTrack: TrackCard? = null
    private val previewUrl: String?
            get() = currentTrack?.preview_url


    override fun handleTrack(track: TrackCard, command: PlayerInterface.Command) : Observable<TrackResult.CommandPlayerResult> {
        Utils.log("MEDIAPLAYER ++ handleTrack: $previewUrl ++ $command")

        createMediaPlayerIfNeeded()

        currentTrack = track

        when (command) {
            PlayerInterface.Command.PLAY -> {
                playAudio(previewUrl!!)
            }
            PlayerInterface.Command.PAUSE_OR_RESUME -> {
                if (mMediaPlayer?.isPlaying == true) {
                    pauseAudio()
                } else {
                    playAudio(previewUrl!!)
                }
            }
            PlayerInterface.Command.STOP -> mMediaPlayer?.stop()
        }

        notifyLoading()

        return resultsSubject
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

                val audioConfig = AudioAttributes.Builder()
                        .setContentType(CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(STREAM_MUSIC).build()
                setAudioAttributes(audioConfig)
                isLooping = true

                // we want the media player to notify us when it's ready preparing,
                // and when it's done playing:
                setOnPreparedListener(this@NativePlayerManager)
                setOnCompletionListener(this@NativePlayerManager)
                setOnErrorListener(this@NativePlayerManager)
            }

        }
    }

    fun playAudio(uri: String) {
        mMediaPlayer?.apply {
            setDataSource(uri)
            prepareAsync()
//            start()
        }
    }

    fun pauseAudio() {
        if (mMediaPlayer != null) {
            mMediaPlayer!!.pause()
            resultsSubject.onNext(
                    TrackResult.CommandPlayerResult.createSuccess(currentTrack!!,
                    currentState()))
        } else {
            notifyError("pauseAudio failed ++ mediaPlayer is NULL")
        }
    }


    override fun currentState(): PlayerInterface.State {
        return PlayerInterface.State.PREPARED // todo change
    }

    override fun onPause() {
        if (mMediaPlayer?.isPlaying == true) {
            pauseAudio()
        }
    }

    override fun onDestroy() { // backpressed
        Utils.log("MEDIAPLAYER ++ onDestroy")
        relaxResources(true)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        Utils.log("MEDIAPLAYER ++ onCompletion")
        relaxResources(true)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Utils.log("MEDIAPLAYER ++ onError ++ $what")
        notifyError("onError $what")
        relaxResources(false)
        return false
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Utils.log("MEDIAPLAYER ++ onPrepared ++ currentUri: $previewUrl")
        startPlaying()
    }

    override fun onResume() {
        Utils.log("MEDIAPLAYER ++ onResume ++ currentUri: $previewUrl")
        startPlaying()
    }

    private fun startPlaying() {
        if (currentTrack != null && mMediaPlayer != null) {
            mMediaPlayer?.start() // starts with preview url
            notifySuccess()
        } else {
            notifyError("startPlaying FAILED -- called with null track or player")
        }
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     * *            be released or not
     */
    private fun relaxResources(releaseMediaPlayer: Boolean) {
        Utils.log("MEDIAPLAYER ++ relaxResources. releaseMediaPlayer= $releaseMediaPlayer")

        currentTrack = null
        mMediaPlayer?.apply {
            reset()
        }

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer) {
            mMediaPlayer?.release()
            mMediaPlayer = null
        }

        // notify listeners
        notifySuccess()
    }

    private fun notifyLoading() {
        resultsSubject.onNext(TrackResult.CommandPlayerResult.createLoading(currentTrack!!, currentState()))
    }

    private fun notifySuccess() {
        resultsSubject.onNext(TrackResult.CommandPlayerResult.createSuccess(currentTrack!!, currentState()))
    }

    private fun notifyError(message: String) {
        resultsSubject.onNext(TrackResult.CommandPlayerResult.createError(
                Error(message),
                currentTrack,
                currentState())
        )
    }
}