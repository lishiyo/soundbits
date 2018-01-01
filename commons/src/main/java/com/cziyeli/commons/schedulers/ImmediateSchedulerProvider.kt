package lishiyo.kotlin_arch.utils.schedulers

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import javax.inject.Singleton

/**
 * Implementation of the [BaseSchedulerProvider] making all [Scheduler]s immediate.
 */
@Singleton
object ImmediateSchedulerProvider : BaseSchedulerProvider {

    override fun computation(): Scheduler {
        return Schedulers.trampoline()
    }

    override fun io(): Scheduler {
        return Schedulers.trampoline()
    }

    override fun ui(): Scheduler {
        return Schedulers.trampoline()
    }
}
