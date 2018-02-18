package com.cziyeli.commons

/**
 * Created by connieli on 12/31/17.
 */
const val TAG = "connie"

// from songbits app
const val SPOTIFY_API_BASE = "https://api.spotify.com/"
const val SPOTIFY_CLIENT_ID = "7943ec6271944a349bea91696be9b8ec"
const val SPOTIFY_CLIENT_SECRET = "ec5a09e5d0ad46bc8bd21d7a4e7bdb3d"
const val SPOTIFY_REDIRECT_URI = "songbits://callback"
const val SPOTIFY_REQUEST_CODE = 1337
val SCOPES = arrayOf(
        "user-read-private",
        "playlist-read",
        "playlist-read-private",
        "streaming",
        "playlist-modify-public",
        "playlist-modify-private",
        "user-read-email"
)

// Shared Prefs
const val LOGIN_EXPIRATION = "login_expiration" // in seconds
const val AUTH_TOKEN = "spotify_auth_token"
const val CURRENT_USER_ID = "current_user_id"


// tests
private val TEST_SONG_URI = "spotify:track:6KywfgRqvgvfJc3JRwaZdZ"
private val TEST_SONG_MONO_URI = "spotify:track:1FqY3uJypma5wkYw66QOUi"
private val TEST_SONG_48kHz_URI = "spotify:track:3wxTNS3aqb9RbBLZgJdZgH"
private val TEST_PLAYLIST_URI = "spotify:user:spotify:playlist:2yLXxKhhziG2xzy7eyD4TD"
private val TEST_ALBUM_URI = "spotify:album:2lYmxilk8cXJlxxXmns1IU"
private val TEST_QUEUE_SONG_URI = "spotify:track:5EEOjaJyWvfMglmEwf9bG3"

// test web api
const val TEST_OWNER_ID = "spotify"
const val TEST_ALBUM_ID = "7e0ij2fpWaxOEHv5fUYZjd"
const val TEST_PLAYLIST_ID_00 = "3TsVGhO21QrDDw2gNjGyfd" // Bryce Vine Approved
const val TEST_PLAYLIST_ID_01 = "37i9dQZF1DWT2jS7NwYPVI" // New Noise