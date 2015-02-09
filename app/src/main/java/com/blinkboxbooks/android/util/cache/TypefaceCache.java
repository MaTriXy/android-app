// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util.cache;

import android.content.Context;
import android.graphics.Typeface;

import java.util.HashMap;

/**
 * Class for caching Typeface objects.
 */
public class TypefaceCache {

    private static final TypefaceCache instance = new TypefaceCache();

    public static TypefaceCache getInstance() {
        return instance;
    }

    private final HashMap<String, Typeface> mTypefaceCache;

    private TypefaceCache() {
        mTypefaceCache = new HashMap<String, Typeface>();
    }

    public Typeface getTypeface(Context context, int fontFile) {
        String fontString = context.getString(fontFile);
        return getTypeface(context, fontString);
    }

    /**
     * Attempts to retrieve the Typeface from the cache. If the Typeface does not exist in the cache it is read for the assets, stored in the cache, and returned
     *
     * @param context  the Context to use
     * @param fontFile the font filename as stored in the assets folder
     * @return the Typeface object
     */
    public Typeface getTypeface(Context context, String fontFile) {
        Typeface typeface = mTypefaceCache.get(fontFile);

        if (typeface == null) {
            typeface = Typeface.createFromAsset(context.getAssets(), fontFile);
            mTypefaceCache.put(fontFile, typeface);
        }

        return typeface;
    }
}