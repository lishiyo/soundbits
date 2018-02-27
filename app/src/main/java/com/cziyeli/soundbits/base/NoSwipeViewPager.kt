package com.cziyeli.soundbits.base

import android.content.Context
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.MotionEvent

/**
 * Root view pager that disables swipe.
 */
class NoSwipePager @JvmOverloads constructor(var pagingEnabled: Boolean = false,
                                             context: Context,
                                             attrs: AttributeSet
) : ViewPager(context, attrs) {

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (pagingEnabled) {
            super.onTouchEvent(event)
        } else false
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        return if (pagingEnabled) {
            super.onInterceptTouchEvent(event)
        } else false
    }
}