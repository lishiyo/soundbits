package com.cziyeli.songbits.playlistcard

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
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
    data class StatLabel(val title: String, val titleNumber: String, val range: Pair<Int, Int>)

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
    var isSecondSet: Boolean = false

    init {
        LayoutInflater.from(context).inflate(R.layout.widget_card_stats, this, true)
        orientation = VERTICAL

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                    R.styleable.TrackStatsView, 0, 0)
            twoColumnView = typedArray.getBoolean(R.styleable.TrackStatsView_two_column, false)
            isSecondSet = typedArray.getBoolean(R.styleable.TrackStatsView_second_set, false)
            typedArray.recycle()
        }

        if (twoColumnView) {
            val numberSize = resources.getDimension(R.dimen.default_title_size)
            title_row_one_number.setTextSize(TypedValue.COMPLEX_UNIT_PX, numberSize)
            title_row_two_number.setTextSize(TypedValue.COMPLEX_UNIT_PX, numberSize)
            title_row_three_number.setTextSize(TypedValue.COMPLEX_UNIT_PX, numberSize)

            title_row_one.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            title_row_two.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            title_row_two.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            title_row_one_number.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            title_row_two_number.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
            title_row_three_number.textAlignment = View.TEXT_ALIGNMENT_TEXT_START
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return true // don't allow the range sliders to move
    }

    fun loadTrackStats(trackStats: TrackListStats) {
        if (!isSecondSet) {
            loadFirstSet(trackStats)
        } else {
            loadSecondSet(trackStats)
        }
    }

    private fun loadFirstSet(trackStats: TrackListStats) {
        val normalizedDanceability = getNormalizedStat(trackStats.avgDanceability)
        val normalizedEnergy = getNormalizedStat(trackStats.avgEnergy)
        val normalizedValence = getNormalizedStat(trackStats.avgValence)

        loadAudioFeatures(
                StatLabel(
                        getStatTitle("danceable", normalizedDanceability, trackStats.avgDanceability),
                        getStatTitleNum(trackStats.avgDanceability, 1.0),
                        Pair(0, normalizedDanceability.roundToInt())
                ),
                StatLabel(
                        getStatTitle("energetic", normalizedEnergy, trackStats.avgEnergy),
                        getStatTitleNum(trackStats.avgEnergy, 1.0),
                        Pair(0, normalizedEnergy.roundToInt())),
                StatLabel(
                        getStatTitle("positive", normalizedValence, trackStats.avgValence),
                        getStatTitleNum(trackStats.avgValence, 1.0),
                        Pair(0, normalizedValence.roundToInt())
                ))
    }

    private fun loadSecondSet(trackStats: TrackListStats) {
        val normalizedPopularity = getNormalizedStat(trackStats.avgPopularity!! / 100)
        val normalizedAcousticness = getNormalizedStat(trackStats.avgAcousticness)
        val normalizedTempo = getNormalizedStat((trackStats.avgTempo-60) / (200-60))

        loadAudioFeatures(
                StatLabel(
                        getStatTitle("popular", normalizedPopularity, trackStats.avgPopularity!!),
                        getStatTitleNum(trackStats.avgPopularity!!, 100.0),
                        Pair(0, normalizedPopularity.roundToInt())
                ),
                StatLabel(
                        getStatTitle("acoustic", normalizedAcousticness, trackStats.avgAcousticness),
                        getStatTitleNum(trackStats.avgAcousticness, 1.0),
                        Pair(0, normalizedAcousticness.roundToInt())),
                StatLabel(
                        getStatTitle("high-tempo", normalizedTempo, trackStats.avgTempo),
                        getStatTitleNum(trackStats.avgTempo, 200.0),
                        Pair(0, normalizedTempo.roundToInt())
                ))
    }

    private fun getNormalizedStat(value: Double) : Double {
        return value * TRACK_RANGE
    }

    private fun getStatTitleNum(originalVal: Double, totalVal: Double) : String {
        return "${"%.2f".format(originalVal)} / ${"%.1f".format(totalVal)}"
    }

    private fun getStatTitle(stat: String, normalizedValue: Double, originalVal: Double) : String {
        // not at all (0-2), less (2-4), more (4-6), very (6-8)
        return when {
            normalizedValue < QUARTILE -> "not at all $stat"
             normalizedValue < QUARTILE * 2 -> "less $stat"
            normalizedValue < QUARTILE * 3 -> "more $stat"
            else -> "super $stat"
        }
    }

    private fun loadAudioFeatures(one: StatLabel, two: StatLabel, three: StatLabel) {
        title_row_one.text = one.title
        title_row_one_number.text = one.titleNumber
        range_one.start = one.range.first
        range_one.end = one.range.second

        title_row_two.text = two.title
        title_row_two_number.text = two.titleNumber
        range_two.start = two.range.first
        range_two.end = two.range.second

        title_row_three.text = three.title
        title_row_three_number.text = three.titleNumber
        range_three.start = three.range.first
        range_three.end = three.range.second
    }

}