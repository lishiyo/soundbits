package com.cziyeli.domain.player

import com.cziyeli.domain.tracks.TrackCard
import com.cziyeli.domain.tracks.TrackResult
import io.reactivex.Observable

/**
 * Created by connieli on 1/3/18.
 */
interface PlayerInterface {
    fun onDestroy()

    fun onPause()

    fun onResume()

    fun handlePlayerCommand(track: TrackCard, command: Command) : Observable<TrackResult.CommandPlayerResult>

    // current player state
    fun currentState() : State

    // valid commands to pass to the player
    enum class Command {
        PAUSE_OR_RESUME, // if paused, resume / if resumed, pause
        PLAY_NEW, // start playing a new track
        END_TRACK, // reset player to IDLE to allow playing new track
        STOP // sets player to STOPPED, must be for current track
    }

    enum class State {
        NOT_PREPARED, // idle or initialized (from new/reset/start), not prepared yet
        PAUSED, // prepared and ready to play but not playing
        PLAYING, // currently playing
        INVALID // released or null player
    }
}