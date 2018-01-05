package com.cziyeli.domain.player

import com.cziyeli.commons.Utils
import com.cziyeli.domain.tracks.TrackCard
import com.cziyeli.domain.tracks.TrackResult
import dm.audiostreamer.CurrentSessionCallback
import dm.audiostreamer.MediaMetaData
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

/**
 * Wrapper around https://github.com/dibakarece/DMAudioStreamer
 *
 * Created by connieli on 1/4/18.
 */
class AudioStreamerPlayer : PlayerInterface, CurrentSessionCallback {
    private val TAG = AudioStreamerPlayer::class.simpleName

    // subject to publish results to
    private val mTrackResultsPublisher: PublishSubject<TrackResult.CommandPlayerResult> by lazy {
        PublishSubject.create<TrackResult.CommandPlayerResult>()
    }

    // The current track to play.
    private var currentTrack: TrackCard? = null
    private val previewUrl: String?
        get() = currentTrack?.preview_url

    // If false, prepare/prepareAsync must be called again to get to the PreparedState
    private var playerPrepared = false


    override fun onDestroy() {
        Utils.mLog(TAG, "onDestroy")
    }

    override fun onPause() {
        Utils.mLog(TAG, "onPause")
    }

    override fun onResume() {
        Utils.mLog(TAG, "onResume")
    }

    override fun handlePlayerCommand(track: TrackCard, command: PlayerInterface.Command): Observable<TrackResult.CommandPlayerResult> {
        Utils.mLog(TAG, "handlePlayerCommand", "track", track.name)
        return mTrackResultsPublisher
    }

    override fun currentState(): PlayerInterface.State {
        Utils.mLog(TAG, "currentState")
        return PlayerInterface.State.INVALID
    }


    // ==== LIB ===

    override fun currentSeekBarPosition(position: Int) {
        Utils.mLog(TAG, "currentSeekBarPosition", "pos", position.toString())
    }

    override fun playSongComplete() {
        Utils.mLog(TAG, "playSongComplete")
    }

    override fun playNext(indexP: Int, currentAudio: MediaMetaData?) {
        Utils.mLog(TAG, "playNext", "pos", indexP.toString(), "currentAudio", currentAudio?.toString())
    }

    override fun updatePlaybackState(state: Int) {
        Utils.mLog(TAG, "updatePlaybackState", "state", state.toString())
    }

    override fun playCurrent(indexP: Int, currentAudio: MediaMetaData?) {
        Utils.mLog(TAG, "playCurrent", "indexP", indexP.toString(), "currentAudio", currentAudio?.toString())
    }

    override fun playPrevious(indexP: Int, currentAudio: MediaMetaData?) {
        Utils.mLog(TAG, "playPrevious", "pos", indexP.toString(), "currentAudio", currentAudio?.toString())
    }

}