// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.text.TextUtils;

import com.android.volley.Network;
import com.android.volley.Request.Priority;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.blinkboxbooks.android.util.cache.MemoryImageCache;

import java.io.File;

/**
 * ImageLoader class for downloading and caching images
 */
public class BBBImageLoader {
    private static final int CACHE_FACTOR = 16;

    private static final BBBImageLoader instance = new BBBImageLoader();

    public static BBBImageLoader getInstance() {
        return instance;
    }

    private static final String IMAGE_PARAMS = "/params;img:q=85;img:w=%d;v=0";

    //Volley ImageLoader
    private BBBCustomImageLoader mImageLoader;

    private BBBImageLoader() {
    }

    public void setContext(Context context) {
        mImageLoader = new BBBCustomImageLoader(newRequestQueue(context, null), new MemoryImageCache((int) (Runtime.getRuntime().maxMemory() / CACHE_FACTOR)));
    }

    /**
     * Default on-disk cache directory.
     */
    private static final String DEFAULT_CACHE_DIR = "volley";
    private static final int CACHE_SIZE_DISK = 1024 * 1024 * 64; //64MB

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack   An {@link com.android.volley.toolbox.HttpStack} to use for the network, or null for default.
     * @return A started {@link RequestQueue} instance.
     */
    private static RequestQueue newRequestQueue(Context context, HttpStack stack) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);

        String userAgent = "volley/0";
        try {
            String packageName = context.getPackageName();
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            userAgent = packageName + "/" + info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
        }

        if (stack == null) {
            if (Build.VERSION.SDK_INT >= 9) {
                stack = new HurlStack();
            } else {
                // Prior to Gingerbread, HttpUrlConnection was unreliable.
                // See: http://android-developers.blogspot.com/2011/09/androids-http-clients.html
                stack = new HttpClientStack(AndroidHttpClient.newInstance(userAgent));
            }
        }

        Network network = new BasicNetwork(stack);

        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir, CACHE_SIZE_DISK), network);
        queue.start();

        return queue;
    }


    /**
     * Cancels a request if it is still in the pending queue.
     *
     * @param requestUrl
     */
    public void cancel(String requestUrl) {
        if (!TextUtils.isEmpty(requestUrl)) {
            mImageLoader.cancelRequest(requestUrl);
        }
    }

    /**
     * Injects a width parameter into an image url in accordance with the blink box image resource server
     *
     * @param url   the URL you want to modify
     * @param width the width you want to inject
     * @return the modified String
     */
    public static String injectWidthIntoCoverUrl(String url, int width) {

        if (width <= 0) {
            return null;
        } else {
            return StringUtils.injectIntoResourceUrl(url, String.format(IMAGE_PARAMS, width));
        }
    }

    /**
     * Attempts to retrieve the image at the url and passes it back via the ImageListener. First checks the cache and if it exists there returns it
     * immediately otherwise attempts to download it and return later.
     *
     * @param url      the url pointing to where this image is located online
     * @param listener the ImageListener via which the image will be returned.
     */
    public void get(String url, ImageListener listener, Priority priority) {

        if (!TextUtils.isEmpty(url)) {
            mImageLoader.getAsync(url, listener, priority);
        }
    }

    /**
     * Gets a Bitmap directly from the cache
     *
     * @param url the url pointing to where this image is located online
     * @return the Bitmap or null if the cache does not contain this bitmap
     */
    public Bitmap getCachedBitmap(String url) {
        return mImageLoader.getCachedBitmap(url);
    }

    /**
     * Checks whether the cache contains this particular image
     *
     * @param url the url pointing to where this image is located online
     * @return true if the cache contains this bitmap or false
     */
    public boolean isCached(String url) {
        return mImageLoader.isCached(url);
    }

    public static final ImageListener IGNORE_HANDLER = new ImageListener() {

        @Override
        public void onErrorResponse(VolleyError error) {
        }

        @Override
        public void onResponse(ImageContainer container, boolean immediate) {
        }
    };
}