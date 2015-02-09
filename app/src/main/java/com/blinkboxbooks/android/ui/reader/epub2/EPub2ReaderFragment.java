// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.blinkbox.java.book.factory.BBBEPubFactory;
import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.model.reader.Event;
import com.blinkboxbooks.android.provider.BBBContract.Bookmarks;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.reader.ReaderFragment;
import com.blinkboxbooks.android.ui.reader.TextSelectionListener;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController;
import com.blinkboxbooks.android.util.DebugUtils;
import com.blinkboxbooks.android.util.DialogUtil;
import com.blinkboxbooks.android.util.EncryptionUtil;
import com.blinkboxbooks.android.util.NetworkUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import it.sephiroth.android.library.imagezoom.ImageViewTouch;

/**
 * ReaderFragment containing ReaderWebView, title header and footer read percentage
 */
public class EPub2ReaderFragment extends ReaderFragment implements EPub2ReaderJSCallbacks {

    private static final String TAG = EPub2ReaderFragment.class.getSimpleName();
    private static final String PARAM_SAVED_READING_POSITION = "saved_reading_position";

    private static final String TAG_DIALOG_FRAGMENT = "dialog_fragment";

    // The maximum width and/or height that we allow a full screen image to be loaded into.
    private static final int MAX_FULL_SCREEN_BITMAP_DIMENSION_IN_PIXELS = 2000;

    // The minimum width or dimension that an image must adhere to before we allow it to be shown full screen
    private static final int MIN_FULL_SCREEN_BITMAP_DIMENSION_IN_PIXELS = 256;

    private static final int FULL_SCREEN_IMAGE_ALPHA_ANIM_DURATION_MS = 400;

    private Book mBook;
    private BBBEPubBook mEPubBook;
    private Bookmark mLastReadingPosition;
    private String mSavedReadingPosition;
    private List<String> mBookmarksOnPage;
    private Set<String> mHighlightsOnPage;
    private String mReaderVersion;

    private EPub2ReaderController mReaderController;
    private EPub2TouchHandler mTouchHandler;
    private EPub2ReaderCallback mReaderCallback;
    private EPub2ReaderWebView mReaderWebView;

    private Handler mHandler;
    private BookmarkContentObserver mBookmarkContentObserver;

    private View mBackToPreviousPositionContainer;
    private Button mButtonGoToPrevious;
    private ProgressBar mProgressLoading;
    private ProgressBar mFullScreenImageLoading;
    private ImageViewTouch mFullScreenImage;

    private int mDuration = 0;
    private int mStartDelay = 0;

    private String mPreviewUrl = null;
    private boolean mShowingPreview = false;
    private TextSelectionListener mTextSelectionListener;
    private String mHighlightContent = null;

    private View mViewRoot;
    private float mBackgroundTouchEventX;

    @Override
    public void setTextSelectionListener(TextSelectionListener textSelectionListener) {
        mTextSelectionListener = textSelectionListener;

        if(mReaderWebView != null) {
            mReaderWebView.setTextSelectionListener(textSelectionListener);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
        mStartDelay = getResources().getInteger(android.R.integer.config_mediumAnimTime);
    }

    @Override
    public int getHeight() {
        return mViewRoot == null ? 0 : mViewRoot.getHeight();
    }

    @Override
    public void hideTextSelector() {

        if(mReaderWebView != null) {
            mReaderWebView.closeActionMode();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        mViewRoot = inflater.inflate(R.layout.fragment_reader_epub2, container, false);

        mBackToPreviousPositionContainer = mViewRoot.findViewById(R.id.layout_go_to_previous);
        mButtonGoToPrevious = (Button)mViewRoot.findViewById(R.id.button_go_to_previous);
        mReaderWebView = (EPub2ReaderWebView) mViewRoot.findViewById(R.id.webview_reader);
        mProgressLoading = (ProgressBar) mViewRoot.findViewById(R.id.progress_loading);
        mFullScreenImageLoading = (ProgressBar) mViewRoot.findViewById(R.id.full_screen_image_loading);
        View backgroundTouchView = mViewRoot.findViewById(R.id.background_touch_view);

        mReaderWebView.requestFocus();

        if(mTextSelectionListener != null) {
            mReaderWebView.setTextSelectionListener(mTextSelectionListener);
        }

        mFullScreenImage = (ImageViewTouch) mViewRoot.findViewById(R.id.imageview_full_screen_image);

        mFullScreenImage.setSingleTapListener(new ImageViewTouch.OnImageViewTouchSingleTapListener() {
            @Override
            public void onSingleTapConfirmed() {
                hideFullScreenImage();
            }
        });

        // enable WebViews for debugging on Kitkat
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {

            if (BuildConfig.DEBUG) {
                enableLoggingApi19Plus();
            }
        }

        Activity activity = getActivity();

        CRCErrorHandler errorHandler = new CRCErrorHandler((BaseActivity) activity);
        mReaderController = new EPub2ReaderController(mReaderWebView, this, errorHandler);
        mHandler = new Handler();

        if (activity instanceof EPub2ReaderCallback) {
            mReaderCallback = (EPub2ReaderCallback) activity;
        } else {
            mReaderCallback = null;
        }

        mTouchHandler = new EPub2TouchHandler(getActivity(), mReaderController, mReaderCallback);

        // Because the web view now shrinks when the overlay is visible we listen for touch events on the background
        // so we can turn the page when the user clicks
        backgroundTouchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            @SuppressLint("ClickableViewAccessibility")
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mBackgroundTouchEventX = event.getX();
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    mTouchHandler.processTap(mBackgroundTouchEventX);
                }
                return true;
            }
        });

        String savedReadingPosition = null;

        if (savedInstanceState != null) {
            savedReadingPosition = savedInstanceState.getString(PARAM_SAVED_READING_POSITION);
        }

        setSavedReadingPosition(savedReadingPosition, null);

        mViewRoot.findViewById(R.id.button_go_to_previous).setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                mReaderController.jumpToCFI(mSavedReadingPosition);
                setSavedReadingPosition(null, null);
            }
        });

        mViewRoot.findViewById(R.id.button_dismiss_go_to_previous).setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                setSavedReadingPosition(null, null);
            }
        });

        if(mShowingPreview) {
            mReaderController.showPreview(mPreviewUrl, mLastReadingPosition);
        }

        return mViewRoot;
    }

    @Override
    public void onResume() {
        super.onResume();
        EPub2ReaderSettingController.getInstance().setEPub2SettingsChangedListener(mReaderController.getEPub2ReaderJSHelper());
        EPub2ReaderSettingController.getInstance().setReaderBrightnessListener(this);
    }

    @TargetApi(19)
    private void enableLoggingApi19Plus() {
        WebView.setWebContentsDebuggingEnabled(true);
    }

    class BookmarkContentObserver extends ContentObserver {

        public BookmarkContentObserver(Handler h) {
            super(h);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);

            if (!selfChange) {
                mReaderController.asyncSetJSBookmarks(mBook.id);
                mReaderController.asyncSetJSHighlights(mBook.id);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        EPub2ReaderSettingController.getInstance().setEPub2SettingsChangedListener(null);

        if (mBookmarkContentObserver != null) {
            getActivity().getContentResolver().unregisterContentObserver(mBookmarkContentObserver);
            mBookmarkContentObserver = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(PARAM_SAVED_READING_POSITION, mSavedReadingPosition);
    }

    @Override
    public void setPreview(String url, Bookmark lastReadingPosition) {
        mShowingPreview = true;
        mPreviewUrl = url;

        mLastReadingPosition = lastReadingPosition;
    }

    @Override
    public void event(final Event event) {

        if (isDetached() || getActivity() == null || ((BaseActivity) getActivity()).hasSavedInstanceState()) {
            return;
        }

        switch (event.code) {
            case Event.EVENT_LOADED://falls through
            case Event.EVENT_LOADING: {
                final boolean loading = event.code == Event.EVENT_LOADING;

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (loading) {
                            mProgressLoading.setAlpha(0.0f);
                            mProgressLoading.setVisibility(View.VISIBLE);
                            mProgressLoading.animate().alpha(1.0f).setDuration(mDuration).setStartDelay(mStartDelay);
                        } else {
                            mReaderWebView.setVisibility(View.VISIBLE);
                            mProgressLoading.setVisibility(View.GONE);
                        }
                    }
                });

                break;
            }
            case Event.EVENT_END_OF_BOOK: {
                mReaderCallback.bookFinished();
                break;
            }
            case Event.EVENT_STATUS: {
                status(event);
                break;
            }
            case Event.EVENT_ERROR_MISSING_FILE: {

                if(mShowingPreview && !NetworkUtils.hasInternetConnectivity(getActivity())) {
                    final GenericDialogFragment genericDialogFragment = GenericDialogFragment.newInstance(null, getString(R.string.error_no_internet_reading_sample), getString(R.string.button_close), null, null, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            getActivity().finish();
                        }
                    }, null, null, null);
                    ((BaseActivity) getActivity()).showDialog(genericDialogFragment, TAG_DIALOG_FRAGMENT, false);
                } else {
                    error(event);
                }

                break;
            }
            case Event.EVENT_CONTENT_NOT_AVAILABLE: {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        DialogUtil.showBookNotPartOfSampleDialog(getActivity(), mBook);
                    }
                });

                break;
            }
            case Event.EVENT_NOTICE_EXT_LINK: {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        //TODO: Remove this once the CPR fixes this bug. (CR-441)
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        final String url;
                        if (URLUtil.isValidUrl("http:/"+event.href)) {
                            url = "http:/"+event.href;
                        } else {
                            url = event.href;
                        }
                        if (URLUtil.isValidUrl(url)) {
                            Uri uri = Uri.parse(url);
                            intent.setData(uri);
                            try {
                                getActivity().startActivity(intent);
                            } catch (ActivityNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });

                break;
            }
            case Event.EVENT_UNHANDLED_TOUCH_EVENT: {
                getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            mTouchHandler.processTap(event.clientX);
                        }
                    });

                break;
            }
            case Event.EVENT_INTERNAL_LINK_CLICKED: {

                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (mLastReadingPosition != null) {
                            setSavedReadingPosition(mLastReadingPosition.position, R.string.button_back_to_your_last_page);
                        }
                    }
                });

                break;

            }

            case Event.EVENT_TEXT_SELECTED: {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {

                        if(!TextUtils.isEmpty(event.value)) {
                            if (mTextSelectionListener.textSelected(event.value, mReaderWebView.getLastTouch())) {
                                mReaderWebView.setInterceptTouches(true);
                                mReaderWebView.startActionModeIfNotActive();
                            }
                        }
                    }
                });

                break;
            }
            case Event.EVENT_IMAGE_DOUBLE_CLICKED: {
                String path = mBook.file_path + "/" + event.src;

                char[] encryptionKey = null;

                // We need to get the encryption key so we can decrypt the image from the book
                if (mBook.enc_key != null) {
                     encryptionKey = EncryptionUtil.decryptEncryptionKey(mBook.enc_key);
                }

                // Prevent user interaction up front to avoid any nasty cases where the user touches the screen while the bitmap is loading.
                mTouchHandler.setInteractionAllowed(false);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFullScreenImageLoading.setVisibility(View.VISIBLE);
                    }
                });

                final Bitmap bitmap = BBBEPubFactory.getInstance().loadBitmapFromBook(getActivity(), path, MAX_FULL_SCREEN_BITMAP_DIMENSION_IN_PIXELS,
                        MAX_FULL_SCREEN_BITMAP_DIMENSION_IN_PIXELS, encryptionKey);

                // Check that we have a valid bitmap and the bitmap has enough resolution to display full screen
                if (bitmap != null && (bitmap.getWidth() > MIN_FULL_SCREEN_BITMAP_DIMENSION_IN_PIXELS || bitmap.getHeight() > MIN_FULL_SCREEN_BITMAP_DIMENSION_IN_PIXELS)) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showFullScreenImage(bitmap);
                            mFullScreenImageLoading.setVisibility(View.GONE);
                        }
                    });
                } else {
                    // Not displaying a full screen image so turn the user interaction back on
                    mTouchHandler.setInteractionAllowed(true);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFullScreenImageLoading.setVisibility(View.GONE);
                        }
                    });
                }

                break;
            }

            case Event.EVENT_HIGHLIGHT_CLICKED: {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        mTextSelectionListener.highlightSelected(event.cfi, mReaderWebView.getLastTouch());
                    }
                });

                break;
            }
        }
    }

    @Override
    public void setBook(BBBEPubBook ePubBook, Book book, Bookmark lastPosition, String
            bookmarks, String highlights, boolean isOnlineSample) {

        if ((mEPubBook != null)&&(mProgressLoading.getVisibility() != View.VISIBLE)) {
            return;
        }

        mBook = book;
        mEPubBook = ePubBook;
        mLastReadingPosition = lastPosition;

        mProgressLoading.setVisibility(View.VISIBLE);

        mReaderController.setBook(book.isbn, mEPubBook, lastPosition, bookmarks, highlights, isOnlineSample);

        DebugUtils.setBook(book);

        Uri uri = Bookmarks.buildBookmarkTypeUri(Bookmarks.TYPE_BOOKMARK, mBook.id);
        if (mBookmarkContentObserver == null) {
            mBookmarkContentObserver = new BookmarkContentObserver(mHandler);
            getActivity().getContentResolver().registerContentObserver(uri, true, mBookmarkContentObserver);
        }
    }

    @Override
    public void setBook(Book book) {
        mBook = book;
    }

    private void setSavedReadingPosition(String cfi, Integer buttonTextResourceId) {
        mSavedReadingPosition = cfi;

        if (mSavedReadingPosition == null) {
            mBackToPreviousPositionContainer.setVisibility(View.INVISIBLE);
        } else {
            if(buttonTextResourceId != null) {
                mButtonGoToPrevious.setText(buttonTextResourceId);
            }

            mBackToPreviousPositionContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void bookmarkPage() {

        if (mBookmarksOnPage != null && mBookmarksOnPage.size() > 0) {
            for (String bookmark : mBookmarksOnPage) {
                mReaderController.removeBookmark(mBook.id, bookmark);
            }
            setBookmarkStatus(false);
        } else {
            mReaderController.setBookmark();
        }
    }

    @Override
    public void setInteractionAllowed(boolean allowInteraction) {
        if (mTouchHandler != null) {
            mTouchHandler.setInteractionAllowed(allowInteraction);
        }
    }

    /**
     * Navigates the reader to the page indicated by the lastPosition.
     */
    public void goToBookmark(Bookmark bookmark, boolean keepCurrentReadingPosition) {

        if (bookmark == null || mLastReadingPosition == null) {
            return;
        }

        if (keepCurrentReadingPosition) {
            setSavedReadingPosition(mLastReadingPosition.position, R.string.button_back_to_your_saved_position);
        } else {
            setSavedReadingPosition(null, null);

            mLastReadingPosition.position = bookmark.position;
            mLastReadingPosition.percentage = bookmark.percentage;
            mLastReadingPosition.content = bookmark.content;
        }

        mReaderController.jumpToCFI(bookmark.position);
    }

    @Override
    public void highlightSelection(String content) {
        mHighlightContent = content;
        mReaderController.setHighlight();
    }

    @Override
    public void removeHighlight(String cfi) {
        mReaderController.removeHighlight(mBook.id,cfi);
    }

    @Override
    public void toggleReaderOverlay(boolean visible) {
        mReaderController.toggleReaderOverlay(visible);
    }

    private void status(final Event event) {

        if (event.cfi == null) {
            return;
        }

        mReaderVersion = event.version;

        mBookmarksOnPage = Arrays.asList(event.bookmarksInPage);

        if (!Event.CALL_INIT.equals(event.call)) {
            mLastReadingPosition.position = event.cfi.CFI;
        }

        mLastReadingPosition.content = event.cfi.preview;
        mLastReadingPosition.percentage = (int) event.progress;

        mLastReadingPosition.name = event.cfi.chapter;

        DebugUtils.setLastPositionData(mLastReadingPosition);

        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (Event.CALL_INIT.equals(event.call) || Event.CALL_PROGRESS_LOAD.equals(event.call)) {
                    mReaderCallback.setSpine(event.book.getSpine());
                    // Product want us to display the preview options overlay by default when loading a sample.
                    // We do it here after setting up the spine else the bottom slider view will not display correctly
                    if (mPreviewUrl != null) {
                        mReaderCallback.setReaderOverlayVisibility(true);
                    }
                }
                mReaderCallback.currentReadingPositionUpdated(mLastReadingPosition, mLastReadingPosition.name);
                mProgressLoading.setVisibility(View.GONE);
                setBookmarkStatus(event.bookmarksInPage.length > 0);
            }
        });

        if (Event.CALL_SET_BOOKMARK.equals(event.call)) {

            if (event.bookmarksInPage.length == 1) {
                String cfi = event.bookmarksInPage[0];

                if (cfi.equals(mLastReadingPosition.position)) {
                    bookmarkAdded(cfi);
                } else {
                    DebugUtils.handleCPRException("bookmark exception: " + cfi + " != " + mLastReadingPosition.position);
                }
            } else {
                DebugUtils.handleCPRException("bookmark exception: " + event.bookmarksInPage.length + " != 1");
            }
        } else if (Event.CALL_SET_HIGHLIGHT.equals(event.call)) {
            for (String cfi : event.highlightsInPage) {
                //check that this highlight is not does not exist in the list of highlights on page:
                if ((mHighlightsOnPage == null)||(!mHighlightsOnPage.contains(cfi))) {
                    highlightAdded(cfi,mHighlightContent);
                    mHighlightContent = null;
                }
            }
        } else {
            showBookmark(event.bookmarksInPage.length == 1, false);
        }

        mHighlightsOnPage = new HashSet<>(Arrays.asList(event.highlightsInPage));
    }

    private void error(final Event event) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                DebugUtils.handleCPRException("CPR exception: " + event.message);
            }
        });
    }

    @Override
    public void goToChapter(String url, boolean keepCurrentReadingPosition) {

        if (keepCurrentReadingPosition && mLastReadingPosition != null) {
            setSavedReadingPosition(mLastReadingPosition.position, R.string.button_back_to_your_saved_position);
        } else {
            setSavedReadingPosition(null, null);
        }

        mReaderController.jumpToUrl(url);
    }

    @Override
    public void goToProgress(float progress) {
        mReaderController.goToProgress(progress);
    }

    private void setBookmarkStatus(boolean bookmarked) {
        mReaderCallback.bookmarkStatus(bookmarked);
    }

    private void bookmarkAdded(final String cfi) {
        Bookmark bookmark = BookmarkHelper.createBookmark(mLastReadingPosition.book_id);
        bookmark.type = Bookmarks.TYPE_BOOKMARK;
        bookmark.name = mLastReadingPosition.name;
        bookmark.content = mLastReadingPosition.content;
        bookmark.percentage = mLastReadingPosition.percentage;
        bookmark.isbn = mBook.isbn;
        bookmark.position = mLastReadingPosition.position;
        BookmarkHelper.updateBookmark(bookmark, false);

        showBookmark(true, true);
    }

    private void highlightAdded(final String cfi, final String content) {
        Bookmark bookmark = BookmarkHelper.createBookmark(mLastReadingPosition.book_id);
        bookmark.type = Bookmarks.TYPE_HIGHLIGHT;
        bookmark.name = mLastReadingPosition.name;
        bookmark.content = content;
        bookmark.percentage = mLastReadingPosition.percentage;
        bookmark.isbn = mBook.isbn;
        bookmark.position = cfi;
        BookmarkHelper.updateBookmark(bookmark, false);
    }

    private void showBookmark(final boolean show, final boolean hideOverlay) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setBookmarkStatus(show);
                if (hideOverlay) {
                    mReaderCallback.setReaderOverlayVisibility(false);
                }
            }
        });
    }

    @Override
    public void displayVersion() {
        if (BuildConfig.DEBUG) {
            Toast.makeText(getActivity(), mReaderVersion, Toast.LENGTH_LONG).show();
        }
    }


    private void showFullScreenImage(Bitmap bitmap) {
        // Hide the overlay (in case its currently visible)
        mReaderCallback.setReaderOverlayVisibility(false);
        // Make the system UI visible to ensure the back button is displayed
        getActivity().getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        Matrix matrix = new Matrix();
        matrix.setScale(1.0f, 1.0f);

        // Set the zoom range of the image to be between 1-4 full screens
        mFullScreenImage.setImageBitmap(bitmap, matrix, 1.0f, 4.0f);
        mFullScreenImage.setAlpha(0.0f);
        mFullScreenImage.setVisibility(View.VISIBLE);

        // Fade in the full screen image
        mFullScreenImage.animate().setDuration(FULL_SCREEN_IMAGE_ALPHA_ANIM_DURATION_MS).alpha(1.0f).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mFullScreenImage.setAlpha(1.0f);
            }
        });
    }

    private void hideFullScreenImage() {
        Activity activity = getActivity();
        if (activity != null) {
            // Set the system UI back into the low profile state to hide the back button etc.
            activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);

            // Fade out the full screen image
            mFullScreenImage.animate().setDuration(FULL_SCREEN_IMAGE_ALPHA_ANIM_DURATION_MS).alpha(0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mFullScreenImage.setVisibility(View.GONE);
                    mTouchHandler.setInteractionAllowed(true);
                }
            });
        }
    }

    @Override
    public boolean handleBackPressed() {
        // If a full screen image is displayed then we intercept the back key to dismiss the image
        if (mFullScreenImage.getVisibility() == View.VISIBLE) {
            hideFullScreenImage();
            return true;
        } else {
            return false;
        }
    }
}