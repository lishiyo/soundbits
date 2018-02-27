package com.cziyeli.soundbits.di.viewModels

import android.arch.lifecycle.ViewModel
import dagger.MapKey
import kotlin.reflect.KClass

/**
 * Created by connieli on 1/2/18.
 */

@MustBeDocumented
@Target(
        AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER,
        AnnotationTarget.PROPERTY_SETTER
)
@kotlin.annotation.Retention()
@MapKey
annotation class ViewModelKey(
        val value: KClass<out ViewModel>)