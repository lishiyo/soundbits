package com.cziyeli.songbits.cards

import android.content.Context
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.tracks.TrackCard
import com.cziyeli.songbits.R
import com.facebook.drawee.view.SimpleDraweeView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.*
import io.reactivex.subjects.PublishSubject

/**
 * Displays a "card" view of a track.
 * Created by connieli on 1/2/18.
 */
@Layout(R.layout.cards_track_view)
class TrackItem(private val context: Context,
                private val model: TrackCard,
                private val listener: TrackListener) {

    private val TAG = TrackItem::class.simpleName

    @View(R.id.image)
    private lateinit var imageView: SimpleDraweeView

    @View(R.id.track_name)
    private lateinit var trackName: TextView

    @View(R.id.artist_name)
    private lateinit var artistName: TextView

    @Resolve
    private fun onResolved() {
        trackName.text = model.name
        artistName.text = model.artist?.name
        imageView.setImageURI(model.coverImage?.url)
        Utils.log(TAG, "onResolved: ${model.name}") // on average will load 3
    }

    @SwipeHead
    private fun onSwipeHeadCard() {
        // a card comes on top of the stack
        Utils.log(TAG, "onSwipeHeadCard: ${model.name}")
        listener.getCommandsStream().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.PLAY_NEW, model))
    }

    @Click(R.id.image)
    private fun onClick() {
        Utils.log(TAG, "onClick image")
        listener.getCommandsStream().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.PAUSE_OR_RESUME, model))
    }

    @SwipeOut
    private fun onSwipedOut() {
        Utils.log(TAG, "onSwipedOut (rejected) ")
        listener.getCommandsStream().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))
    }

    @SwipeCancelState
    private fun onSwipeCancelState() {
//        Utils.log(TAG, "onSwipeCancelState")
    }

    @SwipeIn
    private fun onSwipeIn() {
        Utils.log(TAG, "onSwipeIn (accepted) ")
        listener.getCommandsStream().onNext(
                TrackIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))
    }

    @SwipeInState
    private fun onSwipeInState() {
//        Utils.log(TAG, "onSwipeInState")
    }

    @SwipeOutState
    private fun onSwipeOutState() {
//        Utils.log(TAG, "onSwipeOutState")
    }

    interface TrackListener {
        fun getCommandsStream(): PublishSubject<TrackIntent.CommandPlayer>
    }
}
