// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.os.AsyncTask;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Request.Priority;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.blinkbox.java.book.factory.BBBEPubFactory;
import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.controller.LibraryController;
import com.crashlytics.android.Crashlytics;

public class BBBCustomImageLoader extends ImageLoader {
    private static final String TAG = BBBCustomImageLoader.class.getSimpleName();

    private static final boolean FORCE_TO_JPEG = true; //forces the requested images to jpeg
    private static final int TIMEOUT = 1000 * 30;
    private static final int NUMBER_RETRIES = 2;
    private static final float RETRY_BACKOFF = 1.5f;

    private ImageCache mCache;

    private final DefaultRetryPolicy mRetryPolicy;

    public BBBCustomImageLoader(RequestQueue queue, ImageCache imageCache) {
        super(queue, imageCache);

        mCache = imageCache;

        mRetryPolicy = new DefaultRetryPolicy(TIMEOUT, NUMBER_RETRIES, RETRY_BACKOFF);
    }

    public void cancelRequest(String requestUrl) {
        mRequestQueue.cancelAll(requestUrl);
        mInFlightRequests.remove(getCacheKey(requestUrl));
    }

    /**
     * Asynchronous version of the get method.
     *
     * @param requestUrl    The url of the remote image
     * @param imageListener The listener to call when the remote image is loaded
     */
    public void getAsync(String requestUrl, ImageListener imageListener, Priority priority) {
        String cacheKey = getCacheKey(requestUrl);

        Bitmap bitmap = mCache.getBitmap(cacheKey);

        if (bitmap != null) {
            ImageContainer container = new ImageContainer(bitmap, requestUrl, null, null);
            imageListener.onResponse(container, true);
        } else {
            ImageRequestData requestData = new ImageRequestData(requestUrl, imageListener, priority, cacheKey);
            new ImageRequestTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, requestData);
        }
    }

    /**
     * Checks if the item is available in the cache.
     *
     * @param requestUrl The url of the remote image
     * @return True if the item exists in cache, false otherwise.
     */
    public boolean isCached(String requestUrl) {
        String cacheKey = getCacheKey(requestUrl);
        return mCache.getBitmap(cacheKey) != null;
    }

    /**
     * Retrieve the url directly from the cache
     *
     * @param requestUrl The url of the remote image
     * @return the cached bitmap, or null if does not exist in the cache
     */
    public Bitmap getCachedBitmap(String requestUrl) {
        String cacheKey = getCacheKey(requestUrl);
        return mCache.getBitmap(cacheKey);
    }

    private class ImageRequestData {

        private final String url;
        private final ImageListener imageListener;
        private final Priority priority;
        private final String cacheKey;

        public ImageRequestData(String url, ImageListener imageListener, Priority priority, String cacheKey) {
            this.url = url;
            this.imageListener = imageListener;
            this.priority = priority;
            this.cacheKey = cacheKey;
        }
    }

    private class ImageRequestTask extends AsyncTask<ImageRequestData, Void, Void> {

        private ImageRequestData requestData;
        private Bitmap cachedBitmap;

        protected Void doInBackground(ImageRequestData... params) {

            if (params.length == 0 || params[0] == null) {
                return null;
            }

            requestData = params[0];

            if (BuildConfig.DEBUG) {
                VolleyLog.d("%s %s %s %s", requestData.url, requestData.cacheKey, requestData.priority, "" + requestData.imageListener);
            }

            // Try to look up the request in the cache of remote images.
            try {
                cachedBitmap = mCache.getBitmap(requestData.cacheKey);
            } catch (OutOfMemoryError e) {
                LogUtils.e(TAG, "Out Of Memory loading cached bitmap: " + requestData.url, e);
            }

            if (cachedBitmap != null) {
                return null;
            }

            if (requestData.url != null && requestData.url.startsWith("/")) {
                cachedBitmap = BBBEPubFactory.getInstance().loadBitmapFromBook(BBBApplication.getApplication(), requestData.url, LibraryController.bookCoverWidth, LibraryController.bookCoverWidth, null);
                if (cachedBitmap == null) {
                    return null;
                }
                mCache.putBitmap(requestData.cacheKey, cachedBitmap);
                if (cachedBitmap != null) {
                    return null;
                }
            }

            ImageContainer imageContainer = new ImageContainer(null, requestData.url, requestData.cacheKey, requestData.imageListener);

            // Check to see if a request is already in-flight.
            BatchedImageRequest request = mInFlightRequests.get(requestData.cacheKey);
            if (request != null) {
                // If it is, add this request to the list of listeners.
                request.addContainer(imageContainer);

                // Just return here as we don't want to set off another request when one is in progress
                return null;
            }

            if (requestData.url != null) {
                try {
                    // The request is not already in flight. Send the new request to the network and track it.
                    //TODO: Remove the http -> https rewrite, this is a hack to force https calls (See ALA-1437)
                    final String url = requestData.url.replace("http://","https://") + (FORCE_TO_JPEG ? ".jpg" : "");
                    Request<?> newRequest = new ImageRequest(url, new Listener<Bitmap>() {

                        public void onResponse(Bitmap response) {
                            onGetImageSuccess(requestData.cacheKey, response);
                        }
                    }, 0, 0, Config.RGB_565, new ErrorListener() {

                        public void onErrorResponse(VolleyError error) {
                            onGetImageError(requestData.cacheKey, error);
                        }
                    }
                    );

                    newRequest.setRetryPolicy(mRetryPolicy);
                    newRequest.setPriority(requestData.priority);
                    newRequest.setTag(requestData.url);

                    mRequestQueue.add(newRequest);
                    mInFlightRequests.put(requestData.cacheKey, new BatchedImageRequest(newRequest, imageContainer));
                } catch (NullPointerException e) {
                    Crashlytics.log("requestData: " + requestData);
                    if (requestData != null) {
                        Crashlytics.log("requestData.url: "+ requestData.url);
                        Crashlytics.log("requestData.imageListener: " + requestData.imageListener);
                    }
                    Crashlytics.logException(e);
                }
            }


            return null;
        }

        protected void onPostExecute(Void result) {

            if (cachedBitmap != null) {
                // Return the cached bitmap.
                ImageContainer container = new ImageContainer(cachedBitmap, requestData.url, null, null);
                requestData.imageListener.onResponse(container, true);
            }
        }
    }

    protected static String getCacheKey(String url) {
        return String.valueOf(url.hashCode());
    }
}
