package com.cziyeli.domain.user

import com.cziyeli.data.Repository
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by connieli on 2/18/18.
 */
@Singleton
class ProfileActionProcessor @Inject constructor(private val repository: Repository,
                                                 private val schedulerProvider: BaseSchedulerProvider,
                                                 private val userManager: UserManager) {
    private val TAG = ProfileActionProcessor::class.java.simpleName

}