package com.cziyeli.songbits.profile

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import com.cziyeli.commons.roundToDecimalPlaces
import com.cziyeli.domain.summary.TrackStatsData
import com.cziyeli.songbits.playlistcard.TrackStatsView
import com.jakewharton.rxrelay2.PublishRelay
import kotlinx.android.synthetic.main.widget_card_stats.view.*
import me.bendik.simplerangeview.SimpleRangeView
import org.jetbrains.annotations.NotNull

/**
 * Version of [TrackStatsView] that is movable, reflecting ui state.
 *
 * Created by connieli on 2/18/18.
 */
class EditTrackStatsView@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : TrackStatsView(context, attrs, defStyleAttr) {
    private val TAG = EditTrackStatsView::class.java.simpleName

    // viewmodel representing single row
    data class StatsGroup(val titleView: TextView,
                          val titleNumView: TextView,
                          val displayModel: Pair<String, Double>, // "danceable" -> 1.0
                          var dataModel: Pair<String, Double?> // "target_danceability" -> 0.44
    )

    // rangeView => statsGroup (views, display model, dataModel)
    data class TrackStatsMap(val map: Map<SimpleRangeView, StatsGroup>)

    // just to get the map
    private val dataModel = TrackStatsData.createDefault()

    // map if set one
    private val trackStatsMapOne = TrackStatsMap(map = mapOf(
            range_one to StatsGroup(title_row_one, title_row_one_number,
                    "danceable" to 1.0, dataModel.danceability!!),
            range_two to StatsGroup(title_row_two, title_row_two_number,
                    "energetic" to 1.0,  dataModel.energy!!),
            range_three to StatsGroup(title_row_three, title_row_three_number,
                    "positive" to 1.0, dataModel.valence!!)
    ))

    // map if set two
    private val trackStatsMapTwo = TrackStatsMap(map = mapOf(
            range_one to StatsGroup(title_row_one, title_row_one_number,
                    "popular" to 100.0, dataModel.popularity!!),
            range_two to StatsGroup(title_row_two, title_row_two_number,
                    "acoustic" to 1.0, dataModel.acousticness!!),
            range_three to StatsGroup(title_row_three, title_row_three_number,
                    "high-tempo" to 200.0, dataModel.tempo!!)
    ))

    // public stream that emits whenever the range changes
    val statsChangePublisher: PublishRelay<Map<String, Double>> by lazy { PublishRelay.create<Map<String, Double>>() }

    init {
        val relevantMap = if (isFirstSet) trackStatsMapOne else trackStatsMapTwo
        relevantMap.map.entries.forEach { (range, statsGroup) ->
            statsGroup.let {
                val (titleText, totalVal) = it.displayModel
                range.onChangeRangeListener = object : SimpleRangeView.OnChangeRangeListener {
                    override fun onRangeChanged(@NotNull rangeView: SimpleRangeView, start: Int, end: Int) {
                        val absoluteDiff = (end - start).toDouble() // already normalized to 9
                        val fractionDiff = absoluteDiff / TRACK_RANGE
                        val dataModelVal = totalVal * fractionDiff
                        it.titleView.text = getStatTitle(titleText, absoluteDiff)
                        it.titleNumView.text = getStatTitleNum(dataModelVal, totalVal)

                        // publish the change and update model
                        it.dataModel = it.dataModel.copy(second = dataModelVal.roundToDecimalPlaces(2))
                        statsChangePublisher.accept(mapOf(it.dataModel.first to it.dataModel.second!!))
                    }
                }
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false // never intercept
    }

}