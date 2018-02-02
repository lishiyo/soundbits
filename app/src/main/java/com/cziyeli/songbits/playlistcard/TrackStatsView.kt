package com.cziyeli.songbits.playlistcard

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.support.annotation.ColorRes
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.cziyeli.commons.colorFromRes
import com.cziyeli.domain.summary.TrackListStats
import com.cziyeli.songbits.R
import me.bendik.simplerangeview.SimpleRangeView
import kotlin.math.roundToInt

class TrackStatsView : LinearLayout {
    companion object {
        private val TRACK_STATS_PAIRS = listOf(
                StatLabel("highly danceable", Pair(0, 6)),
                StatLabel("fast tempo", Pair(0, 2)),
                StatLabel("mostly acoustic", Pair(0, 8))
        )
        // range is 0 - 8
        private const val TRACK_STATS_BEGINNING = 0
        private const val TRACK_STATS_END = 8

        @ColorRes
        private const val DEFAULT_ACTIVE_LINE_COLOR = R.color.turquoise
        @ColorRes
        private const val DEFAULT_FIXED_LINE_COLOR = R.color.colorGrey
        private const val DEFAULT_LINE_WIDTH = 12f
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
        LayoutInflater.from(context).inflate(R.layout.widget_card_stats, this, false)
        isFocusable = false
        isEnabled = false
        isClickable = false
        orientation = VERTICAL
    }

    fun loadTrackStats(trackStats: TrackListStats) {
        val normalizedDanceability = getNormalizedStat(trackStats.avgDanceability)
        val normalizedEnergy = getNormalizedStat(trackStats.avgEnergy)
        val normalizedValence = getNormalizedStat(trackStats.avgValence)

        val statLabels = mutableListOf(
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
                )
        )

        loadAudioFeatures(statLabels)
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

    private fun loadAudioFeatures(statLabels: List<StatLabel>) {
        for (audioFeature in statLabels) {
            val beginning = audioFeature.range.first
            val end = audioFeature.range.second

            val titleView = createLabelView(audioFeature.title)

            val rangeView = SimpleRangeView.Builder(context)
                    .count(9)
                    .start(beginning)
                    .end(end)
                    .startFixed(TRACK_STATS_BEGINNING)
                    .endFixed(TRACK_STATS_END)
                    .showFixedLine(true)
                    .movable(false)
                    .activeLineColor(colorFromRes(DEFAULT_ACTIVE_LINE_COLOR))
                    .activeTickColor(colorFromRes(DEFAULT_ACTIVE_LINE_COLOR))
                    .activeThumbColor(colorFromRes(DEFAULT_ACTIVE_LINE_COLOR))
                    .activeThumbRadius(0f)
                    .fixedThumbRadius(0f)
                    .activeLineThickness(DEFAULT_LINE_WIDTH)
                    .fixedLineThickness(DEFAULT_LINE_WIDTH)
                    .fixedLineColor(colorFromRes(DEFAULT_FIXED_LINE_COLOR))
                    .tickColor(colorFromRes(R.color.ghost_white))
                    .fixedThumbLabelColor(colorFromRes(DEFAULT_FIXED_LINE_COLOR))
                    .fixedThumbColor(colorFromRes(DEFAULT_FIXED_LINE_COLOR))
                    .showLabels(false)
                    .showTicks(false)
                    .showFixedLine(true)
                    .showActiveTicks(false)
                    .showFixedTicks(false)
                    .innerRangePadding(0f)
                    .build()

            titleView.isClickable = false
            titleView.isFocusable = false
            rangeView.isClickable = false
            rangeView.isFocusable = false

            addView(titleView)
            addView(rangeView)
        }
    }

    private fun createLabelView(title: String) : TextView {
        val typeface = ResourcesCompat.getFont(context, R.font.indie_flower)
        //for the bolded text view
        val titleView = TextView(context)
        titleView.setTypeface(typeface, Typeface.NORMAL)
        titleView.text = title
        titleView.alpha = 0.9f

        return titleView
    }

}