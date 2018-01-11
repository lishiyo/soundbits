package com.cziyeli.data.remote

class TrackData(val trackUris: List<String>) {
    // needs to be ',' delimited set of strings

    override fun toString(): String {
        return trackUris.joinToString(separator = ",")
    }
}