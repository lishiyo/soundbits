package com.cziyeli.commons

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.widget.Toast
import io.reactivex.Observable
import io.reactivex.ObservableTransformer
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


/**
 * Created by connieli on 1/4/18.
 */
fun Any.toast(context: Context) {
    Toast.makeText(context, this.toString(), Toast.LENGTH_SHORT).show()
}

// Mediator LiveData that only allows distinct object emissions from a source.
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

val schedulersTransformer: ObservableTransformer<Any, Any> = ObservableTransformer { observable: Observable<Any> ->
    observable.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}

fun <T> applySchedulers(): ObservableTransformer<T, T> {
    return schedulersTransformer as ObservableTransformer<T, T>
}