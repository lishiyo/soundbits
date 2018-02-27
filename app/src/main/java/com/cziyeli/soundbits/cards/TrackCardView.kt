package com.cziyeli.soundbits.cards

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.cziyeli.commons.Utils
import com.cziyeli.domain.player.PlayerInterface
import com.cziyeli.domain.tracks.TrackModel
import com.cziyeli.soundbits.R
import com.jakewharton.rxrelay2.PublishRelay
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.SwipeCancelState
import com.mindorks.placeholderview.annotations.swipe.SwipeHead
import com.mindorks.placeholderview.annotations.swipe.SwipeIn
import com.mindorks.placeholderview.annotations.swipe.SwipeOut



/**
 * Displays a "card" view of a track for swiping.
 *
 * Created by connieli on 1/2/18.
 */
@Layout(R.layout.cards_track_view)
class TrackCardView(private val context: Context,
                    private val model: TrackModel,
                    private val listener: TrackListener) {

    private val TAG = TrackCardView::class.simpleName

    @View(R.id.track_image_container)
    private lateinit var trackImageContainer: android.view.View

    @View(R.id.track_image)
    private lateinit var trackImage: ImageView

    @View(R.id.track_play_pause)
    private lateinit var trackPlayPause: ImageView

    @View(R.id.track_name)
    private lateinit var trackName: TextView

    @View(R.id.artist_name)
    private lateinit var artistName: TextView

    @View(R.id.track_title_wrapper)
    private lateinit var trackTitleWrapper: android.view.View

    // Flag to flip the icon
    private var isPlaying: Boolean = true

    private val trackImageClickListener = android.view.View.OnClickListener {
        // pause/resume
        listener.getPlayerIntents()
                .accept(CardsIntent.CommandPlayer.create(PlayerInterface.Command.PAUSE_OR_RESUME, model))

        // switch icon
        val icon = if (isPlaying) R.drawable.basic_pause else R.drawable.basic_play
        trackPlayPause.setImageResource(icon)
        isPlaying = !isPlaying
    }

    private val transform = RequestOptions().transforms(CenterCrop(), RoundedCorners(12))

    @Resolve
    private fun onResolved() {
        Utils.log(TAG, "onResolved: ${model.name}") // on average will load 3

        trackName.text = model.name
        artistName.text = model.artistName
        trackImage.setOnClickListener(trackImageClickListener)

        Glide.with(context)
                .load(model.imageUrl)
                .apply(transform)
                .into(trackImage)
    }

    @SwipeHead
    private fun onSwipeHeadCard() {
        // a card comes on top of the stack (follows onResolved)
        Utils.log(TAG, "onSwipeHeadCard: ${model.name} -- clearing pref")

        // immediately start playing
        listener.getPlayerIntents().accept(
                CardsIntent.CommandPlayer.create(PlayerInterface.Command.PLAY_NEW, model))

        // 'undo' clears liked/disliked pref
        listener.onChangePref(model, TrackModel.Pref.UNSEEN)

        trackImageContainer.alpha = 1f
        Utils.setVisible(trackTitleWrapper, true)
        Utils.setVisible(trackPlayPause, true)
    }

    @SwipeOut
    private fun onSwipedOut() {
        Utils.log(TAG, "onSwipedOut (rejected): ${model.name}")

        finishTrack()
        listener.onChangePref(model, TrackModel.Pref.DISLIKED)
    }

    @SwipeIn
    private fun onSwipeIn() {
        Utils.log(TAG, "onSwipeIn (accepted): ${model.name}")

        finishTrack()
        listener.onChangePref(model, TrackModel.Pref.LIKED)
    }

    @SwipeCancelState
    private fun onSwipeCancelState() {
        Utils.log(TAG, "onSwipeCancelState")
    }

    @Click(R.id.btn_discard)
    private fun onDisliked() {
        Utils.log(TAG, "onDislikeClicked: ${model.name}")

        listener.doSwipe(TrackModel.Pref.DISLIKED)
    }

    @Click(R.id.btn_like)
    private fun onLiked() {
        Utils.log(TAG, "onLikeClicked: ${model.name}")
        listener.doSwipe(TrackModel.Pref.LIKED)
    }

    @Click(R.id.btn_undo)
    private fun onUndoClicked() {
        Utils.log(TAG, "onUndoClicked: ${model.name}")
        listener.doSwipe(TrackModel.Pref.UNSEEN)
    }

    private fun finishTrack() {
        // stop playing this track
        listener.getPlayerIntents().accept(
                CardsIntent.CommandPlayer.create(PlayerInterface.Command.END_TRACK, model))

        Utils.setVisible(trackTitleWrapper, false)
        trackImage.setOnClickListener(null)
    }

    interface TrackListener {
        // emit events to the audio player
        fun getPlayerIntents(): PublishRelay<CardsIntent.CommandPlayer>

        // clicked like or dislike
        fun onChangePref(model: TrackModel, pref: TrackModel.Pref)

        // perform a swipe in/out/undo
        fun doSwipe(pref: TrackModel.Pref)
    }
}
