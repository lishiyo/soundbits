package com.cziyeli.commons

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import com.cziyeli.commons.mvibase.MviAction
import com.cziyeli.commons.mvibase.MviResult
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal


/**
 * General extensions.
 *
 * Created by connieli on 1/4/18.
 */

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

// ==== Primitive utils ===

fun Double.roundToDecimalPlaces(numPlaces: Int) =
        BigDecimal(this).setScale(numPlaces, BigDecimal.ROUND_HALF_UP).toDouble()
