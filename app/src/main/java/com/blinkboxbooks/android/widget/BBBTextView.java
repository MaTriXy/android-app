// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.cache.TypefaceCache;

/**
 * Custom TextView class which simplifies setting a custom font. Font is set via custom view attributes
 */
public class BBBTextView extends TextView {

    private boolean mIsLink = false;
    private Typeface mFont;
    private Typeface mFontSelected;

    public BBBTextView(Context context) {
        super(context);
    }

    public BBBTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode()) {
            setCustomAttributes(context, attrs);
        }
    }

    public BBBTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        if (!isInEditMode()) {
            setCustomAttributes(context, attrs);
        }
    }

    /*
     * Sets the custom font
     */
    private void setCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BBBTextView, 0, 0);

        try {
            String customFont = a.getString(R.styleable.BBBTextView_fontName);

            if (!TextUtils.isEmpty(customFont)) {
                mFont = TypefaceCache.getInstance().getTypeface(context, customFont);
                setTypeface(mFont);
            }

            customFont = a.getString(R.styleable.BBBTextView_fontNameSelected);
            if (!TextUtils.isEmpty(customFont)) {
                mFontSelected = TypefaceCache.getInstance().getTypeface(context, customFont);
            }

            setIsLink(a.getBoolean(R.styleable.BBBTextView_isLink,false));

        } finally {
            a.recycle();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected && mFontSelected != null) {
            setTypeface(mFontSelected);
        } else if (mFont != null) {
            setTypeface(mFont);
        }
    }

    public void setIsLink(boolean isLink) {
        if (isLink != mIsLink) {
            mIsLink = isLink;
            final int paintFlags = super.getPaintFlags();
            if (mIsLink) {
                setPaintFlags(paintFlags | Paint.UNDERLINE_TEXT_FLAG);
            } else {
                setPaintFlags(paintFlags);
            }
            invalidate();
        }
    }

    public boolean getIsLink() {
        return mIsLink;
    }
}