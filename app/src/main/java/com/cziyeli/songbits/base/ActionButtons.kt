package com.cziyeli.songbits.base

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.support.annotation.ColorRes
import android.support.v4.content.res.ResourcesCompat
import android.util.AttributeSet
import android.widget.Button
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


/**
 * Just a button with rounded borders.
 */
class WideButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Button(context, attrs, defStyle, defStyleRes) {
    private val DEFAULT_FONT = R.font.indie_flower
    private val BACKGROUND_DRAWABLE: GradientDrawable = (resources.getDrawable(R.drawable.rounded_borders_button) as RippleDrawable)
            .getDrawable(0) as GradientDrawable
    private var borderColorRes: Int? = resources.getColor(R.color.colorDarkerGrey)

    init {
        background = BACKGROUND_DRAWABLE
        var typeface = ResourcesCompat.getFont(context, DEFAULT_FONT)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                    R.styleable.action_buttons, 0, 0)
            borderColorRes = typedArray.getColor(R.styleable.action_buttons_rounded_border_color,
                    resources.getColor(R.color.colorDarkerGrey))
            typeface = ResourcesCompat.getFont(context, typedArray.getResourceId(R.styleable.action_buttons_font, R.font.quicksand))

            typedArray.recycle()
        }
        borderColorRes?.run {
            BACKGROUND_DRAWABLE.setStroke(1, this)
        }

        setTypeface(typeface)
    }

    /**
     * Set the background color, as well as border color (optional).
     */
    fun setBorderColor(@ColorRes borderColorRes: Int? = null) {
        borderColorRes?.run {
            BACKGROUND_DRAWABLE.setStroke(1, this)
        }
        invalidate()
    }
}

/**
 * Just a button with rounded borders.
 */
class RoundedCornerButton @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0,
        defStyleRes: Int = 0
) : Button(context, attrs, defStyle, defStyleRes) {
    private val DEFAULT_FONT = R.font.indie_flower
    private val BACKGROUND_DRAWABLE: GradientDrawable = resources.getDrawable(R.drawable.rounded_borders) as GradientDrawable
    private var backgroundColorRes : Int? = null
    private var borderColorRes : Int? = null

    init {
        background = BACKGROUND_DRAWABLE
        var typeface = ResourcesCompat.getFont(context, DEFAULT_FONT)

        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it,
                    R.styleable.action_buttons, 0, 0)
            backgroundColorRes = typedArray.getColor(R.styleable.action_buttons_bg_color, resources.getColor(R.color.colorPrimaryShade))
            borderColorRes = typedArray.getColor(R.styleable.action_buttons_rounded_border_color,
                    resources.getColor(R.color.colorPrimaryShade))
            typeface = ResourcesCompat.getFont(context, typedArray.getResourceId(R.styleable.action_buttons_font, R.font.quicksand))

            typedArray.recycle()
        }
        backgroundColorRes?.run {
            BACKGROUND_DRAWABLE.setColor(this)
        }
//        borderColorRes?.run {
//            BACKGROUND_DRAWABLE.setStroke(1, this)
//        }

        setTypeface(typeface)
    }

    /**
     * Set the background color, as well as border color (optional).
     */
    fun setColor(@ColorRes colorRes: Int, @ColorRes borderColorRes: Int? = null) {
        BACKGROUND_DRAWABLE.setColor(resources.getColor(colorRes))
        borderColorRes?.run {
            BACKGROUND_DRAWABLE.setStroke(1, this)
        }
        invalidate()
    }
}