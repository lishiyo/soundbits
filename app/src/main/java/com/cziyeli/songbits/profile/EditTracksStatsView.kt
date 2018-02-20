package com.cziyeli.songbits.profile

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.TextView
import com.cziyeli.songbits.playlistcard.TrackStatsView
import kotlinx.android.synthetic.main.widget_card_stats.view.*
import me.bendik.simplerangeview.SimpleRangeView
import org.jetbrains.anko.collections.forEachWithIndex
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

    init {
        val ranges: List<SimpleRangeView> = listOf(range_one, range_two, range_three)
        val titleViews: List<TextView> = listOf(title_row_one, title_row_two, title_row_three)
        val titleNumViews: List<TextView> = listOf(title_row_one_number, title_row_two_number, title_row_three_number)
        ranges.forEachWithIndex { index, range ->
            val (title, totalView) = TRACK_STATS_MAP[index]
            val titleView = titleViews[index]
            val titleNumView = titleNumViews[index]
            range.onChangeRangeListener = object : SimpleRangeView.OnChangeRangeListener {
                override fun onRangeChanged(@NotNull rangeView: SimpleRangeView, start: Int, end: Int) {
                    val absoluteDiff = (end - start).toDouble()
                    val diff = absoluteDiff / TRACK_RANGE
                    titleView.text = getStatTitle(title, absoluteDiff)
                    titleNumView.text = getStatTitleNum(diff, totalView)
                }
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false // never intercept
    }

}