package com.cziyeli.domain.summary

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
data class TrackStats(val apiModel: AudioFeaturesTrack) {

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
data class TrackListStats(val apiModel: AudioFeaturesTracks) {

    // corresponding list of track ids
    var trackIds: List<String> = apiModel.audio_features.map { it.id }

    var trackStats: List<TrackStats> = apiModel.audio_features.map { TrackStats(it) }

    // danceability from 0 -> 1, higher is more danceable
    val avgDanceability by lazy { calculateAvgDanceability(trackStats) }
    val avgEnergy by lazy { calculateAvgEnergy(trackStats) }
    // average positivity (0 -> 1, higher is more positive)
    val avgValence by lazy { calculateAvgValence(trackStats) }

    override fun toString() : String {
        // test this was correct
        return "${trackStats.size} tracks avgDanceability: $avgDanceability"
    }

    companion object {
        fun create(apiModel: AudioFeaturesTracks) : TrackListStats {
            return TrackListStats(apiModel)
        }

        fun calculateAvgDanceability(tracks: List<TrackStats>) : Double {
            val count = tracks.size
            return tracks.map { it.danceability }
                    .reduce { sum, el -> sum + el }
                    .div(count).toDouble()
        }

        fun calculateAvgEnergy(tracks: List<TrackStats>) : Double {
            return tracks.map { it.energy }
                    .reduce { sum, el -> sum + el }
                    .div(tracks.size).toDouble()
        }

        fun calculateAvgValence(tracks: List<TrackStats>) : Double {
            return tracks.map { it.valence }
                    .reduce { sum, el -> sum + el }
                    .div(tracks.size).toDouble()
        }
    }
}