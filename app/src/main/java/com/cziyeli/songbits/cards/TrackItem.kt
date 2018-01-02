package com.cziyeli.songbits.cards

import android.content.Context
import android.widget.TextView
import com.cziyeli.commons.Utils
import com.cziyeli.domain.tracks.TrackCard
import com.cziyeli.songbits.R
import com.facebook.drawee.view.SimpleDraweeView
import com.mindorks.placeholderview.SwipePlaceHolderView
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.mindorks.placeholderview.annotations.swipe.*

/**
 * Created by connieli on 1/2/18.
 */
@Layout(R.layout.cards_track_view)
class TrackItem(private val context: Context,
                private val model: TrackCard,
                private val swipeView: SwipePlaceHolderView) {

    @View(R.id.image)
    private lateinit var imageView: SimpleDraweeView

    @View(R.id.track_name)
    private lateinit var trackName: TextView

    @View(R.id.artist_name)
    private lateinit var artistName: TextView

    @SwipeView
    private val cardView: android.view.View? = null

    @Resolve
    private fun onResolved() {
        trackName.text = model.name
        artistName.text = model.artist?.name

        imageView.setImageURI(model.coverImage?.url)
    }

    @SwipeHead
    private fun onSwipeHeadCard() {
//        Glide.with(mContext).load(mProfile.getImageUrl())
//                .bitmapTransform(RoundedCornersTransformation(
//                        mContext, Utils.dpToPx(7), 0,
//                        RoundedCornersTransformation.CornerType.TOP))
//                .into(profileImageView)
//        cardView!!.invalidate()
    }

    @Click(R.id.image)
    private fun onClick() {
        Utils.log("image click")
    }

    @SwipeOut
    private fun onSwipedOut() {
        Utils.log("onSwipedOut")
    }

    @SwipeCancelState
    private fun onSwipeCancelState() {
        Utils.log("onSwipeCancelState")
    }

    @SwipeIn
    private fun onSwipeIn() {
        Utils.log("onSwipeIn")
    }

    @SwipeInState
    private fun onSwipeInState() {
        Utils.log("onSwipeInState")
    }

    @SwipeOutState
    private fun onSwipeOutState() {
        Utils.log("onSwipeOutState")
    }
}
