package com.cziyeli.commons.di

import javax.inject.Qualifier
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


// PerFragment.java
@Scope
@Retention(AnnotationRetention.RUNTIME)
annotation class PerFragment

@Qualifier
@Retention( AnnotationRetention.RUNTIME )
annotation class ForApplication