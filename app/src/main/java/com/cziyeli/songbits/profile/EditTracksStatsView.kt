package com.cziyeli.songbits.profile

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.cziyeli.songbits.playlistcard.TrackStatsView
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

    init {
        range_one.onChangeRangeListener = object : SimpleRangeView.OnChangeRangeListener {
            override fun onRangeChanged(@NotNull rangeView: SimpleRangeView, start: Int, end: Int) {
                // figure out range
                val diff = ((end - start) / TRACK_RANGE.toDouble())
                title_row_one.text = getStatTitle("danceable", diff * TRACK_RANGE)
                title_row_one_number.text = getStatTitleNum(diff, 1.0)
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false // never intercept
    }

}