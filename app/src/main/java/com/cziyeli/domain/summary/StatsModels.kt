package com.cziyeli.domain.summary

import com.cziyeli.commons.Utils
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

const val STAT_DANCEABILITY = "danceability"
const val STAT_ENERGY = "energy"
const val STAT_VALENCE = "valence"
const val STAT_POPULARITY = "popularity"
const val STAT_ACOUSTICNESS = "acousticness"
const val STAT_TEMPO = "tempo"

data class TrackStatsData(val map: HashMap<String, Pair<String, Double?>> = hashMapOf(
        STAT_DANCEABILITY to Pair("target_danceability", null),
        STAT_ENERGY to Pair("target_energy", null),
        STAT_VALENCE to Pair("target_valence", null),
        STAT_POPULARITY to Pair("target_popularity", null),
        STAT_ACOUSTICNESS to Pair("target_acousticness", null),
        STAT_TEMPO to Pair("target_tempo", null)
)) {

    companion object {
        fun createDefault() : TrackStatsData {
            return TrackStatsData()
        }

        fun convertTrackListStatsToMap(tracksStatsData: TrackStatsData, stats: TrackListStats) : MutableMap<String, Double> {
            val targetsMap = mutableMapOf<String, Double>()
            targetsMap[tracksStatsData.map[STAT_DANCEABILITY]!!.first] = stats.avgDanceability
            targetsMap[tracksStatsData.map[STAT_ENERGY]!!.first] = stats.avgEnergy
            targetsMap[tracksStatsData.map[STAT_VALENCE]!!.first] = stats.avgValence
            targetsMap[tracksStatsData.map[STAT_POPULARITY]!!.first] = stats.avgPopularity!!
            targetsMap[tracksStatsData.map[STAT_ACOUSTICNESS]!!.first] = stats.avgAcousticness
            targetsMap[tracksStatsData.map[STAT_TEMPO]!!.first] = stats.avgDanceability
            return targetsMap
        }
    }

    fun convertToOutgoing() : MutableMap<String, Double> {
        val targetsMap = mutableMapOf<String, Double>()
        map.entries.forEach { (stat, pair) ->
            // only add to outgoing if not null
            pair.second?.let {
                targetsMap.put(pair.first, it)
            }
        }
        return targetsMap
    }

    fun convertFromTrackListStats(stats: TrackListStats) : TrackStatsData {
        return this.copy(map = hashMapOf(
                STAT_DANCEABILITY to Pair("target_danceability", stats.avgDanceability),
                STAT_ENERGY to Pair("target_energy", stats.avgEnergy),
                STAT_VALENCE to Pair("target_valence", stats.avgValence),
                STAT_POPULARITY to Pair("target_popularity", stats.avgPopularity!!),
                STAT_ACOUSTICNESS to Pair("target_acousticness", stats.avgAcousticness),
                STAT_TEMPO to Pair("target_tempo", stats.avgTempo)
        ))
    }

    override fun toString(): String {
        return "TrackStatsData -- size: ${map.size} -- danceability: $danceability"
    }

    var danceability: Pair<String, Double?>?
        get() = map[STAT_DANCEABILITY]!!
        set(value) {
            value?.let { map[STAT_DANCEABILITY] = it }
        }

    var energy: Pair<String, Double?>?
        get() = map[STAT_ENERGY]!!
        set(value) {
            value?.let { map[STAT_ENERGY] = it }
        }

    var valence: Pair<String, Double?>?
        get() = map[STAT_VALENCE]!!
        set(value) {
            value?.let { map[STAT_VALENCE] = it }
        }

    var popularity: Pair<String, Double?>?
        get() = map[STAT_POPULARITY]!!
        set(value) {
            value?.let { map[STAT_POPULARITY] = it }
        }

    var acousticness: Pair<String, Double?>?
        get() = map[STAT_ACOUSTICNESS]!!
        set(value) {
            value?.let { map[STAT_ACOUSTICNESS] = it }
        }

    var tempo: Pair<String, Double?>?
        get() = map[STAT_TEMPO]!!
        set(value) {
            value?.let { map[STAT_TEMPO] = it }
        }

}

// Stats of a list of tracks
// Wraps [AudioFeaturesTracks]
data class TrackListStats(private val apiModel: AudioFeaturesTracks, var tracks: List<TrackModel>? = null) {
    var trackIds: List<String> = apiModel.audio_features?.map { it.id } ?: listOf()
    var trackStats: List<TrackStats> = apiModel.audio_features?.map { TrackStats(it) } ?: listOf()

    // danceability from 0 -> 1, higher is more danceable
    val avgDanceability by lazy { Utils.unlessEmpty(trackStats, 0.toDouble(), { calculateAvgDanceability(trackStats) }) }
    // average energy from 0 -> 1, higher is more eneretic
    val avgEnergy by lazy { Utils.unlessEmpty(trackStats, 0.toDouble(), { calculateAvgEnergy(trackStats) }) }
    // average positivity (0 -> 1, higher is more positive)
    val avgValence by lazy { Utils.unlessEmpty(trackStats, 0.toDouble(), { calculateAvgValence(trackStats) }) }

    // SECOND set - for full stats
    val avgAcousticness by lazy { Utils.unlessEmpty(trackStats, 0.toDouble(), { calculateAvgAcousticness(trackStats) }) }
    // 40-200 - ex Hip Hop is around 80-115 BPM
    // https://music.stackexchange.com/questions/4525/list-of-average-genre-tempo-bpm-levels
    val avgTempo by lazy { Utils.unlessEmpty(trackStats, 0.toDouble(), { calculateAvgTempo(trackStats) }) }
    // 0-100
    val avgPopularity: Double? by lazy { tracks?.let { calculateAveragePopularity(it) }}

    override fun toString() : String {
        return "${trackStats.size} tracks avgDanceability: $avgDanceability -- with tracks? ${tracks?.size}"
    }

    companion object {
        fun create(apiModel: AudioFeaturesTracks, tracks: List<TrackModel>? = null) : TrackListStats {
            return TrackListStats(apiModel, tracks)
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

        fun calculateAvgAcousticness(tracks: List<TrackStats>) : Double {
            return tracks.map { it.acousticness }
                    .reduce { sum, el -> sum + el }
                    .div(tracks.size).toDouble()
        }

        fun calculateAvgTempo(tracks: List<TrackStats>) : Double {
            return tracks.map { it.tempo }
                    .reduce { sum, el -> sum + el }
                    .div(tracks.size).toDouble()
        }

        fun calculateAveragePopularity(tracks: List<TrackModel>) : Double {
            return tracks
                    .filter { it.popularity != null}
                    .map { it.popularity as Int }
                    .reduce { sum, el -> sum + el }
                    .div(tracks.size.toDouble())
        }
    }
}