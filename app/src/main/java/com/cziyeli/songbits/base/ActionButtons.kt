package com.cziyeli.songbits.base

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import com.cziyeli.songbits.R


class LikeButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : ImageView(context, attrs, defStyle, defStyleRes) {
    companion object {
        const val ACTIVE_DRAWABLE = R.drawable.like_line_dazzle_filled
        const val INACTIVE_DRAWABLE = R.drawable.like_line_dazzle
    }

    init {
        var isActive = true
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                    R.styleable.action_buttons, 0, 0)
            isActive = typedArray.getBoolean(R.styleable.action_buttons_active, true)
            typedArray.recycle()
        }

        setActive(isActive)
    }

    // Toggle filled or unfilled
    fun setActive(active: Boolean) {
        val drawable = if (active) ACTIVE_DRAWABLE else INACTIVE_DRAWABLE
        setImageResource(drawable)
    }
}

class DisLikeButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : ImageView(context, attrs, defStyle, defStyleRes) {
    companion object {
        const val ACTIVE_DRAWABLE = R.drawable.dislike_smiley
        const val INACTIVE_DRAWABLE = R.drawable.dislike_smiley_line
    }

    init {
        var isActive = true
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                    R.styleable.action_buttons, 0, 0)
            isActive = typedArray.getBoolean(R.styleable.action_buttons_active, true)
            typedArray.recycle()
        }

        setActive(isActive)
    }

    // Toggle filled or unfilled
    fun setActive(active: Boolean) {
        val drawable = if (active) ACTIVE_DRAWABLE else INACTIVE_DRAWABLE
        setImageResource(drawable)
    }
}

