package com.happybird.photosender.presentation

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class PaddingDivider(
    val mLeft: Int,
    val mTop: Int,
    val mRight: Int,
    val mBottom: Int
): RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.run {
            left = mLeft
            top = mTop
            right = mRight
            bottom = mBottom
        }
    }
}