package com.cziyeli.domain.player

/**
 * Created by connieli on 1/3/18.
 */
interface PlayerInterface {
    fun onDestroy()

    fun onPause()

    fun onResume()

    fun handleTrack(uri: String, command: Command)

    enum class Command {
        PAUSE_OR_RESUME, PLAY, STOP
    }
}