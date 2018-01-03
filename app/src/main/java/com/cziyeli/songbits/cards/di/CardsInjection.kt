package com.cziyeli.songbits.cards.di

import android.app.Activity
import android.content.Context
import com.cziyeli.commons.di.ForApplication
import com.cziyeli.commons.di.PerActivity
import com.cziyeli.data.Repository
import com.cziyeli.domain.player.NativePlayerManager
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.tracks.TrackActionProcessor
import com.cziyeli.songbits.cards.CardsActivity
import com.cziyeli.songbits.di.viewModels.ViewModelsModule
import dagger.Module
import dagger.Provides
import dagger.Subcomponent
import dagger.android.AndroidInjector
import lishiyo.kotlin_arch.utils.schedulers.BaseSchedulerProvider
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by connieli on 1/1/18.
 */

@Module(includes = [ViewModelsModule::class])
class CardsModule {

    @Provides
    @PerActivity
    fun provideActionProcessor(repo: Repository, schedulerProvider: BaseSchedulerProvider)
            : TrackActionProcessor {
        return TrackActionProcessor(repo, schedulerProvider)
    }


//    @Provides
//    @PerActivity
//    fun provideSpotifyPlayerManager(@Named("ActivityContext") activity: Activity, accessToken: String) : SpotifyPlayerManager {
//        return SpotifyPlayerManager(activity, accessToken)
//    }

    @Provides
    @Named("ActivityContext")
    fun provideActivityContext(canvasActivity: CardsActivity): Activity {
        return canvasActivity
    }

}


@Subcomponent(modules = [CardsModule::class])
interface CardsActivitySubComponent : AndroidInjector<CardsActivity> {

    @Subcomponent.Builder
    abstract class builder : AndroidInjector.Builder<CardsActivity>()

}
