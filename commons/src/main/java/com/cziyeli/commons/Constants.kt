package com.cziyeli.commons

/**
 * App constants.
 */
const val TAG = "app"

const val SPOTIFY_CLIENT_ID = BuildConfig.SPOTIFY_CLIENT_ID
const val SPOTIFY_REDIRECT_URI = BuildConfig.SPOTIFY_REDIRECT_URI
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