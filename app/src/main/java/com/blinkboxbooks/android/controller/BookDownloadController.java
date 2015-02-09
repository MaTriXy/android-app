// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.controller;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DownloadManager;
import android.app.DownloadManager.Query;
import android.app.DownloadManager.Request;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.StatFs;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.LongSparseArray;
import android.text.TextUtils;
import android.util.Base64;
import android.widget.Toast;

import com.android.volley.Request.Priority;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBAuthenticationError;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.net.ApiConnector;
import com.blinkboxbooks.android.provider.BBBAsyncQueryHandler;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.BBBImageLoader;
import com.blinkboxbooks.android.util.DeviceUtils;
import com.blinkboxbooks.android.util.EncryptionUtil;
import com.blinkboxbooks.android.util.LogUtils;
import com.blinkboxbooks.android.util.NetworkUtils;
import com.blinkboxbooks.android.util.NotificationUtil;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.io.File;
import java.net.HttpURLConnection;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

/**
 * Controller for the BookListFragment
 */
public class BookDownloadController extends Handler {

    private static final String TAG = BookDownloadController.class.getSimpleName();
    public static final String ISBN = "isbn";

    public static final String BOOK_DOWNLOAD_PROGRESS_ACTION = "BOOK_DOWNLOAD_PROGRESS_ACTION";
    public static final String BOOK_DOWNLOAD_BOOK_ID_EXTRA = "BOOK_DOWNLOAD_BOOK_ID_EXTRA";
    public static final String BOOK_DOWNLOAD_PROGRESS_EXTRA = "BOOK_DOWNLOAD_PROGRESS_EXTRA";

    private static final int CALLBACK_DOWNLOAD_PROGRESS = 0;
    private static final int CALLBACK_DOWNLOAD_PROGRESS_DELAY = 500;
    private static final String BOOK_DIRECTORY = "books";

    private static BookDownloadController sInstance = null;

    private final Context mContext;
    private final DownloadManager mDownloadManager;
    private final LongSparseArray<Book> mDownloadMap = new LongSparseArray<Book>();
    private final Set<Long> mCancelled = new HashSet<Long>();

    /**
     * Send a new download manager callback
     */
    private void refreshProgressCallback() {
        removeMessages(CALLBACK_DOWNLOAD_PROGRESS);
        sendEmptyMessageDelayed(CALLBACK_DOWNLOAD_PROGRESS, CALLBACK_DOWNLOAD_PROGRESS_DELAY);
    }

    /*
     * (non-Javadoc)
     * @see android.os.Handler#handleMessage(android.os.Message)
     */
    @Override
    public void handleMessage(Message msg) {
        if (msg.what == CALLBACK_DOWNLOAD_PROGRESS) {
            new Thread() {
                @Override
                public void run() {
                    updateDownloadProgress();
                }
            }.start();
            if (mDownloadMap.size() != 0) {
                refreshProgressCallback();
            }
        }
    }

    /**
     * Static singleton getInstance
     *
     * @param context
     * @return the {@link BookDownloadController} singleton object
     */
    public static BookDownloadController getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BookDownloadController(context.getApplicationContext());
        }
        return sInstance;
    }

    /**
     * Private constructor
     *
     * @param context
     */
    private BookDownloadController(Context context) {
        this.mContext = context;
        this.mDownloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    /**
     * Update the download progress from the DownloadManager database.
     */
    private void updateDownloadProgress() {
        Cursor cursor = mDownloadManager.query(new Query());

        if (cursor == null) {
            //apparently the DownloadManager.query can return null, although the docs do not specify this
            return;
        }

        int downloadIdIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID);
        int sizeIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
        int downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);

        while (cursor.moveToNext()) {
            long downloadId = cursor.getLong(downloadIdIndex);
            Book book = mDownloadMap.get(downloadId);
            if (book == null) {
                continue;
            }

            int status = cursor.getInt(statusIndex);
            long file_size = cursor.getLong(sizeIndex);
            long download_offset = cursor.getLong(downloadedIndex);
            String filePath = "";

            boolean notifyChange = true;
            Uri uri = Books.buildDownloadBookIdUri(book.id);
            int download_status = Books.DOWNLOAD_FAILED;

            if (status == DownloadManager.STATUS_SUCCESSFUL) {

                try {
                    AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_BOOK_DOWNLOADS, AnalyticsHelper.GA_EVENT_DOWNLOAD_STATUS_SUCCESS, book.isbn, null);
                } catch (NumberFormatException e) {
                }

                Uri downloadPath = mDownloadManager.getUriForDownloadedFile(downloadId);
                if (downloadPath != null && new File(downloadPath.getPath()).exists()) {
                    filePath = downloadPath.getPath();
                    download_status = Books.DOWNLOADED;
                } else {
                    download_status = Books.NOT_DOWNLOADED;
                }
                mDownloadMap.remove(downloadId);

                NotificationUtil.showDownloadCompleteNotification(mContext, book);


            } else if (status == DownloadManager.STATUS_PENDING || status == DownloadManager.STATUS_RUNNING) {
                notifyChange = false;
                download_status = Books.DOWNLOADING;

                Intent intent = new Intent();
                intent.setAction(mContext.getPackageName() + BOOK_DOWNLOAD_PROGRESS_ACTION);
                intent.putExtra(BOOK_DOWNLOAD_BOOK_ID_EXTRA, book.id);

                double progress = (((double) download_offset) / file_size) * 100;
                intent.putExtra(BOOK_DOWNLOAD_PROGRESS_EXTRA, progress);
                mContext.sendBroadcast(intent);
            } else {
                mDownloadManager.remove(downloadId);
            }

            if (download_status == Books.DOWNLOAD_FAILED) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_DOWNLOAD_ERROR, AnalyticsHelper.GA_LABEL_DOWNLOAD_FAILED, null);
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_BOOK_DOWNLOADS, AnalyticsHelper.GA_EVENT_DOWNLOAD_STATUS_FAILED, book.isbn, null);

                int reason = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON));

                if (reason == DownloadManager.ERROR_INSUFFICIENT_SPACE) {

                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, R.string.error_insufficient_space_for_book, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            ContentValues contentValues = new ContentValues();

            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                contentValues.put(BBBContract.BooksColumns.BOOK_IN_DEVICE_LIBRARY, true);
            }

            contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_STATUS, download_status);
            contentValues.put(BBBContract.BooksColumns.BOOK_SIZE, file_size);
            contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_OFFSET, download_offset);
            contentValues.put(BBBContract.BooksColumns.BOOK_FILE_PATH, filePath);

            mContext.getContentResolver().update(uri, contentValues, null, null);
            if (notifyChange) {
                mContext.getContentResolver().notifyChange(Books.buildBookIdUri(book.id), null, false);
            }
        }
        cursor.close();
    }

    /**
     * Action to perform when the download button on a book is clicked
     *
     * @param context
     * @param book
     */
    public void startDownloadClicked(Context context, final Book book) {

        if (!NetworkUtils.hasInternetConnectivity(context)) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_DOWNLOAD_ERROR, AnalyticsHelper.GA_LABEL_NO_NETWORK, null);

            BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(context);
            builder.setTitle(R.string.title_device_offline);
            builder.setMessage(R.string.dialog_device_is_offline);
            builder.setNeutralButton(R.string.button_close, null);
            builder.show();
        } else if (!NetworkUtils.isConnectedWithWifi(context)) {
            BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(context);
            builder.setTitle(R.string.dialog_use_wifi_for_optimum_download_experience);
            builder.setMessage(R.string.dialog_are_you_sure_you_want_to_continue);
            builder.setNegativeButton(android.R.string.no, null);
            builder.setPositiveButton(android.R.string.yes, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startDownloadBook(book);
                }
            }).show();
        } else {
            startDownloadBook(book);
        }
    }

    /**
     * Cancel an ongoing download
     *
     * @param book
     */
    public void cancelDownloadClicked(Book book) {
        mCancelled.add(book.id);
        long downloadId = -1;

        for (int i = 0; i < mDownloadMap.size(); i++) {
            long key = mDownloadMap.keyAt(i);
            Book item = mDownloadMap.get(key);
            if (book != null && item != null && book.id == item.id) {
                downloadId = key;
                break;
            }
        }

        if (downloadId != -1) {
            mDownloadManager.remove(downloadId);
            mDownloadMap.remove(downloadId);
        }

        book.download_status = BBBContract.Books.NOT_DOWNLOADED;
        BBBAsyncQueryHandler.getInstance().startUpdate(0, null, Books.buildBookIdUri(book.id), BookHelper.getContentValues(book), null, null);
    }

    /**
     * Start downloading a book object
     *
     * @param book
     */
    public void startDownloadBook(Book book) {
        mCancelled.remove(book.id);
        new BookDownloader(book).start();
    }

    /*
     * Goes through the required steps to download a book. Ensures we have an access token which has been associated with a device, downloads the key required for decryption and
     * if all prior steps are successful, begins the download of a book.
     */
    private class BookDownloader extends Thread {

        private Book mBook;
        private Uri mBookUri;

        public BookDownloader(Book book) {
            mBook = book;
            mBookUri = Books.buildBookIdUri(book.id);
        }

        public void run() {

            if (mBook == null) {
                LogUtils.e(TAG, "Download failed. Book object was null");
                return;
            }

            mBook.download_offset = 0;
            mBook.download_status = BBBContract.Books.DOWNLOADING;
            BookHelper.updateBook(mBookUri, mBook, true);

            LogUtils.d(TAG, "Downloading book: " + mBook.title + " by " + mBook.author);

            if (mBook.enc_key != null && mBook.enc_key.length > 0) {
                startDownloadBook();
                return;
            }

            ApiConnector apiConnector = ApiConnector.getInstance();

            AccountManager accountManager = AccountManager.get(mContext);

            Account account = AccountController.getInstance().getLoggedInAccount();

            if (account == null) {
                LogUtils.e(TAG, "Download failed. Could not get Account object");
                failDownload();
                return;
            } else {
                LogUtils.i(TAG, "Got account object");
            }

            String accessToken = AccountController.getInstance().getAccessToken(account);

            if (TextUtils.isEmpty(accessToken)) {
                LogUtils.e(TAG, "Download failed. Could not get access token");
                failDownload();
                return;
            } else {
                LogUtils.i(TAG, "Got access token");
            }

            if (mBook.media_path == null) {
                LogUtils.e(TAG, "Download failed. Book media path was null");
                // We don't expect to see a null media path, so log to Crashlytics so we can get a handle on any failures of this type

                Crashlytics.log(String.format("Download failed - null media_path: " + mBook.isbn + " (" + mBook.title + ") embedded? " + mBook.embedded_book + " download_status " + mBook.download_status));
                Crashlytics.logException(new RuntimeException("Null media path"));
                failDownload();
                return;
            }

            //TODO: Remove the http:// to https:// redirect. This is a hack to force https (See ALA-1437)
            long size = apiConnector.getFileSize(mBook.media_path.replace("http://", "https://"));

            StatFs stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();

            if (size > bytesAvailable) {//not enough storage space available
                failDownload();
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(BaseActivity.ACTION_INSUFFICIENT_STORAGE_SPACE));
                return;
            }

            String client_secret = accountManager.getUserData(account, BBBApiConstants.PARAM_CLIENT_SECRET);

            if (TextUtils.isEmpty(client_secret)) {
                LogUtils.i(TAG, "No client secret stored. Attempting to register device.");

                BBBResponse response = apiConnector.registerDevice(DeviceUtils.getClientName(mContext), DeviceUtils.getClientBrand(),
                        DeviceUtils.getClientModel(), DeviceUtils.getClientOs());

                if (response != null) {

                    if (response.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        accountManager.invalidateAuthToken(BBBApiConstants.ACCOUNT_TYPE, accessToken);

                        String newToken = AccountController.getInstance().getAccessToken(account);

                        if (!TextUtils.isEmpty(newToken)) {
                            accessToken = newToken;
                            LogUtils.d(TAG, "Got new device associated access token");
                        } else {
                            LogUtils.e(TAG, "Download failed. Could not get new access token after registering device");
                            failDownload();
                            return;
                        }
                    } else if (response.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST) {

                        try {
                            BBBAuthenticationError error = new Gson().fromJson(response.getResponseData(), BBBAuthenticationError.class);

                            if (error != null && BBBApiConstants.ERROR_CLIENT_LIMIT_REACHED.equals(error.error_reason)) {
                                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent(BaseActivity.ACTION_CLIENT_LIMIT_EXCEEDED));
                            }

                        } catch (Exception e) {
                            LogUtils.e(TAG, e.getMessage(), e);
                        }
                    } else {
                        LogUtils.e(TAG, "Download failed. Could not register device");
                        failDownload();
                        return;
                    }
                }
            } else {
                LogUtils.i(TAG, "Verified we have client secret");
            }

            //if book is sample we do not need the key. we can just download
            if (mBook.sample_book) {
                mBook.enc_key = null;
                BookHelper.updateBook(mBookUri, mBook, true);

                startDownloadBook();
                return;
            }

            KeyPair pair = EncryptionUtil.generateRSAKey(EncryptionUtil.KEY_SIZE_2048);
            PublicKey publicKey = pair.getPublic();

            String publicKeyBase64 = Base64.encodeToString(publicKey.getEncoded(), Base64.URL_SAFE | Base64.NO_WRAP).trim();

            LogUtils.i(TAG, "Getting decryption key");

            BBBResponse response = apiConnector.getDecryptionKey(mBook.key_path, publicKeyBase64);

            String decryptionKeyBase64 = null;

            if (response.getResponseCode() == HttpURLConnection.HTTP_OK || response.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
                decryptionKeyBase64 = response.getResponseData();
            } else if (response.getResponseCode() == HttpURLConnection.HTTP_CONFLICT) {
                AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_DOWNLOAD_ERROR, AnalyticsHelper.GA_LABEL_EXCEED_DOWNLOAD_LIMIT, null);

                LogUtils.e(TAG, "Download failed. Book has already been downloaded to the maximum number of allowed devices");
                failDownload();
                LibraryController.getInstance().showDownloadLimitExceededError(mBook);
                return;
            } else {
                LogUtils.e(TAG, "Download failed. Could not get key. Error: " + response.getResponseCode());
                failDownload();
                return;
            }

            if (TextUtils.isEmpty(decryptionKeyBase64)) {
                LogUtils.e(TAG, "Download failed. Could not get decryption key");
                failDownload();
                return;
            } else {
                LogUtils.d(TAG, "Got encryption key '" + decryptionKeyBase64 + "'");
            }

            byte[] decryptionKey = Base64.decode(decryptionKeyBase64, Base64.DEFAULT);

            byte[] decryptedBytes = EncryptionUtil.decryptRSAEncryptedBytes(decryptionKey, pair.getPrivate());

            if (decryptedBytes == null || decryptedBytes.length == 0) {
                LogUtils.e(TAG, "Download failed. Could not decrypt key");
                failDownload();
                return;
            } else {
                LogUtils.d(TAG, "Decrypted key ok '" + new String(decryptedBytes) + "'");
            }

            // TODO - remove when key length issue fixed
            int keyLength = 32;
            int startIndex = decryptedBytes.length - keyLength;

            if (startIndex > 0) {
                byte[] dkey = new byte[keyLength];

                for (int i = 0; i < keyLength; i++) {
                    dkey[i] = decryptedBytes[startIndex + i];
                }

                decryptedBytes = EncryptionUtil.encryptEncryptionKey(dkey);
            }

            mBook.enc_key = decryptedBytes;
            BookHelper.updateBook(mBookUri, mBook, true);

            startDownloadBook();
        }

        private void startDownloadBook() {
            if (mCancelled.contains(mBook.id)) {
                LogUtils.d(TAG, "Book " + mBook.id + " download cancelled ");
                mBook.download_status = Books.NOT_DOWNLOADED;
                BookHelper.updateBook(mBookUri, mBook, true);
                return;
            }

            String existingFilePath = BookHelper.getExistingFilePath(mBook);

            if (!TextUtils.isEmpty(existingFilePath)) {
                LogUtils.d(TAG, "Existing book found: " + mBook.media_path + " = " + existingFilePath);

                ContentValues contentValues = new ContentValues();
                contentValues.put(BBBContract.BooksColumns.BOOK_IN_DEVICE_LIBRARY, true);
                contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_STATUS, Books.DOWNLOADED);
                contentValues.put(BBBContract.BooksColumns.BOOK_FILE_PATH, existingFilePath);
                mContext.getContentResolver().update(mBookUri, contentValues, null, null);

                return;
            } else {

                if (mBook.media_path == null) {
                    LogUtils.e(TAG, "null media path for for book:" + mBook);
                    mBook.download_status = Books.DOWNLOAD_FAILED;
                    BookHelper.updateBook(mBookUri, mBook, true);
                    return;
                }

                //TODO: Remove this code below: This is a hack to force http to https (See ALA-1437)
                Uri mediaPath = Uri.parse(mBook.media_path.replace("http://", "https://"));

                LogUtils.d(TAG, "Starting media download: " + mediaPath + " -> " + mediaPath.getLastPathSegment());

                Request request = new DownloadManager.Request(mediaPath);
                request.setVisibleInDownloadsUi(false);
                try {
                    // In some rare cases this call can throw a null pointer exception (see ALA-1922). This was previously causing
                    // occasional crashes when a user tries to download a book
                    request.setDestinationInExternalFilesDir(mContext, BOOK_DIRECTORY, mediaPath.getLastPathSegment());
                } catch (NullPointerException e) {
                    // If we do see a null pointer exception then we mark the book as failed and log some information to Crashlytics as a handled exception
                    File externalDir = mContext.getExternalFilesDir(BOOK_DIRECTORY);
                    Crashlytics.log(String.format("mediaPath=" + mediaPath + " externalDir=" + (externalDir != null ? externalDir.getAbsolutePath() : "null")));
                    Crashlytics.logException(e);
                    mBook.download_status = Books.DOWNLOAD_FAILED;
                    BookHelper.updateBook(mBookUri, mBook, true);
                    return;
                }

                request.setNotificationVisibility(Request.VISIBILITY_HIDDEN);
                request.setTitle(mBook.author);
                request.setDescription(mBook.title);

                try {
                    long downloadId = mDownloadManager.enqueue(request);
                    mDownloadMap.put(downloadId, mBook);
                    refreshProgressCallback();

                    ContentValues contentValues = new ContentValues();
                    contentValues.put(BBBContract.BooksColumns.BOOK_DOWNLOAD_STATUS, Books.DOWNLOADING);
                    mContext.getContentResolver().update(mBookUri, contentValues, null, null);

                    // download a full sized cover image once download of book begins
                    BBBImageLoader.getInstance().get(mBook.getFormattedFullcoverUrl(), BBBImageLoader.IGNORE_HANDLER, Priority.NORMAL);
                } catch (IllegalArgumentException e) {

                    // It is possible for the user to disable the download manager, which was causing crashes when trying to download a book.
                    // To prevent these crashes we catch this exception and handle accordingly.
                    mBook.download_status = Books.DOWNLOAD_FAILED;
                    BookHelper.updateBook(mBookUri, mBook, true);

                    // Inform the user they need to enable the download manager (via a toast message)
                    Handler mainHandler = new Handler(mContext.getMainLooper());
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext,R.string.enable_download_manager,Toast.LENGTH_LONG).show();
                        }
                    });
                    try {
                        // Attempt to take the user to download manager screen where they can re-enable it
                        final String downloadManagerPackage = "com.android.providers.downloads";

                        //Open the specific App Info page:
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setData(Uri.parse("package:" + downloadManagerPackage));
                        mContext.startActivity(intent);

                    } catch ( ActivityNotFoundException ex ) {
                        // This should not happen but if the user has some wacky device where they don't have that package we just swallow the exception.
                        // They will at least have seen the download manager popup message to prompt them to do something
                    }
                }
            }
        }

        private void failDownload() {
            mBook.download_status = BBBContract.Books.DOWNLOAD_FAILED;
            BookHelper.updateBook(mBookUri, mBook, true);

            refreshProgressCallback();

            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_ERROR, AnalyticsHelper.GA_EVENT_DOWNLOAD_ERROR, AnalyticsHelper.GA_LABEL_DOWNLOAD_FAILED, null);
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_BOOK_DOWNLOADS, AnalyticsHelper.GA_EVENT_DOWNLOAD_STATUS_FAILED, mBook.isbn, null);
        }
    }
}