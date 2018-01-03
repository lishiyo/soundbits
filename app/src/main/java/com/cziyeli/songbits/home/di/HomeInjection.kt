package com.cziyeli.songbits.home.di

import com.cziyeli.commons.di.PerActivity
import com.cziyeli.data.Repository
import com.cziyeli.domain.playlists.PlaylistActionProcessor
import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import com.cziyeli.songbits.home.HomeActivity
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider

/**
 * HomeActivity specific instances.
 *
 * Created by connieli on 12/31/17.
 */
@Module(includes = [ViewModelsModule::class])
class HomeModule {

    @Provides
    @PerActivity
    fun providePlaylistActionProcessor(repo: Repository, schedulerProvider: BaseSchedulerProvider)
            : PlaylistActionProcessor {
        return PlaylistActionProcessor(repo, schedulerProvider)
    }
}


@Subcomponent(modules = [HomeModule::class])
interface HomeActivitySubComponent : AndroidInjector<HomeActivity> {

    @Subcomponent.Builder
    abstract class builder : AndroidInjector.Builder<HomeActivity>()

}