package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class BBBViewPager extends ViewPager {

    private boolean mSwipeEnabled = true;

    public BBBViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setSwipeEnabled(boolean swipeEnabled) {
        mSwipeEnabled = swipeEnabled;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        if(mSwipeEnabled) {
            return super.onInterceptTouchEvent(ev);
        } else {
            return false;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if(mSwipeEnabled) {
            return super.onTouchEvent(ev);
        } else {
            return false;
        }
    }
}
