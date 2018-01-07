package com.cziyeli.domain.stats

import com.cziyeli.domain.tracks.TrackModel
import kaaes.spotify.webapi.android.models.AudioFeaturesTrack
import kaaes.spotify.webapi.android.models.AudioFeaturesTracks

/**
 * Calculate the stats of a single [TrackModel].
 * https://developer.spotify.com/web-api/console/get-audio-features-several-tracks/#complete
 *
 * Wraps a [AudioFeaturesTrack] from api.
 *
 * Created by connieli on 1/6/18.
 */
class TrackStats(val apiModel: AudioFeaturesTrack) {

    val id: String
        get() = apiModel.id

    val danceability: Float
        get() = apiModel.danceability

    val energy: Float
        get() = apiModel.energy

    val loudness: Float
        get() = apiModel.loudness

    val speechiness: Float
        get() = apiModel.speechiness

    val acousticness: Float
        get() = apiModel.acousticness

    val instrumentalness: Float
        get() = apiModel.instrumentalness

    val liveness: Float
        get() = apiModel.liveness

    val valence: Float
        get() = apiModel.valence

    val tempo: Float
        get() = apiModel.tempo
}

// Stats of a list of tracks
// Wraps [AudioFeaturesTracks]
class TrackListStats(val apiModel: AudioFeaturesTracks) {

    // corresponding list of track ids
    val trackIds: List<String>
        get() = apiModel.audio_features.map { it.id }

    val trackStats: List<TrackStats>
        get() = apiModel.audio_features.map { TrackStats(it) }

    val avgDanceability: Double
        get() = calculateAvgDanceability(trackStats)

    companion object {
        fun calculateAvgDanceability(tracks: List<TrackStats>) : Double {
            val count = tracks.size
            return tracks.map { it.danceability }
                    .reduce { sum, el -> sum + el }
                    .div(count).toDouble()
        }
    }
}