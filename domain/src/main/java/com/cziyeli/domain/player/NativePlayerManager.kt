package com.cziyeli.domain.player

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.PowerManager
import com.cziyeli.commons.Utils
import com.cziyeli.commons.di.ForApplication
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

    override fun handleTrack(uri: String, command: PlayerInterface.Command) {
        Utils.log("handleTrack: $uri ++ $command")

        createMediaPlayerIfNeeded()

        when (command) {
            PlayerInterface.Command.PLAY -> {
                playAudio(uri)
            }
            PlayerInterface.Command.PAUSE_OR_RESUME -> {
                if (mMediaPlayer?.isPlaying == true) {
                    pauseAudio()
                } else {
                    playAudio(uri)
                }
            }
            PlayerInterface.Command.STOP -> mMediaPlayer?.stop()
        }

    }

    var mMediaPlayer: MediaPlayer? = null

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

                setAudioStreamType(AudioManager.STREAM_MUSIC);

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
            start()
        }
    }

    fun pauseAudio() {
        mMediaPlayer?.pause()
    }

    /**
     * Releases resources used by the service for playback. This includes the
     * "foreground service" status, the wake locks and possibly the MediaPlayer.
     * @param releaseMediaPlayer Indicates whether the Media Player should also
     * *            be released or not
     */
    private fun relaxResources(releaseMediaPlayer: Boolean) {
        Utils.log("relaxResources. releaseMediaPlayer= $releaseMediaPlayer")

        // stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer?.reset()
            mMediaPlayer?.release()
            mMediaPlayer = null
        }
    }

    override fun onPause() {
        if (mMediaPlayer?.isPlaying == true) {
            pauseAudio()
        }
    }

    override fun onDestroy() { // backpressed
        relaxResources(true)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        relaxResources(true)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        return false
    }

    override fun onPrepared(mp: MediaPlayer?) {
        Utils.log("onPrepared ++ ")
        mp?.start()
    }

    override fun onResume() {
        Utils.log("onResume")
    }

}