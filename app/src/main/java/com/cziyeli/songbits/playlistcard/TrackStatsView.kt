package com.cziyeli.songbits.playlistcard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.LinearLayout
import com.cziyeli.commons.Utils
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.songbits.R
import kotlinx.android.synthetic.main.widget_card_stats.view.*
import kotlin.math.roundToInt

class TrackStatsView@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    data class StatLabel(val title: String, val range: Pair<Int, Int>)

    companion object {
        // range is 0 - 8
        private const val TRACK_STATS_BEGINNING = 0
        private const val TRACK_STATS_END = 9
        private const val TRACK_RANGE = TRACK_STATS_END - TRACK_STATS_BEGINNING
        private const val QUARTILE: Double = TRACK_RANGE / 4.0

        // height of title in two-column view
        private val FULL_HEIGHT_PX = Utils.dpToPx(42)
    }

    // Whether this is the two-column "full" stats view
    var twoColumnView: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_card_stats, this, true)
        orientation = VERTICAL

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                    R.styleable.TrackStatsView, 0, 0)
            twoColumnView = typedArray.getBoolean(R.styleable.TrackStatsView_two_column, false)
            typedArray.recycle()
        }

        if (twoColumnView) {
            title_row_one.height = FULL_HEIGHT_PX
            title_row_two.height = FULL_HEIGHT_PX
            title_row_three.height = FULL_HEIGHT_PX
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true // don't allow the range sliders to move
    }

    fun loadTrackStats(trackStats: TrackListStats, secondSet: Boolean = false) {
        if (!secondSet) {
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
        } else {
            val normalizedPopularity = getNormalizedStat(trackStats.avgPopularity!! / 100)
            val normalizedAcousticness = getNormalizedStat(trackStats.avgAcousticness)
            val normalizedTempo = getNormalizedStat(trackStats.avgTempo / (200-40))

            loadAudioFeatures(
                    StatLabel(
                            getStatTitle("popular", normalizedPopularity, trackStats.avgPopularity!!),
                            Pair(0, normalizedPopularity.roundToInt())
                    ),
                    StatLabel(
                            getStatTitle("acoustic", normalizedAcousticness, trackStats.avgAcousticness),
                            Pair(0, normalizedAcousticness.roundToInt())),
                    StatLabel(
                            getStatTitle("high-tempo", normalizedTempo, trackStats.avgTempo),
                            Pair(0, normalizedTempo.roundToInt())
                    ))
        }
    }

    private fun getNormalizedStat(value: Double) : Double {
        return value * TRACK_RANGE
    }

    private fun getStatTitle(stat: String, normalizedValue: Double, originalVal: Double) : String {
        // not at all (0-2), less (2-4), more (4-6), very (6-8)
        return when {
            !twoColumnView && normalizedValue < QUARTILE -> "not at all $stat ~ ${"%.2f".format(originalVal)}"
            !twoColumnView && normalizedValue < QUARTILE * 2 -> "less $stat ~ ${"%.2f".format(originalVal)}"
            !twoColumnView && normalizedValue < QUARTILE * 3 -> "more $stat ~ ${"%.2f".format(originalVal)}"
            !twoColumnView -> "super $stat ~ ${"%.2f".format(originalVal)}"
            twoColumnView && normalizedValue < QUARTILE -> "not at all $stat \n${"%.2f".format(originalVal)}"
            twoColumnView && normalizedValue < QUARTILE * 2 -> "less $stat \n${"%.2f".format(originalVal)}"
            twoColumnView && normalizedValue < QUARTILE * 3 -> "more $stat \n${"%.2f".format(originalVal)}"
            else -> "super $stat \n${"%.2f".format(originalVal)}"
        }
    }

    private fun loadAudioFeatures(danceability: StatLabel, energy: StatLabel, valence: StatLabel) {
        title_row_one.text = danceability.title
        range_one.start = danceability.range.first
        range_one.end = danceability.range.second

        title_row_two.text = energy.title
        range_two.start = energy.range.first
        range_two.end = energy.range.second

        title_row_three.text = valence.title
        range_three.start = valence.range.first
        range_three.end = valence.range.second
    }

}