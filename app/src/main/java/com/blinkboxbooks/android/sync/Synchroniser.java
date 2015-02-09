// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.text.TextUtils;
import android.widget.Toast;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookInfo;
import com.blinkboxbooks.android.api.model.BBBBookmark;
import com.blinkboxbooks.android.api.model.BBBBookmarkList;
import com.blinkboxbooks.android.api.model.BBBLibraryChanges;
import com.blinkboxbooks.android.api.model.BBBLibraryItem;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.BookDownloadController;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.Library;
import com.blinkboxbooks.android.model.LibraryItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.net.ApiConnector;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.provider.BBBContract.SyncState;
import com.blinkboxbooks.android.provider.BBBProviderHelper;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBTextUtils;
import com.blinkboxbooks.android.util.DeviceUtils;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.PreferenceManager;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/**
 * Class which handles all synchronization
 */
public class Synchroniser {

    public static final String TAG = Synchroniser.class.getSimpleName();

    public static final String ACTION_SYNC_STARTED = "sync_started";
    public static final String ACTION_SYNC_STOPPED = "sync_stopped";

    public static final String SYNC_SELECTIVE = "sync_selective";
    public static final String SYNC_ACCOUNT = "sync_account";
    public static final String SYNC_LIBRARY = "sync_library";
    public static final String SYNC_BOOKMARKS = "sync_bookmarks";
    private static final long ONE_MINUTE_IN_MILLISECONDS = 60000;

    private final ApiConnector apiConnector;

    private AccountManager mAccountManager;
    private Account mAccount;
    private String mAccessToken;
    private String mAuthority;
    private Context mContext;
    private long mLibraryId;
    private String mUserId;

    private Library mLibrary;

    private SyncResult mSyncResult;

    public Synchroniser() {
        apiConnector = ApiConnector.getInstance();
    }

    /**
     * Helper method for quickly retrieving library changes for the current user account on the current thread
     *
     * @param context
     * @param isbn    the ISBN of a book that will auto downloaded after the sync
     */
    public static void performLibrarySyncForCurrentAccount(Context context, String isbn) {

        if (context == null) {
            return;
        }

        Account account = AccountController.getInstance().getLoggedInAccount();

        Bundle extras = new Bundle();
        extras.putBoolean(SYNC_SELECTIVE, true);
        extras.putBoolean(SYNC_LIBRARY, true);
        extras.putString(BBBApiConstants.PARAM_BOOK, isbn);

        context.getContentResolver().requestSync(account, BBBContract.CONTENT_AUTHORITY, extras);
    }

    /**
     * Perform a sync for this account. SyncAdapter-specific parameters may be specified in extras, which is guaranteed to not be null. Invocations of this method are guaranteed to be serialized.
     *
     * @param account    the account that should be synced
     * @param extras     SyncAdapter-specific parameters
     * @param authority  the authority of this sync request
     * @param syncResult SyncAdapter-specific parameters
     */
    public void performSync(Context context, Account account, Bundle extras, String authority, SyncResult syncResult) {
        BBBRequestManager.getInstance().setInterface(AccountController.getInstance());

        mContext = context;
        mAccount = account;
        mAccountManager = AccountManager.get(mContext);
        mSyncResult = syncResult;
        mAuthority = authority;

        if (mAccount == null) {
            LogUtils.e(TAG, "Skipping sync for null account");
            return;
        }

        mUserId = mAccountManager.getUserData(mAccount, BBBApiConstants.PARAM_USER_ID);

        if (mUserId == null || !mUserId.equals(AccountController.getInstance().getUserId())) {
            LogUtils.e(TAG, "Skipping sync of logged out user " + mUserId);
            return;
        }

        mAccessToken = AccountController.getInstance().getAccessToken(mAccount);

        if (mAccessToken == null || mAccessToken.trim().length() == 0) {
            LogUtils.e(TAG, "Sync failed. Could not get access token");

            if (mSyncResult != null) {
                mSyncResult.stats.numAuthExceptions = 1;
            }

            return;
        }

        String library_id = mAccountManager.getUserData(mAccount, AccountController.PARAM_LIBRARY_ID);

        if (TextUtils.isEmpty(library_id)) {
            LogUtils.e(TAG, "Could not find library id for account: " + mUserId);
            return;
        } else {
            mLibraryId = Long.parseLong(library_id);
        }

        LogUtils.i(TAG, String.format("library: '%s' account:'%s'", mLibraryId, mUserId));

        mLibrary = LibraryHelper.getLibraryForUserId(mUserId);

        if (mLibrary == null) {
            LogUtils.e(TAG, "Could not load library data for account: " + mUserId);
            return;
        }

        if (extras != null && extras.getBoolean(SYNC_SELECTIVE, false)) {
            LogUtils.d(TAG, String.format("Performing selective sync"));

            if (extras.getBoolean(SYNC_ACCOUNT, false)) {
                registerClubcard();
            }

            if (extras.getBoolean(SYNC_LIBRARY, false)) {

                if (pullLibraryChanges(mLibrary.date_library_last_sync)) {
                    checkForBookToDownload(extras);
                    pushLibraryChanges();
                }

                mContext.getContentResolver().notifyChange(Books.ACCOUNT_URI, null, false);
            }

            if (extras.getBoolean(SYNC_BOOKMARKS, false)) {

                if (pullBookmarkChanges(mLibrary.date_bookmark_last_sync)) {
                    pushBookmarkChanges();
                }
            }
        } else {
            LogUtils.d(TAG, String.format("Performing full sync"));

            registerClubcard();

            if (pullLibraryChanges(mLibrary.date_library_last_sync)) {
                checkForBookToDownload(extras);
                pushLibraryChanges();

                //sync bookmarks only if library sync is successful
                if (pullBookmarkChanges(mLibrary.date_bookmark_last_sync)) {
                    pushBookmarkChanges();
                }

                mContext.getContentResolver().notifyChange(Books.ACCOUNT_URI, null, false);
            } else {
                new ToastMessageLooper(R.string.error_library_sync_failure).start();
            }
        }
    }

    private void checkForBookToDownload(Bundle extras) {

        String isbn = extras.getString(BBBApiConstants.PARAM_BOOK, null);

        if (!TextUtils.isEmpty(isbn)) {
            LogUtils.i(TAG, "Book to download after sync: " + isbn);

            final Book book = BookHelper.getBookFromISBN(mUserId, isbn, false);

            if (book != null) {
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        BookDownloadController.getInstance(mContext).startDownloadBook(book);
                    }
                });
            }
        }
    }

    /*
     * Registers the users clubcard if there is one pending upload
     */
    private void registerClubcard() {
        String clubcard = mAccountManager.getUserData(mAccount, BBBApiConstants.PARAM_CLUBCARD);

        if (!TextUtils.isEmpty(clubcard)) {
            LogUtils.i(TAG, "registering clubcard number: " + clubcard);

            boolean success = apiConnector.uploadClubcardNumber(clubcard);

            if (success) {
                mAccountManager.setUserData(mAccount, BBBApiConstants.PARAM_CLUBCARD, null);
                LogUtils.i(TAG, "successfully uploaded clubcard number");
            } else {
                LogUtils.e(TAG, "failed to upload clubcard number");
            }
        }
    }

    /*
     * Scans the library for changes that need to be pushed to the server
     */
    private void pushLibraryChanges() {
        LogUtils.i(TAG, "pushing library changes");

        Uri uri = BBBContract.Books.buildBookSynchronizationUri(mUserId);

        Cursor cursor = null;
        boolean operationSuccess = true;

        try {
            cursor = mContext.getContentResolver().query(uri, null, null, null, null);
            Book book;
            boolean itemSuccess;

            Uri bookUri;

            int deleteCountSuccess = 0, deleteCountFail = 0;
            int updateCountSuccess = 0, updateCountFail = 0;

            while (cursor.moveToNext()) {
                itemSuccess = false;

                book = BookHelper.createBook(cursor);

                bookUri = BBBContract.Books.buildBookServerIdUri(mUserId, book.server_id);

                if (book.sync_state == BBBContract.SyncState.STATE_DELETED) {
                    itemSuccess = apiConnector.removeBook(book);

                    if (itemSuccess) {
                        deleteCountSuccess++;

                        mContext.getContentResolver().delete(bookUri, null, null);
                    } else {
                        deleteCountFail++;
                    }

                    operationSuccess &= itemSuccess;
                } else if (book.sync_state == BBBContract.SyncState.STATE_UPDATED) {
                    LogUtils.i(TAG, "update book reading status:" + book.title + " isbn:" + book.isbn);
                    itemSuccess = apiConnector.updateReadingStatus(book);

                    if (itemSuccess) {
                        updateCountSuccess++;

                        ContentValues values = new ContentValues();
                        values.put(Books.BOOK_SYNC_STATE, BBBContract.SyncState.STATE_NORMAL);
                        values.put(Books.BOOK_UPDATE_DATE, System.currentTimeMillis());
                        mContext.getContentResolver().update(bookUri, values, null, null);
                    } else {
                        updateCountFail++;
                    }

                    operationSuccess &= itemSuccess;
                }
            }

            LogUtils.i(TAG, String.format("Push library changes (success/fail) deletions(%d/%d) updates(%d/%d)",
                    deleteCountSuccess, deleteCountFail, updateCountSuccess, updateCountFail));
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
            operationSuccess = false;
        } finally {
            cursor.close();
        }

        if (operationSuccess) {
            LogUtils.i(TAG, "successfully pushed library changes");
        } else {
            LogUtils.e(TAG, "failed to push library changes");
        }
    }

    /**
     * Rewind the given server time string by the given amount - but no earlier than epoch time (1/1/70 00:00:00).
     *
     * This is used as a sticking plaster to fix a potential server issue where a sync time reported
     * by the server is not in sync with the database time - which means we could miss a bought book,
     * or new bookmark (See ALA-1933).
     *
     *
     * @param utcTime              a string in the format of yyyy-MM-ddTHH:mm:ssZ, or null.
     * @param millisecondsToRewind the number of milliseconds to subtract from this datetime
     * @return a string in the format yyyy-MM-ddTHH:mm:ssZ that is millisecondsToRewind earlier than the given date, or null if the date was null.
     * @throws ParseException
     */
    private static String rewindServerTime(String utcTime, long millisecondsToRewind) throws ParseException {
        if (utcTime == null) {
            return null;
        }
        final SimpleDateFormat simpleDateFormat;

        if (utcTime.indexOf('.') >= 0) {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.S'Z'",Locale.US);
        } else {
            simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        }

        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date date = simpleDateFormat.parse(utcTime);
        final long dateTimeInMilliseconds = Math.max(date.getTime()- millisecondsToRewind,0L);
        return simpleDateFormat.format(new Date(dateTimeInMilliseconds));
    }

    /*
     * Requests the library changes from the server based on last sync time and apply them in a batch.
     */
    private boolean pullLibraryChanges(String lastSyncTime) {
        LogUtils.i(TAG, "pulling library changes with last sync time: " + lastSyncTime);
        try {
            lastSyncTime = rewindServerTime(lastSyncTime, ONE_MINUTE_IN_MILLISECONDS);
        } catch (ParseException e) {
            Crashlytics.log("error rewinding sync time: " + lastSyncTime);
            Crashlytics.logException(e);
            e.printStackTrace();
        }

        LogUtils.i(TAG, "pulling library changes with new last sync time: " + lastSyncTime);

        BBBRequest request = BBBRequestFactory.getInstance().createGetAllLibraryItemsRequest(lastSyncTime);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
            final String message = "failed to pull library changes";
            LogUtils.e(TAG, message);

            // ERROR CONNECTION FAILED MESSAGES are expected to happen normally so don't log this message
            if (response.getResponseCode() != BBBApiConstants.ERROR_CONNECTION_FAILED) {
                Crashlytics.log("Response code: "+response.getResponseCode());
                Crashlytics.logException(new RuntimeException(message));
            }
            return false;
        }

        final BBBLibraryChanges libraryChanges;

        try {
            libraryChanges = new Gson().fromJson(response.getResponseData(), BBBLibraryChanges.class);
        } catch (Exception e) {
            final String message = "Could not parse library changes object";
            LogUtils.e(TAG, message);
            Crashlytics.log("Response data:"+response.getResponseData());
            Crashlytics.logException(new RuntimeException(message));
            return false;
        }

        if (libraryChanges != null && libraryChanges.libraryChangesList != null) {

            BBBLibraryItem[] items = libraryChanges.libraryChangesList.items;

            if (items == null || items.length == 0) {
                LogUtils.i(TAG, "No library changes");
                return true;
            }

            LibraryItem[] libraryItems = getBookInfoForLibraryItems(items);

            if (libraryItems == null) {
                final String message = "failed to pull library changes. could not get book info.";
                LogUtils.e(TAG, message);
                Crashlytics.log("Response data:"+response.getResponseData());
                Crashlytics.logException(new RuntimeException(message));
                return false;
            }

            long syncTimestamp = BBBCalendarUtil.parseDate(libraryChanges.lastSyncDateTime, BBBCalendarUtil.FORMAT_TIME_STAMP).getTimeInMillis();
            ArrayList<ContentProviderOperation> operations = BBBProviderHelper.convertBookInfoToContentValues(mUserId, mLibraryId, libraryItems, syncTimestamp);

            try {
                mContext.getContentResolver().applyBatch(mAuthority, operations);

                LogUtils.i(TAG, String.format("Successfully pulled and updated %d records", items.length));

                if (libraryChanges.lastSyncDateTime != null) {
                    mLibrary.date_library_last_sync = libraryChanges.lastSyncDateTime;
                    LibraryHelper.updateLibrary(mLibrary);
                }

                return true;
            } catch (RemoteException e) {
                LogUtils.e(TAG, e.getMessage(), e);
                Crashlytics.logException(e);
            } catch (OperationApplicationException e) {
                LogUtils.e(TAG, e.getMessage(), e);
                Crashlytics.logException(e);
            } catch(Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
                Crashlytics.logException(e);
            }
        }

        LogUtils.e(TAG, "failed to pull library changes");
        return false;
    }

    /*
     * Requests the bookmark changes from the server based on last sync time and apply them in a batch
     */
    private boolean pullBookmarkChanges(String lastSyncTime) {
        LogUtils.i(TAG, "pulling bookmarks changes with last sync time: " + lastSyncTime);

        BBBRequest request = BBBRequestFactory.getInstance().createGetBookmarksRequest(null, null, lastSyncTime);
        BBBResponse response = BBBRequestManager.getInstance().executeRequestSynchronously(request);

        if (response.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LogUtils.e(TAG, "failed to pull bookmark changes");
            return false;
        }

        BBBBookmarkList bookmarkList = null;

        try {
            bookmarkList = new Gson().fromJson(response.getResponseData(), BBBBookmarkList.class);
        } catch (Exception e) {
            LogUtils.e(TAG, "could not parse bookmark changes object");
            return false;
        }
        ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
        Set<Long> changedBookIds = new HashSet<>();

        if (bookmarkList != null) {
            int updates = 0, deletes = 0;
            if (bookmarkList.bookmarks != null) {
                for (BBBBookmark bbbBookmark : bookmarkList.bookmarks) {

                    if (bbbBookmark.deleted) {
                        deletes++;
                        Uri uri = Bookmarks.buildBookmarkCloudIdUri(mUserId, bbbBookmark.id);
                        ContentProviderOperation.Builder builder = ContentProviderOperation.newDelete(uri);
                        operations.add(builder.build());
                        changedBookIds.add(BookmarkHelper.getBookIdFromBookmarkCloudUri(uri));
                    } else {
                        Book book = BookHelper.getBookFromISBN(mUserId, bbbBookmark.book, null);
                        if (book == null) {
                            LogUtils.w(TAG, "No book for bookmark with ISBN " + bbbBookmark.book);
                            continue;
                        }

                        int type = BookmarkHelper.getBookmarkType(bbbBookmark.bookmarkType);
                        if (type == Bookmarks.TYPE_LAST_POSITION) {
                            // Users with last positions don't need to see the reader help overlay
                            PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_SHOWN_READER_HELP, true);

                            String clientId = BBBTextUtils.getIdFromGuid(AccountController.getInstance().getClientId());
                            String pushedBy = bbbBookmark.updatedByClient == null ? bbbBookmark.createdByClient : bbbBookmark.updatedByClient;

                            if (pushedBy == null || pushedBy.equals(clientId)) {
                                LogUtils.d(TAG, "Ignoring locally pushed change");
                                continue;
                            }

                            Bookmark lastPosition = BookmarkHelper.getBookmark(Bookmarks.TYPE_LAST_POSITION, book.id);


                            // Conflict detected, wait until the book is opened and prompt user to resolve
                            if (lastPosition != null && (lastPosition.update_by == null || lastPosition.update_by.equals(clientId))) {
                                continue;
                            }
                        }
                        Bookmark bookmark = BookmarkHelper.createBookmark(bbbBookmark);

                        updates++;

                        bookmark.book_id = book.id;

                        Uri uri = Bookmarks.buildBookmarkCloudIdUri(mUserId, bookmark.cloud_id);
                        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(uri);
                        ContentValues contentValues = BookmarkHelper.getContentValues(bookmark);
                        // Don't clear the sync state
                        contentValues.remove(Bookmarks.BOOKMARK_STATE);
                        builder.withValues(contentValues);
                        operations.add(builder.build());

                        changedBookIds.add(book.id);
                    }
                }
            }

            try {
                mContext.getContentResolver().applyBatch(mAuthority, operations);

                LogUtils.i(TAG, String.format("Successfully pulled %d updated and %d deleted bookmarks with %d operations", updates, deletes, operations.size()));
                mLibrary.date_bookmark_last_sync = bookmarkList.lastSyncDateTime;
                LibraryHelper.updateLibrary(mLibrary);

                for (long bookId : changedBookIds) {
                    final Uri bookUri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, bookId);
                    //Notify any content observers that a bookmark change has occurred for the book
                    mContext.getContentResolver().notifyChange(bookUri, null, false);
                }

                return true;
            } catch (RemoteException e) {
                LogUtils.e(TAG, e.getMessage(), e);
            } catch (OperationApplicationException e) {
                LogUtils.e(TAG, e.getMessage(), e);
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }

        LogUtils.e(TAG, "failed to pull bookmark changes");
        return false;
    }

    /*
     * Scans the library for changes that need to be pushed to the server
     */
    private void pushBookmarkChanges() {
        LogUtils.i(TAG, "pushing bookmark changes");

        Uri uri = Bookmarks.buildBookmarkSynchronizationUri(mUserId);

        Cursor cursor = null;
        boolean operationSuccess = true;

        try {
            cursor = mContext.getContentResolver().query(uri, null, null, null, null);

            int addCountSuccess = 0, addCountFail = 0;
            int deleteCountSuccess = 0, deleteCountFail = 0;
            int updateCountSuccess = 0, updateCountFail = 0;

            while (cursor.moveToNext()) {
                boolean itemSuccess;
                Bookmark bookmark = BookmarkHelper.createBookmark(cursor);
                Uri bookmarkUri = Bookmarks.buildBookmarkCloudIdUri(mUserId, bookmark.cloud_id);

                if (bookmark.cloud_id == null || bookmark.state == SyncState.STATE_ADDED) {
                    itemSuccess = apiConnector.addBookmark(bookmark);

                    if (itemSuccess) {
                        long bookmarkId = cursor.getLong(0);
                        bookmarkUri = Bookmarks.buildBookmarkIdUri(bookmarkId);
                        ContentValues values = BookmarkHelper.getContentValues(bookmark);
                        int rows = mContext.getContentResolver().update(bookmarkUri, values, null, null);
                        if (rows == 1) {
                            addCountSuccess++;
                        }
                    } else {
                        addCountFail++;
                    }

                    operationSuccess &= itemSuccess;
                } else if (bookmark.state == BBBContract.SyncState.STATE_DELETED) {
                    itemSuccess = apiConnector.removeBookmark(bookmark, DeviceUtils.getClientName(mContext));

                    if (itemSuccess) {
                        deleteCountSuccess++;
                        mContext.getContentResolver().delete(bookmarkUri, null, null);
                    } else {
                        deleteCountFail++;
                    }

                    operationSuccess &= itemSuccess;
                } else if (bookmark.state == BBBContract.SyncState.STATE_UPDATED) {
                    itemSuccess = apiConnector.updateBookmark(bookmark);

                    if (itemSuccess) {
                        updateCountSuccess++;
                        ContentValues values = BookmarkHelper.getContentValues(bookmark);
                        values.put(Bookmarks.BOOKMARK_STATE, SyncState.STATE_NORMAL);
                        mContext.getContentResolver().update(bookmarkUri, values, null, null);
                    } else {
                        updateCountFail++;
                    }

                    operationSuccess &= itemSuccess;
                }
            }

            LogUtils.i(TAG, String.format("Push bookmark changes (success/fail) additions(%d/%d) deletions(%d/%d) updates(%d/%d)",
                    addCountSuccess, addCountFail, deleteCountSuccess, deleteCountFail, updateCountSuccess, updateCountFail));
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
            operationSuccess = false;
        } finally {
            cursor.close();
        }

        if (operationSuccess) {
            LogUtils.i(TAG, "successfully pushed bookmark changes");
        } else {
            LogUtils.e(TAG, "failed to push bookmark changes");
        }
    }

    private LibraryItem[] getBookInfoForLibraryItems(BBBLibraryItem[] items) {

        if (items == null) {
            return null;
        }

        LibraryItem[] libraryItems = new LibraryItem[items.length];

        String[] ids = new String[items.length];

        for (int i = 0; i < items.length; i++) {
            ids[i] = items[i].isbn;
        }

        ArrayList<BBBBookInfo> bookInfoList = ApiConnector.getInstance().getBookInfo(ids);

        if (bookInfoList == null || bookInfoList.size() != items.length) {
            return null;
        }

        BBBBookInfo bookInfo;

        for (int i = 0; i < items.length; i++) {
            libraryItems[i] = new LibraryItem();
            libraryItems[i].libraryItem = items[i];

            for (int j = 0; j < items.length; j++) {
                bookInfo = bookInfoList.get(j);

                if (items[i].isbn.equals(bookInfo.id)) {
                    libraryItems[i].bookInfo = bookInfo;
                    break;
                }
            }

            //fail if we could not retrieve book information. this will ensure the last sync date does not get updated and we can try again next time
            if (libraryItems[i].bookInfo == null) {
                return null;
            }
        }

        return libraryItems;
    }

    /* Allows us to show a toast message from a worker thread */
    private class ToastMessageLooper extends Thread {

        private int messageResourceId;

        public ToastMessageLooper(int messageResourceId) {
            this.messageResourceId = messageResourceId;
        }

        public void run() {
            Looper.prepare();

            new Handler().post(new Runnable() {
                public void run() {
                    Toast.makeText(mContext, messageResourceId, Toast.LENGTH_SHORT).show();
                }
            });

            Looper.loop();
        }
    }
}