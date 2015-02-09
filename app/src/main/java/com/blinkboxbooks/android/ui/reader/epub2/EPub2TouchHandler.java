// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;

import com.blinkboxbooks.android.R;

/**
 * Class for processing touch events on the ReaderFragment
 */
public class EPub2TouchHandler implements OnTouchListener {

    private EPub2ReaderController mReaderController;
    private EPub2ReaderCallback mReaderCallback;

    private int mScreenWidth;
    private int mMinDragDistance;
    private float mDownX = 0;
    private float mDownY = 0;

    private boolean mAllowInteraction = true;

    private float mDensity;

    public EPub2TouchHandler(Context context, EPub2ReaderController readerController, EPub2ReaderCallback readerCallback) {
        mDensity = context.getResources().getDisplayMetrics().density;

        mReaderController = readerController;
        mReaderCallback = readerCallback;

        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        Point point = new Point();
        manager.getDefaultDisplay().getSize(point);

        mScreenWidth = point.x;
        mMinDragDistance = context.getResources().getDimensionPixelOffset(R.dimen.reader_minimum_swipe_distance);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {

        if (!mAllowInteraction) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mDownX = event.getX();
            mDownY = event.getY();
        } else if (mDownX != 0 && event.getAction() == MotionEvent.ACTION_UP) {
            float deltaX = mDownX - event.getX();
            float deltaY = mDownY - event.getY();

            if ((Math.abs(deltaY) < Math.abs(deltaX) * 2) && // horizontal
                    (Math.abs(deltaX) > mMinDragDistance)) {

                if (deltaX > 0) {
                    mReaderController.nextPage();
                } else {
                    mReaderController.prevPage();
                }

                return true;
            }
        }

        return false;
    }

    public void processTap(float x) {
        if (mAllowInteraction) {
            x *= mDensity;

            double percentage = getXPercentage(x);

            if (percentage < 25) {
                mReaderController.prevPage();
            } else if (percentage > 75) {
                mReaderController.nextPage();
            } else {
                mReaderCallback.toggleReaderOverlayVisiblity();
            }
        }
    }

    /**
     * Set the touch handler to allow or disallow user interaction
     * @param allowInteraction true to allow interaction
     */
    public void setInteractionAllowed(boolean allowInteraction) {
        mAllowInteraction = allowInteraction;
    }

    private double getXPercentage(float x) {
        return (x / mScreenWidth) * 100;
    }
}