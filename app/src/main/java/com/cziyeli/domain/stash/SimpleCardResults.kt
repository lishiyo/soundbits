package com.cziyeli.domain.stash

import com.cziyeli.commons.mvibase.MviResult
import com.cziyeli.domain.playlistcard.CardResultMarker

sealed class SimpleCardResult(var status: MviResult.StatusInterface = MviResult.Status.IDLE,
                              var error: Throwable? = null) : CardResultMarker {

    // Start the "create" mode of a SimpleCard after selecting 'create' in the fab menu
    // Empty edit title, fab icon now the create icon
    class SetCreateMode(val inCreateMode: Boolean = true) : SimpleCardResult(MviResult.Status.SUCCESS, null)
}
