// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.cache.TypefaceCache;

/**
 * Custom Switch class which simplifies setting a custom font. Font is set via custom view attributes
 */
public class BBBSwitch extends SwitchCompat {

    public BBBSwitch(Context context) {
        super(context);
    }

    public BBBSwitch(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        if (!isInEditMode()) {
            setCustomAttributes(context, attrs);
        }
    }

    public BBBSwitch(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isInEditMode()) {
            setCustomAttributes(context, attrs);
        }
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
}
