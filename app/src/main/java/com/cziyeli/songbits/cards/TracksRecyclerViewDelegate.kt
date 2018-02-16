package com.cziyeli.songbits.cards

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.cziyeli.commons.Utils
import com.cziyeli.songbits.R
import com.nikhilpanju.recyclerviewenhanced.RecyclerTouchListener
import eu.gsottbauer.equalizerview.EqualizerView

/**
 * Wrapper for the tracks view handling for [SimpleCardWidget].
 */
class TracksRecyclerViewDelegate(val activity: Activity,
                                 private val tracksRecyclerView: RecyclerView,
                                 private val listener: ActionButtonListener
) {
    private val TAG = TracksRecyclerViewDelegate::class.java.simpleName

    private val onSwipeListener: RecyclerTouchListener.OnSwipeListener = createOnSwipeListener()
    val onTouchListener: RecyclerTouchListener = createOnTouchListener(onSwipeListener)

    private fun createOnSwipeListener() : RecyclerTouchListener.OnSwipeListener {
        return object : RecyclerTouchListener.OnSwipeListener {
            override fun onForegroundAnimationStart(isFgOpening: Boolean, duration: Long, foregroundView: View, backgroundView: View?) {
                // shrink the textview size
                val scale = if (isFgOpening) 0.7f else 1.0f
                val parentView = foregroundView as ViewGroup
                val shrinkingViews = listOf<View>(
                        parentView.findViewById(R.id.track_left_container),
                        parentView.findViewById(R.id.track_image)
                )
                shrinkingViews.forEach { view ->
                    view.pivotX = 0f
                    view.animate()
                            .scaleX(scale)
                            .scaleY(scale)
                            .setDuration(duration)
                            .start()
                }

                // animate the wave
                val toAlpha = if (isFgOpening) 1.0f else 0.0f
                val animatedView = foregroundView.findViewById<EqualizerView>(R.id.equalizer_animation)
//                val animatedView = foregroundView.findViewById<LottieAnimationView>(R.id.wave_animation)
                animatedView.animate().alpha(toAlpha).withEndAction {
                    if (isFgOpening) {
                        animatedView.visibility = View.VISIBLE
                        animatedView.animateBars()
//                        animatedView.playAnimation()
                    } else {
                        animatedView.visibility = View.INVISIBLE
                        animatedView.stopBars()
//                        animatedView.pauseAnimation()
                    }
                }.setDuration(duration).start()
            }

            override fun onSwipeOptionsOpened(foregroundView: View, backgroundView: View?) {

            }

            override fun onSwipeOptionsClosed(foregroundView: View, backgroundView: View?) {
                val animatedView = foregroundView.findViewById<EqualizerView>(R.id.equalizer_animation)
                animatedView.stopBars()
            }
        }
    }

    private fun createOnTouchListener(swipeListener: RecyclerTouchListener.OnSwipeListener) : RecyclerTouchListener {
        val onTouchListener = RecyclerTouchListener(activity, tracksRecyclerView)
        onTouchListener
                .setViewsToFade(R.id.track_status)
                .setClickable(object : RecyclerTouchListener.OnRowClickListener {
                    override fun onRowClicked(position: Int) {
                        Utils.mLog(TAG, "row @ ${position} clicked")
                    }

                    override fun onIndependentViewClicked(independentViewID: Int, position: Int) {
                        Utils.mLog(TAG,"independent view @ ${position} clicked")
                    }
                })
                .setOnSwipeListener(swipeListener)
                .setSwipeOptionViews(R.id.like_icon_container, R.id.dislike_icon_container)
                .setSwipeable(R.id.row_foreground, R.id.row_background) { viewID, position ->
                    when (viewID) {
                        R.id.like_icon_container -> {
                            listener.onLiked(position)
                        }
                        R.id.dislike_icon_container -> {
                            listener.onDisliked(position)
                        }
                    }
                }

        return onTouchListener
    }

    /**
     * Listen to the like/dislike actions.
     */
    interface ActionButtonListener {
        fun onLiked(position: Int = -1)
        fun onDisliked(position: Int = -1)
    }

}