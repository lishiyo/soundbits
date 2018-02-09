package com.cziyeli.commons

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.support.annotation.ColorRes
import android.support.v4.content.ContextCompat
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * Created by connieli on 1/4/18.
 */

const val DTAG = "connie"

/**
 * Basic observe/subscribe on pair.
 */
val schedulersTransformer: ObservableTransformer<Any, Any> = ObservableTransformer { observable: Observable<Any> ->
    observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}
fun <T> applySchedulers(): ObservableTransformer<T, T> {
    return schedulersTransformer as ObservableTransformer<T, T>
}

/**
 * Strip out any [MviAction]/[MviResult] not of the relevant type (most commonly for [None]).
 */
inline fun <reified T> actionFilter(): ObservableTransformer<MviAction, T> = ObservableTransformer { actions ->
    actions.filter { it is T }.map { it as T }
}

inline fun <reified T> resultFilter(): ObservableTransformer<MviResult, T> = ObservableTransformer { results ->
    results.filter { it is T }.map { it as T }
}

// ===== VIEWS ======

fun Any.toast(context: Context, length: Int = Toast.LENGTH_LONG) {
    Toast.makeText(context, this.toString(), length).show()
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




// A LiveData that only allows distinct object emissions from a source.
// https://medium.com/google-developers/7-pro-tips-for-room-fbadea4bfbd1
fun <T> LiveData<T>.getDistinct(): LiveData<T> {
    val distinctLiveData = MediatorLiveData<T>()
    distinctLiveData.addSource(this, object : Observer<T> {
        private var initialized = false
        private var lastObj: T? = null
        override fun onChanged(obj: T?) {
            if (!initialized) {
                initialized = true
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            } else if ((obj == null && lastObj != null)
                    || obj != lastObj) {
                lastObj = obj
                distinctLiveData.postValue(lastObj)
            }
        }
    })
    return distinctLiveData
}
