// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.AutoCompleteTextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.cache.TypefaceCache;

/**
 * Custom AutoCompleteTextView class which simplifies setting a custom font. Font is set via custom view attributes
 */
public class BBBAutoCompleteTextView extends AutoCompleteTextView {
    private static final int[] STATE_ERROR = { R.attr.state_error };

    private boolean mIsError = false;

    public BBBAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (!isInEditMode()) {
            setCustomAttributes(context, attrs);
        }
    }

    public BBBAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            setCustomAttributes(context, attrs);
        }
    }

    public BBBAutoCompleteTextView(Context context) {
        super(context);
    }

    /*
     * Sets the custom font
     */
    private void setCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.CustomFont, 0, 0);

        try {
            String customFont = a.getString(R.styleable.CustomFont_fontName);

            if (!TextUtils.isEmpty(customFont)) {
                Typeface typeFace = TypefaceCache.getInstance().getTypeface(context, customFont);
                setTypeface(typeFace);
            }
        } finally {
            a.recycle();
        }
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mIsError) {
            mergeDrawableStates(drawableState, STATE_ERROR);
        }
        return drawableState;
    }

    public void setErrorState(boolean isError) {
        if (isError != mIsError) {
            mIsError = isError;
            refreshDrawableState();
        }
    }
}
