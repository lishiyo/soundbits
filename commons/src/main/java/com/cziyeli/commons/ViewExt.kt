package com.cziyeli.commons

import android.content.Context
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import es.dmoral.toasty.Toasty

/**
 * View-related extensions.
 *
 * Created by connieli on 2/16/18.
 */
fun Any.toast(context: Context, length: Int = Toast.LENGTH_LONG) {
    Toasty.success(context, this.toString(), Toast.LENGTH_LONG, true).show()
}

fun Any.errorToast(context: Context, length: Int = Toast.LENGTH_LONG) {
    Toasty.error(context, this.toString(), Toast.LENGTH_LONG, true).show()
}

fun Context.fetchColor(@ColorRes color: Int): Int {
    return ContextCompat.getColor(this, color)
}

fun View.disableTouchTheft() {
    this.setOnTouchListener { view, motionEvent ->
        view.parent.requestDisallowInterceptTouchEvent(true)

        when (motionEvent.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_UP -> view.parent.requestDisallowInterceptTouchEvent(false)
        }
        false
    }
}

fun Context.colorFromRes(colorResId: Int) : Int {
    return ContextCompat.getColor(this, colorResId)
}

fun View.colorFromRes(colorResId: Int) : Int {
    return ContextCompat.getColor(this.context, colorResId)
}

fun View.removeFromParent() {
    val parent = this.parent
    if (parent is ViewGroup) {
        parent.removeView(this)
    }
}
fun View.setScaleXY(scale: Float) {
    scaleX = scale
    scaleY = scale
}

