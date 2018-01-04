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

    fun currentState() : State

    // valid commands to pass to the player
    enum class Command {
        PAUSE_OR_RESUME, // if paused, resume / if resumed, pause
        PLAY, // start playing
        RESET, // reset player to IDLE to allow setDataUri again
        STOP // sets player to STOPPED, but can't setDataUri
    }

    enum class State {
        PREPARED, // prepared and not playing yet
        PAUSED, // prepared with a track but paused
        PLAYING, // playing
        NOT_PREPARED, // idle (from new/reset/start), not prepared yet
        RELEASED // can no longer be used
    }
}