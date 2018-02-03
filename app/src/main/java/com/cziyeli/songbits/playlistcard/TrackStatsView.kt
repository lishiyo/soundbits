package com.cziyeli.songbits.playlistcard

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.songbits.R
import kotlinx.android.synthetic.main.widget_card_stats.view.*
import kotlin.math.roundToInt

class TrackStatsView : LinearLayout {
    companion object {
        // range is 0 - 8
        private const val TRACK_STATS_BEGINNING = 0
        private const val TRACK_STATS_END = 8
    }

    @JvmOverloads
    constructor(
            context: Context,
            attrs: AttributeSet? = null,
            defStyleAttr: Int = 0)
            : super(context, attrs, defStyleAttr)

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
            context: Context,
            attrs: AttributeSet?,
            defStyleAttr: Int,
            defStyleRes: Int)
            : super(context, attrs, defStyleAttr, defStyleRes)

    data class StatLabel(val title: String, val range: Pair<Int, Int>)

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_card_stats, this, true)
        isFocusable = false
        isEnabled = false
        isClickable = false
        orientation = VERTICAL
    }

    fun loadTrackStats(trackStats: TrackListStats) {
        val normalizedDanceability = getNormalizedStat(trackStats.avgDanceability)
        val normalizedEnergy = getNormalizedStat(trackStats.avgEnergy)
        val normalizedValence = getNormalizedStat(trackStats.avgValence)

        loadAudioFeatures(
                StatLabel(
                    getStatTitle("danceable", normalizedDanceability, trackStats.avgDanceability),
                    Pair(0, normalizedDanceability.roundToInt())
                ),
                StatLabel(
                        getStatTitle("energetic", normalizedEnergy, trackStats.avgEnergy),
                        Pair(0, normalizedEnergy.roundToInt())),
                StatLabel(
                        getStatTitle("positive", normalizedValence, trackStats.avgValence),
                        Pair(0, normalizedValence.roundToInt())
                ))
    }

    private fun getNormalizedStat(value: Double) : Double {
        val range = TRACK_STATS_END - TRACK_STATS_BEGINNING
        return value * range
    }

    private fun getStatTitle(stat: String, normalizedValue: Double, originalVal: Double) : String {
        val range = TRACK_STATS_END - TRACK_STATS_BEGINNING
        val quartiles = range / 4
        // not at all (0-2), less (2-4), more (4-6), very (6-8)
        return when {
            (normalizedValue < quartiles) -> "not at all $stat ~ ${"%.2f".format(originalVal)}"
            (normalizedValue < quartiles * 2) -> "less $stat ~ ${"%.2f".format(originalVal)}"
            (normalizedValue < quartiles * 3) -> "more $stat ~ ${"%.2f".format(originalVal)}"
            else -> "very $stat"
        }
    }

    private fun loadAudioFeatures(danceability: StatLabel, energy: StatLabel, valence: StatLabel) {
        danceability_title.text = danceability.title
        danceability_range.start = danceability.range.first
        danceability_range.end = danceability.range.second

        energy_title.text = energy.title
        energy_range.start = energy.range.first
        energy_range.end = energy.range.second

        valence_title.text = valence.title
        valence_range.start = valence.range.first
        valence_range.end = valence.range.second
    }

}