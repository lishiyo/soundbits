package com.cziyeli.commons

import android.annotation.TargetApi
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by connieli on 12/31/17.
 */
private class SharedPreferenceDelegate<T>(
        private val context: Context,
        private val defaultValue: T,
        private val getter: SharedPreferences.(String, T) -> T,
        private val setter: SharedPreferences.Editor.(String, T) -> SharedPreferences.Editor,
        private val key: String
) : ReadWriteProperty<Any, T> {

    private val safeContext: Context by lazy { context.safeContext() }

    private val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(safeContext)
    }

    override fun getValue(thisRef: Any, property: KProperty<*>) =
            sharedPreferences
                    .getter(key, defaultValue)

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T) =
            sharedPreferences
                    .edit()
                    .setter(key, value)
                    .apply()
}

@TargetApi(Build.VERSION_CODES.N)
fun Context.safeContext(): Context =
        takeUnless { isDeviceProtectedStorage }?.run {
            this.applicationContext.let {
                ContextCompat.createDeviceProtectedStorageContext(it) ?: it
            }
        } ?: this

@Suppress("UNCHECKED_CAST")
fun <T> bindSharedPreference(context: Context, key: String, defaultValue: T): ReadWriteProperty<Any, T> =
        when (defaultValue) {
            is Boolean ->
                SharedPreferenceDelegate(context, defaultValue, SharedPreferences::getBoolean, SharedPreferences.Editor::putBoolean, key)
            is Int ->
                SharedPreferenceDelegate(context, defaultValue, SharedPreferences::getInt, SharedPreferences.Editor::putInt, key)
            is Long ->
                SharedPreferenceDelegate(context, defaultValue, SharedPreferences::getLong, SharedPreferences.Editor::putLong, key)
            is Float ->
                SharedPreferenceDelegate(context, defaultValue, SharedPreferences::getFloat, SharedPreferences.Editor::putFloat, key)
            is String ->
                SharedPreferenceDelegate(context, defaultValue, SharedPreferences::getString, SharedPreferences.Editor::putString, key)
            else -> throw IllegalArgumentException()
        } as ReadWriteProperty<Any, T>