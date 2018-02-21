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
        "user-read-email",
        "user-top-read"
)

// Shared Prefs
const val LOGIN_EXPIRATION = "login_expiration" // in seconds
const val AUTH_TOKEN = "spotify_auth_token"
const val CURRENT_USER_ID = "current_user_id"

// TODO fetch instead of hardcoding this
val GENRE_SEEDS = listOf("acoustic", "afrobeat", "alt-rock", "alternative", "ambient", "anime", "black-metal", "bluegrass", "blues",
        "bossanova", "brazil", "breakbeat", "british", "cantopop", "chicago-house", "children", "chill", "classical", "club", "comedy",
        "country", "dance", "dancehall", "death-metal", "deep-house", "detroit-techno", "disco", "disney", "drum-and-bass", "dub",
        "dubstep", "edm", "electro", "electronic", "emo", "folk", "forro", "french", "funk", "garage", "german", "gospel", "goth",
        "grindcore", "groove", "grunge", "guitar", "happy", "hard-rock", "hardcore", "hardstyle", "heavy-metal", "hip-hop", "holidays",
        "honky-tonk", "house", "idm", "indian", "indie", "indie-pop", "industrial", "iranian", "j-dance", "j-idol", "j-pop", "j-rock",
        "jazz", "k-pop", "kids", "latin", "latino", "malay", "mandopop", "metal", "metal-misc", "metalcore", "minimal-techno", "movies",
        "mpb", "new-age", "new-release", "opera", "pagode", "party", "philippines-opm", "piano", "pop", "pop-film", "post-dubstep",
        "power-pop", "progressive-house", "psych-rock", "punk", "punk-rock", "r-n-b", "rainy-day", "reggae", "reggaeton", "road-trip",
        "rock", "rock-n-roll", "rockabilly", "romance", "sad", "salsa", "samba", "sertanejo", "show-tunes", "singer-songwriter",
        "ska", "sleep", "songwriter", "soul", "soundtracks", "spanish", "study", "summer", "swedish", "synth-pop", "tango",
        "techno", "trance", "trip-hop", "turkish", "work-out", "world-music")
