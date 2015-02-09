// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import com.blinkbox.java.book.crc.CRCHandler;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.util.PreferenceManager;
import com.crashlytics.android.Crashlytics;

import java.util.HashSet;
import java.util.Set;

/**
 * Internal implementation of the BBBEPubLibrary CRCError Handler. We associate a book with the error handler so we can
 * report the correct failure.
 */
public class CRCErrorHandler implements CRCHandler.CRCErrorHandler {

    private String mBookISBN;
    private BaseActivity mParentActivity;

    /**
     * Create a new CRCErrorHandler.
     * @param activity the parent activity
     */
    public CRCErrorHandler(BaseActivity activity) {
        mParentActivity = activity;
    }

    /**
     * Set the book ISBN that is associated with this Error Handler
     * @param isbn the book ISBN to set
     */
    public void setBookISBN(String isbn) {
        mBookISBN = isbn;
    }

    @Override
    public void onCRCError() {

        // Note that we must be careful using the activity as this callback could occur after the activity has gone away
        if (mParentActivity != null && !mParentActivity.isDestroyedOrFinishing()) {

            // We want to display a message to warn the user that this book is dodgy, but we only display the warning once as they may be able to read
            // the book without (noticeable) problems and wont want to see the warning over and over
            Set<String> warningsReportedSet = PreferenceManager.getInstance().getPreference(PreferenceManager.PREF_KEY_CRC_ERRORS_SHOWN, new HashSet<String>());

            if (mBookISBN != null && !warningsReportedSet.contains(mBookISBN)) {
                mParentActivity.showMessage(mParentActivity.getString(R.string.crc_error_title), mParentActivity.getString(R.string.crc_error_warning));
                warningsReportedSet.add(mBookISBN);
                PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_CRC_ERRORS_SHOWN, warningsReportedSet);
            }
        }

        // Log the error to bug sense so we are notified that the book has a CRC error
        if (mBookISBN != null) {
            String error = String.format("CRC Error: ISBN=" + mBookISBN);
            Crashlytics.logException(new Exception(error));
        }
    }
}