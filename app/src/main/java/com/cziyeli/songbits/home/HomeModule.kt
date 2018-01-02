package com.cziyeli.songbits.home

import com.cziyeli.data.Repository
import com.cziyeli.domain.playlists.PlaylistActionProcessor
import com.cziyeli.songbits.di.PerActivity
import dagger.Module
import dagger.Provides
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import lishiyo.kotlin_arch.utils.schedulers.SchedulerProvider

/**
 * Created by connieli on 12/31/17.
 */

@Module
class HomeModule {

    @Provides
    @PerActivity
    fun providePlaylistActionProcessor(repo: Repository, schedulerProvider: BaseSchedulerProvider)
            : PlaylistActionProcessor {
        return PlaylistActionProcessor(repo, SchedulerProvider)
    }

}