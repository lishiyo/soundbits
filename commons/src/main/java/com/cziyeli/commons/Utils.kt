package com.cziyeli.commons

import android.content.Context
import android.content.res.Resources
import android.graphics.Point
import android.os.Parcel
import android.os.Parcelable
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager


/**
 * Created by connieli on 12/31/17.
 */
object Utils {
    fun log(status: String) {
        Log.i(TAG, status)
    }

    fun log(tag: String?, status: String) {
        if (tag == null) {
            log(status)
        } else {
            Log.i(TAG, "$tag -- $status")
        }
    }

    fun mLog(tag: String?, methodName: String?, vararg extras: String?) {
        val message = "${methodName ?: ""} -- ${extras.joinToString(postfix = ": ")}"
        log(tag, message)
    }

    inline fun <reified T : Parcelable> createParcel(crossinline createFromParcel: (Parcel) -> T?)
            : Parcelable.Creator<T> = object : Parcelable.Creator<T> {
                override fun createFromParcel(source: Parcel): T? = createFromParcel(source)
                override fun newArray(size: Int): Array<out T?> = arrayOfNulls(size)
            }

    // ======= RESOURCE UTILS =======

    fun setVisible(view: View, isVisible: Boolean) {
        val visibility = if (isVisible) View.VISIBLE else View.GONE
        view.visibility = visibility
    }

    fun getDisplaySize(windowManager: WindowManager): Point {
        return try {
            val display = windowManager.defaultDisplay
            val displayMetrics = DisplayMetrics()
            display.getMetrics(displayMetrics)
            Point(displayMetrics.widthPixels, displayMetrics.heightPixels)
        } catch (e: Exception) {
            e.printStackTrace()
            Point(0, 0)
        }
    }

    fun dpToPx(dp: Int): Int {
        return (dp * Resources.getSystem().displayMetrics.density).toInt()
    }

    fun hideKeyboard(context: Context, focusedView: View) {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        imm?.hideSoftInputFromWindow(focusedView.windowToken, 0)
    }

    fun showKeyboard(context: Context, focusedView: View) {
        focusedView.post({
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(focusedView, InputMethodManager.SHOW_IMPLICIT)
        })
    }

    fun <T> unlessEmpty(list: List<*>, defaultVal: T, fn: (List<*>) -> T): T {
        return if (list.isEmpty()) {
            defaultVal
        } else {
            fn(list)
        }
    }

    fun getRandomGenreSeeds(count: Int = 5) : List<String> {
        return GENRE_SEEDS.shuffled().take(count)
    }

}