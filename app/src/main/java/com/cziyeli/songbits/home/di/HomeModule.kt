package com.cziyeli.songbits.home.di

import com.cziyeli.data.Repository
import com.cziyeli.domain.playlists.PlaylistActionProcessor
import com.cziyeli.songbits.di.PerActivity
import dagger.Module
import dagger.Provides
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * Created by connieli on 12/31/17.
 */

@Module
class HomeModule {

    @Provides
    @PerActivity
    fun providePlaylistActionProcessor(repo: Repository, schedulerProvider: BaseSchedulerProvider)
            : PlaylistActionProcessor {
        return PlaylistActionProcessor(repo, schedulerProvider)
    }

}