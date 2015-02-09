// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util.cache;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

/**
 * Memory only image cache with a size limit. When this cache becomes full, the least recently used bitmap is removed.
 */
public class MemoryImageCache implements ImageCache {

    private final LruCache<String, Bitmap> mLruCache;

    /**
     * Initialize this memory cache with the given size in bytes
     *
     * @param cacheSize the size of this cache in bytes
     */
    public MemoryImageCache(int cacheSize) {

        mLruCache = new LruCache<String, Bitmap>(cacheSize) {

            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    /**
     * Gets a Bitmap from the cache.
     *
     * @param id the ID of the bitmap to retrieve.
     * @return the Bitmap object or null if it does not exist in this cache
     */
    @Override
    public Bitmap getBitmap(String id) {
        synchronized (mLruCache) {
            return mLruCache.get(id);
        }
    }

    /**
     * Puts a Bitmap into the cache.
     *
     * @param id     the ID of the bitmap to insert
     * @param bitmap the Bitmap object you want to cache
     */
    @Override
    public void putBitmap(String id, Bitmap bitmap) {
        synchronized (mLruCache) {
            mLruCache.put(id, bitmap);
        }
    }
}