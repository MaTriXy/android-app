// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.loader;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.CursorLoader;

import com.blinkboxbooks.android.net.exceptions.APIConnectorException;
import com.blinkboxbooks.android.net.exceptions.APIConnectorRuntimeException;
import com.blinkboxbooks.android.net.exceptions.InternalServerErrorException;
import com.blinkboxbooks.android.net.exceptions.NoNetworkException;
import com.blinkboxbooks.android.net.exceptions.NoResultsException;
import com.blinkboxbooks.android.ui.shop.SortOption;

/**
 * Loads items from the catalogue REST API
 */
public class CatalogueLoader extends CursorLoader {

    private ErrorListener mErrorListener;

    /**
     * Load the catalogue content with the supplied URI
     *
     * @param context
     * @param uri
     * @param projection not used at the moment
     * @param selection not used at the moment
     * @param selectionArgs not used at the moment
     * @param sortOption this is not the standard content provider sort option. We need to pass two sorting values to the ContentProvider so we use a colon
     *                   ':' seperated parameter. The first element is the 'order' to sort by (BBBApiConstants.ORDER_*). The second parameter is
     *                   either 'true' or 'false' to specify a descending or ascending order respectively.
     *
     *                   example:
     *                   String sortOption = BBBApiConstants.ORDER_SEQUENTIAL+':'+false;
     *
     *                   If the sortOption does not contain a ':' then the ContentProvider will treat the entire value as the 'order' (BBBApiConstants.ORDER_*)
     */
    public CatalogueLoader(Context context, Uri uri, String[] projection, String selection, String[] selectionArgs, SortOption sortOption) {
        super(context, uri, projection, selection, selectionArgs, sortOption == null ? null : sortOption.sortParameter+":"+sortOption.desc);
    }

    /**
     * Sets the ErrorListener for listening for network/server errors
     *
     * @param errorListener
     */
    public void setErrorListener(ErrorListener errorListener) {
        mErrorListener = errorListener;
    }

    @Override
    public Cursor loadInBackground() {
        try {
            return super.loadInBackground();
        } catch (Throwable throwable) {

            if(mErrorListener != null) {

                // We expect that any exception thrown by the query will be an APIConnectorRuntimeException
                if(throwable instanceof APIConnectorRuntimeException) {

                    // Get the wrapped APIConnectorException so we can handle the failure appropriately
                    APIConnectorException wrappedException = ((APIConnectorRuntimeException) throwable).getAPIConnectorException();

                    if (wrappedException instanceof InternalServerErrorException) {
                        mErrorListener.internalServerError();
                    } else if (wrappedException instanceof NoNetworkException) {
                        mErrorListener.noNetworkError();
                    } else if (wrappedException instanceof NoResultsException) {
                        mErrorListener.noResults();
                    } else {
                        // As a fall back we just assume a network error for any other exception type
                        mErrorListener.noNetworkError();
                    }
                } else {
                    // As a fall back we just assume a network error for any other exception type
                    mErrorListener.noNetworkError();
                }
            }
        }

        return null;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    public interface ErrorListener {
        public void internalServerError();
        public void noNetworkError();
        public void noResults();
    }
}
