package com.cziyeli.spotifydemo.di

import javax.inject.Scope

/**
 * Created by connieli on 12/31/17.
 */
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class UserScope

@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerActivity