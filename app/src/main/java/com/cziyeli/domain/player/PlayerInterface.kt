package com.cziyeli.domain.player

/**
 * Created by connieli on 1/3/18.
 */
interface PlayerInterface {
    fun onDestroy()

    fun onPause()

    fun onResume()

    fun handleTrack(uri: String, command: Command)

    fun currentState() : State

    // valid commands to pass to the player
    enum class Command {
        PAUSE_OR_RESUME, PLAY, STOP
    }

    enum class State {
        PREPARED, // prepared and not playing yet
        PAUSED, // prepared with a track but paused
        PLAYING, // playing
        NOT_PREPARED, // idle (from new/reset/start), not prepared yet
        RELEASED // can no longer be used
    }
}