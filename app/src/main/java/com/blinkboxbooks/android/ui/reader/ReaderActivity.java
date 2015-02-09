// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.OvershootInterpolator;

import com.blinkbox.java.book.json.BBBSpineItem;
import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.api.BBBApiConstants;
import com.blinkboxbooks.android.api.model.BBBBookmarkList;
import com.blinkboxbooks.android.api.model.BBBClientInformation;
import com.blinkboxbooks.android.api.model.BBBClientInformationList;
import com.blinkboxbooks.android.api.net.BBBRequest;
import com.blinkboxbooks.android.api.net.BBBRequestFactory;
import com.blinkboxbooks.android.api.net.BBBRequestManager;
import com.blinkboxbooks.android.api.net.BBBResponse;
import com.blinkboxbooks.android.api.net.responsehandler.BBBBasicResponseHandler;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.controller.BookDownloadController;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.dialog.ReaderHelpDialogFragment;
import com.blinkboxbooks.android.loader.BookFileLoader;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.BookItem;
import com.blinkboxbooks.android.model.BookType;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.ShopItem;
import com.blinkboxbooks.android.model.helper.BookHelper;
import com.blinkboxbooks.android.model.helper.BookmarkHelper;
import com.blinkboxbooks.android.model.reader.CFI;
import com.blinkboxbooks.android.provider.BBBContract.Books;
import com.blinkboxbooks.android.sync.Synchroniser;
import com.blinkboxbooks.android.ui.AboutBookActivity;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.ui.reader.epub2.EPub2ReaderCallback;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingsFragment;
import com.blinkboxbooks.android.ui.shop.SearchActivity;
import com.blinkboxbooks.android.ui.shop.ShopActivity;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBTextUtils;
import com.blinkboxbooks.android.util.BBBUriManager;
import com.blinkboxbooks.android.util.NotificationUtil;
import com.blinkboxbooks.android.util.PreferenceManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

/**
 * The Reader screen.
 */
public class ReaderActivity extends BaseActivity implements Reader, LoaderCallbacks<BookItem>, EPub2ReaderCallback,
        ReaderHelpDialogFragment.ReaderLastPositionListener, TextSelectionListener, DictionaryViewDialogFragment.DictionaryDismissedListener {

    private static final String RESPONSE_HANDLER_LAST_POSITION = "last_position";
    private static final String RESPONSE_HANDLER_CLIENT_NAME = "client_name";

    private static final int MSG_SHOW_HELP_OVERLAY = 0;
    private static final int HIGHLIGHT_MENU_ANIMATE_Y = 100;

    private static final String TAG_DIALOG = "dialog";
    private static final String TAG_DICTIONARY_DIALOG = "dictionary_dialog";

    private static final String PARAM_FULLSCREEN = "PARAM_FULLSCREEN";
    private static final String PARAM_BOOKMARKED = "PARAM_BOOKMARK";

    public static final String ACTION_GOTO_BOOKMARK = "goto_bookmark";
    public static final String ACTION_BOOKMARK = "bookmark";
    public static final String ACTION_ABOUT = "about";
    public static final String ACTION_HELP = "help";
    public static final String ACTION_TABLE_OF_CONTENTS = "tableofcontents";
    public static final String ACTION_SETTINGS = "settings";

    public static final String PARAM_BOOK = "book";
    public static final String PARAM_BOOKMARK = "bookmark";
    public static final String PARAM_KEEP_READING_POSITION = "keep_reading_position";
    public static final String PARAM_NEW_POSITION = "new_position";
    public static final String PARAM_SHOP_ITEM = "shop_item";
    public static final String PARAM_LAST_POSITION_FOR_PREVIEW = "last_position_for_preview";

    private static final boolean DISABLE_HIGHLIGHTS = true;

    private ReaderFragment mReaderFragment;

    private View mHeaderView;
    private View mFooterView;
    private View mBookmarkImageView;
    private View mHighlightPopup;
    private View mHighlightPopupItemHighlight;
    private View mHighlightPopupItemDelete;

    private EPub2ReaderSettingsFragment mSettings = null;

    private Uri mBookUri;
    private Book mBook;
    private Bookmark mLastPosition;
    private Bookmark mExternalLastPosition;
    private String mBookmarks;
    private String mHighlights;
    private BBBEPubBook mEPubBook;
    private boolean mFullscreen = true;
    private int mProgress;
    private String mReaderChapter;
    private String mReaderProgress;
    private boolean mBookmarked;
    private ArrayList<BBBSpineItem> mSpineItems;

    private InnerHandler mHandler;
    private DictionaryViewDialogFragment mDictionaryDialog;
    private String mDictionaryWord = "";

    private ShopItem mShopItem;
    private AlertDialog mNewPositionAlert;

    private PopupMenu mPopupMenu;
    private boolean mIsOnlineSample;

    private float mTouchDownY;
    private float mTouchDownX;
    private boolean mShowHighlightBelow;

    /**
     * Toggle the window fullscreen flag
     *
     * @param fullscreen true to enable fullscreen, false to disable
     */
    private void setFullscreen(boolean fullscreen) {
        mFullscreen = fullscreen;

        final Window window = getWindow();

        if (mFullscreen) {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
            window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            window.clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);

            window.addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
            if (Build.VERSION.SDK_INT <= 19) {
                window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            }
            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // In case we come in here via a marketing deep link we want to track in Ad-X
        AnalyticsHelper.handleAdXDeepLink(this, getIntent());

        mHandler = new InnerHandler(this);

        BBBRequestManager.getInstance().addResponseHandler(RESPONSE_HANDLER_LAST_POSITION, mBookMarkListHandler);
        BBBRequestManager.getInstance().addResponseHandler(RESPONSE_HANDLER_CLIENT_NAME, mClientListHandler);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentViewAndToolbarState(R.layout.activity_reader, false);
        final ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        supportActionBar.setIcon(null);

        if (savedInstanceState != null) {
            mBookmarked = savedInstanceState.getBoolean(PARAM_BOOKMARKED, false);
        }

        if (savedInstanceState == null) {
            mReaderFragment = ReaderFragmentFactory.createReaderFragment(BookType.EPUB2);
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.layout_reader_container, mReaderFragment);
            fragmentTransaction.commit();
        } else {
            mReaderFragment = (ReaderFragment) getSupportFragmentManager().findFragmentById(R.id.layout_reader_container);
            setFullscreen(savedInstanceState.getBoolean(PARAM_FULLSCREEN));
        }


        //When we go to action mode (when the user is selecting text) the
        //height of the activity is smaller because the action bar appears
        //this results in the webview being movable (up/down) - so to fix this
        //we hard code the webview to be exactly the screen width & height.
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        final ViewGroup.LayoutParams layoutParams = findViewById(R.id.layout_reader_container).getLayoutParams();
        layoutParams.height = size.y;
        layoutParams.width = size.x;


        if (getIntent().hasExtra(PARAM_SHOP_ITEM)) {
            mShopItem = (ShopItem) getIntent().getSerializableExtra(PARAM_SHOP_ITEM);
            mBook = mShopItem.book;
            mBook.sample_book = true;
            mIsOnlineSample = true;

            Bookmark lastPositionForPreview = new Bookmark();
            if (savedInstanceState != null) {
                // Restore the last read position for the preview if we have previously stored it (e.g. on rotation)
                lastPositionForPreview = (Bookmark) savedInstanceState.getSerializable(PARAM_LAST_POSITION_FOR_PREVIEW);
            }
            mReaderFragment.setPreview(mShopItem.book.sample_uri, lastPositionForPreview);
        } else {
            mBookUri = getIntent().getData();
        }

        mHeaderView = findViewById(R.id.reader_container_toolbar);
        mFooterView = findViewById(R.id.reader_container_footer);
        mBookmarkImageView = findViewById(R.id.reader_imageview_bookmark);
        mHighlightPopup = findViewById(R.id.reader_container_highlighpopup);
        mHighlightPopupItemHighlight = findViewById(R.id.reader_imageview_menu_highlight);
        mHighlightPopupItemDelete = findViewById(R.id.reader_imageview_menu_delete);
        mReaderFragment.setTextSelectionListener(this);


        if (Build.VERSION.SDK_INT <= 19) {
            //There are two issues with pre-lollipop devices that this hack "fixes":
            //1) if you go in to another activity from the reader with the default theme (such as
            // Table of Contents, or About this book. The colours for the toolbar of the ReaderActivity
            // take on those colours, so we display these images over the top with the correct brand
            // colours (and we set the theme colours to white, so they are not visible at all).
            //2) because we use LAYOUT_NO_LIMITS for pre-lollipop, there is a bug in these devices where
            //the popup menu will display offscreen, so for these devices we show our own popup menu.
            //We have LAYOUT_NO_LIMITS set so that the animation going from fullscreen/non fullscreen
            //is smooth - if this is not set then it will be janky.
            final View backButton = findViewById(R.id.activity_reader_image_back);
            backButton.setVisibility(View.VISIBLE);
            final View overflowButton = findViewById(R.id.activity_reader_image_overflow);
            overflowButton.setVisibility(View.VISIBLE);
            overflowButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPopupMenu.show();
                }
            });

            mPopupMenu = new PopupMenu(this, findViewById(R.id.activity_reader_popupanchor));
            mPopupMenu.inflate(R.menu.reader_actionbar_options);
            final boolean showFullOptions = (mBookUri != null);
            final Menu menu = mPopupMenu.getMenu();
            menu.findItem(R.id.action_add_bookmark).setVisible(false);
            menu.findItem(R.id.action_remove_bookmark).setVisible(false);
            menu.findItem(R.id.action_settings).setVisible(false);
            menu.findItem(R.id.action_table_of_contents).setVisible(showFullOptions);
            menu.findItem(R.id.action_about_this_book).setVisible(showFullOptions);
            menu.findItem(R.id.action_my_highlights_and_bookmarks).setVisible(showFullOptions);
            menu.findItem(R.id.action_gotoshop).setVisible(showFullOptions);
            mPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem menuItem) {
                    return onOptionsItemSelected(menuItem);
                }
            });
        }

        mBookmarkImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bookmarks are only supported for downloaded book/samples (not online)
                if (!mIsOnlineSample) {
                    handleOptionSelection("bookmark", "bbb://app/reader/bookmark");
                }
            }
        });

        // Set the fullscreen state here after the animation has completed to avoid jitter
        setFullscreen(mFullscreen);

        if (mEPubBook != null) {
            displayBookContent();
        } else {
            mReaderFragment.setBook(mBook);
        }

        mScreenName = AnalyticsHelper.GA_SCREEN_Reader;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        BBBRequestManager.getInstance().removeResponseHandler(RESPONSE_HANDLER_LAST_POSITION);
        BBBRequestManager.getInstance().removeResponseHandler(RESPONSE_HANDLER_CLIENT_NAME);

        // To prevent leaking the dialog we dismiss the new position alert if it is showing when dismissing
        if (mNewPositionAlert != null) {
            mNewPositionAlert.dismiss();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.reader_actionbar_options, menu);

        final boolean showFullOptions = (mBookUri != null);

        if (mBookmarked) {
            menu.findItem(R.id.action_remove_bookmark).setVisible(showFullOptions);
            menu.findItem(R.id.action_add_bookmark).setVisible(false);
        } else {
            menu.findItem(R.id.action_remove_bookmark).setVisible(false);
            menu.findItem(R.id.action_add_bookmark).setVisible(showFullOptions);
        }

        menu.findItem(R.id.action_table_of_contents).setVisible(showFullOptions);
        menu.findItem(R.id.action_about_this_book).setVisible(showFullOptions);
        menu.findItem(R.id.action_my_highlights_and_bookmarks).setVisible(showFullOptions);
        menu.findItem(R.id.action_gotoshop).setVisible(showFullOptions);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final String uri;
        switch (item.getItemId()) {
            case R.id.action_settings:
                uri = "bbb://app/reader/settings";
                break;
            case R.id.action_add_bookmark:
            case R.id.action_remove_bookmark:
                uri = "bbb://app/reader/bookmark";
                break;
            case R.id.action_about_this_book:
                uri = "bbb://app/reader/about";
                break;
            case R.id.action_table_of_contents:
                uri = "bbb://app/reader/tableofcontents";
                break;
            case R.id.action_my_highlights_and_bookmarks:
                uri = "bbb://app/reader/bookmarks";
                break;
            case R.id.action_gotoshop:
                uri = "bbb://urimanager/#!/shop";
                break;
            case R.id.action_help:
                uri = "bbb://app/reader/help";
                break;
            case android.R.id.home:
                // The 'up' button behaves the same as the back button in the reader
                onBackPressed();
                return true;
            default:
                uri = null;
        }

        if (uri != null) {
            handleOptionSelection(item.getTitle().toString(), uri);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mTouchDownX = ev.getX();
            mTouchDownY = ev.getY();
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public BBBEPubBook getEPubBook() {
        return mEPubBook;
    }

    @Override
    public void goToChapter(String url, boolean keepReadingPosition) {
        mReaderFragment.goToChapter(url, keepReadingPosition);
    }

    @Override
    public void goToProgress(float progress) {
        mReaderFragment.goToProgress(progress);
    }

    @Override
    protected void onStart() {
        super.onStart();
        NotificationUtil.hideNotification(this, NotificationUtil.NOTIFICATION_ID_BOOK);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBookUri != null) {
            getSupportLoaderManager().restartLoader(0, null, this);
        }
    }

    @Override
    protected void onPause() {
        // When the activity is paused we clear the dictionary. If we leave the dialog up when it gets reinstated at the top
        // there is a poor animation artifact where the full screen mode is kicking in and changes the y location of the dialog.
        // This is not something that is easy to resolve so this seems like the best compromise.
        hideDictionary();

        // Force the dictionary dialog to dismiss as we can't rely on the normal callbacks that are usually used because
        // the activity is pausing.
        if (mDictionaryDialog != null) {
            mDictionaryDialog.dismissAllowingStateLoss();
            mDictionaryDialog = null;
        }
        mDictionaryWord = "";

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Note that we don't display a notification for shop samples read online as we cannot restore them correctly
        if (mBook != null && mBook.state != Books.BOOK_STATE_FINISHED && mLastPosition != null && mShopItem == null) {
            NotificationUtil.showReadingNotification(this, mBook, mLastPosition);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getData() != null) {

            // In case we come in here via a marketing deep link we want to track in Ad-X
            AnalyticsHelper.handleAdXDeepLink(this, intent);

            String lastPath = intent.getData().getLastPathSegment();

            if (ACTION_SETTINGS.equals(lastPath)) {
                setReaderSettingsVisibility(true);
            } else if (ACTION_HELP.equals(lastPath)) {
                showReaderHelpDialog(false);
                mReaderFragment.displayVersion();
            } else if (BookmarkTabActivity.EXTRA_KEY_TAB_BOOKMARKS.equals(lastPath)
                    || BookmarkTabActivity.EXTRA_KEY_TAB_HIGHLIGHTS.equals(lastPath)
                    || BookmarkTabActivity.EXTRA_KEY_TAB_NOTES.equals(lastPath)) {
                Intent tabIntent = new Intent(this, BookmarkTabActivity.class);
                tabIntent.putExtra(BookmarkTabActivity.PARAM_BOOK, mBook);
                startActivity(tabIntent);
            } else if (ACTION_ABOUT.equals(lastPath)) {
                Intent aboutIntent = new Intent(this, AboutBookActivity.class);
                aboutIntent.putExtra(AboutBookActivity.PARAM_BOOK, mBook);
                startActivity(aboutIntent);
            } else if (ACTION_BOOKMARK.equals(lastPath)) {
                mReaderFragment.bookmarkPage();
                AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
            } else if (ACTION_GOTO_BOOKMARK.equals(lastPath)) {
                Bookmark bookmark = (Bookmark) intent.getSerializableExtra(PARAM_BOOKMARK);
                boolean keepCurrentReadingPosition = intent.getBooleanExtra(PARAM_KEEP_READING_POSITION, false);
                mReaderFragment.goToBookmark(bookmark, keepCurrentReadingPosition);
            } else if (ACTION_TABLE_OF_CONTENTS.equals(lastPath)) {
                Intent aboutIntent = new Intent(this, TableOfContentsActivity.class);
                aboutIntent.putExtra(PARAM_BOOK, mBook);
                startActivity(aboutIntent);
            }

            String gotoUrl = intent.getStringExtra(PARAM_NEW_POSITION);
            if (gotoUrl != null) {
                mReaderFragment.goToChapter(gotoUrl, intent.getBooleanExtra(PARAM_KEEP_READING_POSITION, false));
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleReaderOverlayVisiblity();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (mHeaderView.getVisibility() == View.VISIBLE) {
                toggleReaderOverlayVisiblity();
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(PARAM_FULLSCREEN, mFullscreen);
        outState.putBoolean(PARAM_BOOKMARKED, mBookmarked);

        // If we are previewing a shop item we need to persist the last read position so the user does
        // not lose their position if for example they rotate the device.
        if (mShopItem != null) {
            outState.putSerializable(PARAM_LAST_POSITION_FOR_PREVIEW, mLastPosition);
        }
    }

    @Override
    public void setReaderOverlayVisibility(boolean overlayOn) {
        final View reader = findViewById(R.id.webview_reader);
        if (overlayOn) {
            if (mHeaderView.getVisibility() == View.VISIBLE) {
                //Exit early if we are already visible
                return;
            }
            if (mSpineItems != null) {
                final FragmentManager supportFragmentManager = getSupportFragmentManager();
                Fragment footerFragment = supportFragmentManager.findFragmentById(R.id.layout_reader_footer);
                if (footerFragment == null) {
                    footerFragment = ReaderFooterFragment.newInstance(mProgress, mReaderChapter, mReaderProgress, mSpineItems);
                    supportFragmentManager.beginTransaction().add(R.id.layout_reader_footer, footerFragment).commit();
                }
                mFooterView.setVisibility(View.VISIBLE);
                mFooterView.setTranslationY(mFooterView.getHeight());
                mFooterView.setAlpha(0.0f);
                mFooterView.animate().translationY(0.0f).alpha(1.0f);
            }

            mHeaderView.setVisibility(View.VISIBLE);
            mHeaderView.setTranslationY(0 - mHeaderView.getHeight());
            mHeaderView.setAlpha(0.0f);
            final int statusBarHeight = Build.VERSION.SDK_INT <= 19 ? getStatusBarHeight() : 0;
            mHeaderView.animate().translationY(statusBarHeight).alpha(1.0f);

            mBookmarkImageView.animate().translationYBy(statusBarHeight + getSupportActionBar().getHeight()).setInterpolator(new AnticipateInterpolator());

            reader.animate().scaleX(0.8f).scaleY(0.8f).setInterpolator(new OvershootInterpolator());
            hideHighlightPopup();
        } else {
            if (mHeaderView.getVisibility() == View.INVISIBLE) {
                //Exit early if we are already invisible
                //This will stop a bug where the footer view animator listener will be notified &
                //make the footer/header view invisible after a page turn.
                return;
            }


            reader.animate().scaleX(1.0f).scaleY(1.0f).setInterpolator(new OvershootInterpolator());
            mBookmarkImageView.animate().translationY(0f).setInterpolator(new OvershootInterpolator());
            mHeaderView.animate().alpha(0.0f).translationY(0 - mHeaderView.getHeight());
            mFooterView.animate().alpha(0.0f).translationY(mFooterView.getHeight()).setListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mFooterView.animate().setListener(null);
                    mHeaderView.setVisibility(View.INVISIBLE);
                    mFooterView.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        setFullscreen(!overlayOn);
        mReaderFragment.toggleReaderOverlay(!overlayOn);
    }

    @Override
    public void toggleReaderOverlayVisiblity() {
        setReaderOverlayVisibility(mHeaderView.getVisibility() != View.VISIBLE);
    }

    private void showDictionary(String word, MotionEvent lastTouch) {

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        final int searchTopMargin = findViewById(R.id.toolbar).getHeight();

        // We want to slide from the top if the user clicks in the bottom half and slide from the bottom if they click in the top.
        // The slide from top calculation allows for the fact that the search top margin will be visible when displaying the dictionary
        // there adjust the touch point and half way point accordingly.
        boolean slideFromTop = (lastTouch.getY() - searchTopMargin) > (metrics.heightPixels - searchTopMargin) / 2;

        // If an existing dictionary dialog is visible then we just update it
        DictionaryViewDialogFragment fragment = (DictionaryViewDialogFragment) getSupportFragmentManager().findFragmentByTag(TAG_DICTIONARY_DIALOG);

        if (fragment != null) {
            fragment.updateWord(word);
        } else {
            // Create a new dictionary dialog and display it
            mDictionaryDialog = DictionaryViewDialogFragment.newInstance(word, slideFromTop, searchTopMargin);
            mDictionaryDialog.show(getSupportFragmentManager(), TAG_DICTIONARY_DIALOG);
            mDictionaryDialog.setDictionaryDismissedListener(this);
            mReaderFragment.setInteractionAllowed(false);

            String userId = String.format("userId: %s", AccountController.getInstance().getUserId());
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_CALL_TO_ACTIONS, AnalyticsHelper.GA_EVENT_DICTIONARY_INVOKED, userId, null);
        }
    }

    private void setReaderSettingsVisibility(boolean visible) {
        if (visible) {
            if (mSettings == null) {
                mSettings = ReaderFragmentFactory.createReaderSettingFragment(BookType.EPUB2);
                mSettings.setStyle(DialogFragment.STYLE_NO_TITLE, R.style.ReaderSettingsDialog);
            }
            showDialog(mSettings, TAG_DIALOG, false);
        } else {
            mSettings.dismiss();
        }
    }

    private void displayBookContent() {
        mReaderFragment.setBook(mEPubBook, mBook, mLastPosition, mBookmarks, mHighlights, mIsOnlineSample);
        mBook.state = Books.BOOK_STATE_READING;
        BookHelper.updateBookReadingStatus(mBook.id, mBook.state);

        // Show the help page the first time the reader is opened
        if (!PreferenceManager.getInstance().getBoolean(PreferenceManager.PREF_KEY_SHOWN_READER_HELP, false)) {
            PreferenceManager.getInstance().setPreference(PreferenceManager.PREF_KEY_SHOWN_READER_HELP, true);
            mHandler.sendEmptyMessage(MSG_SHOW_HELP_OVERLAY);
        } else {
            requestLastPosition();
        }
    }

    private void showReaderHelpDialog(boolean checkLastPosition) {
        ReaderHelpDialogFragment readerHelpDialogFragment = ReaderHelpDialogFragment.newInstance(null, checkLastPosition);
        showDialog(readerHelpDialogFragment, ReaderHelpDialogFragment.TAG_READER_OVERLAY, false);
    }

    @Override
    public void requestLastPosition() {

        if (AccountController.getInstance().isLoggedIn()) {
            mExternalLastPosition = null;
            BBBRequest request = BBBRequestFactory.getInstance().createGetBookmarksRequest(mBook.isbn, null, null, BBBApiConstants.BOOKMARK_TYPE_LAST_READ_POSITION);
            BBBRequestManager.getInstance().executeRequest(RESPONSE_HANDLER_LAST_POSITION, request);
        }
    }

    private void updateReaderProgress(int progress, String chapter) {
        int reader_string;
        String progressString;
        String chapterString;

        if (mShopItem != null) {
            reader_string = R.string.reader_chapter_sample;
            chapterString = TextUtils.isEmpty(chapter) ? "" : getString(R.string.reader_chapter, chapter);
        } else if (!TextUtils.isEmpty(chapter)) {

            if (mEPubBook.getBookInfo().isSample()) {
                reader_string = R.string.reader_chapter_sample;
            } else {
                reader_string = R.string.reader_chapter_read;
            }

            chapterString = getString(R.string.reader_chapter, chapter);
        } else {

            if (mEPubBook.getBookInfo().isSample()) {
                reader_string = R.string.reader_sample;
            } else {
                reader_string = R.string.reader_read;
            }

            chapterString = "";
        }

        progressString = getString(reader_string, progress);

        mProgress = progress;
        mReaderChapter = chapterString;
        mReaderProgress = progressString;

        ReaderFooterFragment footerFragment = (ReaderFooterFragment) getSupportFragmentManager().findFragmentById(R.id.layout_reader_footer);
        if (footerFragment != null) {
            footerFragment.readerProgressUpdated(mProgress, mReaderChapter, mReaderProgress);
        }
    }

    private void hideDictionary() {
        mReaderFragment.hideTextSelector();
    }

    @Override
    public void bookFinished() {
        mBook.state = Books.BOOK_STATE_FINISHED;
        BookHelper.updateBookReadingStatus(mBook.id, mBook.state);

        AccountController.getInstance().requestSynchronisation();

        Intent intent;
        if (mBook.sample_book) {
            intent = new Intent(this, EndOfSampleActivity.class);
            intent.putExtra(EndOfSampleActivity.PARAM_BOOK, mBook);
        } else {
            intent = new Intent(this, EndOfBookActivity.class);
            intent.putExtra(EndOfBookActivity.PARAM_BOOK, mBook);
        }

        startActivity(intent);
    }

    @Override
    public void currentReadingPositionUpdated(Bookmark readingPosition, String chapter) {
        //Workaround for CR-109
        final String gotoUrl = getIntent().getStringExtra(PARAM_NEW_POSITION);

        mHighlightPopup.setVisibility(View.INVISIBLE);
        hideDictionary();

        if (gotoUrl != null) {
            getIntent().removeExtra(PARAM_NEW_POSITION);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mReaderFragment.goToChapter(gotoUrl, getIntent().getBooleanExtra(PARAM_KEEP_READING_POSITION, false));
                }
            });
        }

        mLastPosition = readingPosition;
        mLastPosition.update_by = BBBTextUtils.getIdFromGuid(AccountController.getInstance().getClientId());
        updateReaderProgress(mLastPosition.percentage, chapter);

        if (mEPubBook != null) {
            BookmarkHelper.updateBookmark(mLastPosition, true);
            AccountController.getInstance().requestSynchronisation(Synchroniser.SYNC_BOOKMARKS);
        }
    }

    @Override
    public void bookmarkStatus(boolean bookmarked) {
        if (mBookmarkImageView.getVisibility() == View.GONE) {
            mBookmarkImageView.setVisibility(View.VISIBLE);
            if (mBookmarked == bookmarked) {
                mBookmarkImageView.setAlpha(0.0f);
                return;
            }
        }

        if (mBookmarked != bookmarked) {
            mBookmarked = bookmarked;
            if (mBookmarked) {
                mBookmarkImageView.animate().alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setInterpolator(new OvershootInterpolator());
                mBookmarkImageView.setContentDescription(getString(R.string.reader_bookmark_on_page));
            } else {
                mBookmarkImageView.animate().alpha(0.0f).scaleX(2.0f).scaleY(0.0f).setInterpolator(new AnticipateInterpolator());
                mBookmarkImageView.setContentDescription(getString(R.string.reader_bookmark_off_page));
            }
            supportInvalidateOptionsMenu();
        }
    }

    @Override
    public void setSpine(ArrayList<BBBSpineItem> spineItems) {
        mSpineItems = spineItems;
    }

    private void handleOptionSelection(String optionTitle, String uri) {
        String isbn = mShopItem != null ? mShopItem.book.isbn : mBook.isbn;

        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_MENU, optionTitle, isbn, null);

        if (uri.endsWith(BBBUriManager.BBBUri.SHOP.stringValue)) {
            Intent intent;

            if (mBook.sample_book) {
                intent = new Intent(this, SearchActivity.class);
                intent.putExtra(SearchActivity.ARG_VIEW_TYPE, SearchActivity.ViewType.BOOK_DETAIL);
                intent.putExtra(SearchActivity.ARG_ISBN, mBook.isbn);
            } else {
                intent = new Intent(this, ShopActivity.class);
            }

            startActivity(intent);
        } else {
            BBBUriManager.getInstance().handleUri(this, uri);
            boolean toggle = false;
            Uri u = Uri.parse(uri);
            String lastPath = u.getLastPathSegment();
            if (lastPath != null && lastPath.equalsIgnoreCase(ACTION_SETTINGS)) {
                toggle = true;
            }
            if (toggle) {
                toggleReaderOverlayVisiblity();
            }
        }
    }

    @Override
    public Loader<BookItem> onCreateLoader(int id, Bundle args) {

        if (mBookUri != null) {
            return new BookFileLoader(this, mBookUri);
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<BookItem> loader, final BookItem bookItem) {

        if (bookItem == null || bookItem.bbbePubBook == null || bookItem.book == null) {
            new Handler().post(new Runnable() {
                public void run() {
                    displayErrorLoadingBook(bookItem);
                }
            });
            return;
        }

        mEPubBook = bookItem.bbbePubBook;
        mBook = bookItem.book;
        mLastPosition = bookItem.lastPosition;

        mBookmarks = bookItem.bookmarks;
        mHighlights = bookItem.highlights;
        displayBookContent();
    }

    private void displayErrorLoadingBook(final BookItem bookItem) {

        // If we have a valid book then the we should be able to recover by re-downloading the book so we display a special error message
        // to prompt the user to re-download.
        final boolean displayRedownloadDialog = (bookItem != null && bookItem.book != null);
        String errorMessage = displayRedownloadDialog ? getString(R.string.error_book_open_redownload) : getString(R.string.error_book_open);

        final GenericDialogFragment dialogFragment = GenericDialogFragment.newInstance(getString(R.string.error_book_open_title), errorMessage,
                getString(R.string.ok), null, null, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        // If we are displaying the re-download dialog then we have additional handling to delete the existing physical book
                        // and attempt to re-download it before closing the reader.
                        if (displayRedownloadDialog) {
                            // Clear the book and try to re-download it
                            bookItem.book.download_status = Books.NOT_DOWNLOADED;
                            BookHelper.deletePhysicalBook(bookItem.book);
                            BookHelper.updateBook(mBookUri, bookItem.book, true);
                            BookDownloadController.getInstance(ReaderActivity.this).startDownloadBook(bookItem.book);
                        }
                        finish();
                    }
                }, null, null, null);

        showDialog(dialogFragment, TAG_DIALOG, false);
    }

    @Override
    public void onLoaderReset(Loader<BookItem> BookItem) {
    }

    @Override
    public boolean textSelected(String text, MotionEvent lastTouch) {
        if (mHighlightPopup.getVisibility() != View.VISIBLE) {
            showHighlightPopup(null);
        }

        // Make the word lower case and trim any whitespace before showing the dictionary for it
        String word = text.trim().toLowerCase(Locale.US);

        // On Android 4.1.1 we can observe multiple text selected call backs. To avoid unwanted side effects we
        // check that the dictionary word has changed and just ignore the callback if not.
        if (word.length() > 0 && !word.equals(mDictionaryWord)) {
            showDictionary(word, lastTouch);
            mDictionaryWord = word;
            return true;
        } else {
            return false;
        }
    }

    private void hideHighlightPopup() {
        if (DISABLE_HIGHLIGHTS) {
            return;
        }
        final float animateTo = mHighlightPopup.getY() + (mShowHighlightBelow ? HIGHLIGHT_MENU_ANIMATE_Y : 0 - HIGHLIGHT_MENU_ANIMATE_Y);
        mHighlightPopup.animate().alpha(0.0f).translationY(animateTo).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mHighlightPopup.animate().setListener(null);
                mHighlightPopup.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void showHighlightPopup(final CFI highlight) {
        if (DISABLE_HIGHLIGHTS) {
            return;
        }
        mHighlightPopup.setX(mTouchDownX - (mHighlightPopup.getWidth() / 2));

        final float animateFrom;
        final float animateTo;

        mShowHighlightBelow = mTouchDownY - (1.5f * mHighlightPopup.getHeight()) < 0;

        if (mShowHighlightBelow) {
            //we show the tooltip at the bottom
            animateTo = mTouchDownY + (0.5f * mHighlightPopup.getHeight());
            animateFrom = animateTo + HIGHLIGHT_MENU_ANIMATE_Y;
        } else {
            //we show the tooltip at the top
            animateTo = mTouchDownY - (1.5f * mHighlightPopup.getHeight());
            animateFrom = animateTo - HIGHLIGHT_MENU_ANIMATE_Y;
        }

        mHighlightPopup.setY(animateFrom);

        mHighlightPopup.setVisibility(View.VISIBLE);
        mHighlightPopup.animate().alpha(1.0f).y(animateTo).setListener(null);

        final boolean editingHighlight = highlight != null;
        mHighlightPopupItemHighlight.setVisibility(editingHighlight ? View.GONE : View.VISIBLE);
        mHighlightPopupItemDelete.setVisibility(editingHighlight ? View.VISIBLE : View.GONE);

        if (editingHighlight) {
            mHighlightPopupItemDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReaderFragment.removeHighlight(highlight.CFI);
                    hideHighlightPopup();
                }
            });
        } else {
            mHighlightPopupItemHighlight.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mReaderFragment.highlightSelection(mDictionaryWord);
                    hideHighlightPopup();
                }
            });
        }
    }

    @Override
    public boolean highlightSelected(CFI highlight, MotionEvent lastTouch) {
        if (DISABLE_HIGHLIGHTS) {
            return true;
        }
        if (mHeaderView.getVisibility() != View.VISIBLE) {
            showHighlightPopup(highlight);
        } else {
            setReaderOverlayVisibility(false);
        }
        return true;
    }

    @Override
    public void actionModeCancelled() {
        if (mDictionaryDialog != null && !hasSavedInstanceState()) {
            mDictionaryDialog.dismissAllowingStateLoss();
            mDictionaryDialog = null;
        }
        hideHighlightPopup();
        mDictionaryWord = "";
        mReaderFragment.setInteractionAllowed(true);
    }

    @Override
    public void actionModeCreated() {
        setFullscreen(true);

        // If we are displaying the overlay (we have the header fragment present), then we need to
        // get rid of it before it messes with the native action mode control positioning.
        if (mHeaderView.getVisibility() == View.VISIBLE) {
            toggleReaderOverlayVisiblity();
        }
    }

    @Override
    public void dictionaryDismissed() {
        mReaderFragment.hideTextSelector();
        mDictionaryDialog = null;
        mDictionaryWord = "";
        mReaderFragment.setInteractionAllowed(true);
    }

    private static class InnerHandler extends Handler {
        private final WeakReference<ReaderActivity> mActivity;

        public InnerHandler(ReaderActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            ReaderActivity activity = mActivity.get();
            if (activity != null && msg.what == MSG_SHOW_HELP_OVERLAY) {
                activity.showReaderHelpDialog(true);
            }
        }
    }

    private final BBBBasicResponseHandler<BBBBookmarkList> mBookMarkListHandler = new BBBBasicResponseHandler<BBBBookmarkList>() {

        public void receivedData(BBBResponse response, BBBBookmarkList list) {

            if (list.bookmarks != null && list.bookmarks.length > 0) {
                final Bookmark lastReadingPositionBookmark = BookmarkHelper.createBookmark(list.bookmarks[0]);

                final String clientId = BBBTextUtils.getIdFromGuid(AccountController.getInstance().getClientId());

                //show the message if the last reading position was updated by another device and the current and remote reading positions are at different CFIs
                if (!TextUtils.equals(clientId, lastReadingPositionBookmark.update_by) && (mLastPosition == null || !TextUtils.equals(mLastPosition.position, lastReadingPositionBookmark.position))) {
                    mExternalLastPosition = lastReadingPositionBookmark;
                    BBBRequest request = BBBRequestFactory.getInstance().createGetClientsRequest();
                    BBBRequestManager.getInstance().executeRequest(RESPONSE_HANDLER_CLIENT_NAME, request);
                }
            }
        }

        public void receivedError(BBBResponse response) {
        }
    };

    private final BBBBasicResponseHandler<BBBClientInformationList> mClientListHandler = new BBBBasicResponseHandler<BBBClientInformationList>() {

        public void receivedData(BBBResponse response, BBBClientInformationList data) {

            if (mExternalLastPosition == null || !isInForeground() || isDestroyed()) {
                return;
            }

            String clientName = getString(R.string.a_different_device);
            if (mExternalLastPosition.update_by != null) {
                for (BBBClientInformation client : data.clients) {
                    String clientId = BBBTextUtils.getIdFromGuid(client.client_id);
                    if (mExternalLastPosition.update_by.equals(clientId)) {
                        clientName = client.client_name;
                        break;
                    }
                }
            }

            int percent = 0;
            if (mLastPosition != null) {
                percent = mLastPosition.percentage;
            }

            String message = getString(R.string.dialog_your_last_read_location);
            String time = BBBCalendarUtil.formatTime(mExternalLastPosition.update_date, BBBCalendarUtil.DATE_FORMAT_TIME);
            String date = BBBCalendarUtil.formatDate(mExternalLastPosition.update_date);
            message = String.format(message, percent, mExternalLastPosition.percentage, clientName, time, date);

            BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(ReaderActivity.this);
            builder.setTitle(R.string.title_choose_your_reading_position);
            builder.setMessage(message);
            builder.setNegativeButton(R.string.button_stay_where_i_am, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    mNewPositionAlert = null;
                }
            });

            // Take a copy of the external last position at this point as the mExternalLastPosition value could potentially be updated while
            // the dialog is displaying therefore we shouldn't reference it directly in click handler.
            final Bookmark lastPositionCopy = mExternalLastPosition;

            builder.setPositiveButton(R.string.button_go_to_new_position, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    mLastPosition = lastPositionCopy;
                    mLastPosition.update_by = BBBTextUtils.getIdFromGuid(AccountController.getInstance().getClientId());
                    mReaderFragment.goToBookmark(lastPositionCopy, false);
                    mNewPositionAlert = null;
                }
            });

            mNewPositionAlert = builder.show();
        }

        public void receivedError(BBBResponse response) {
        }
    };

    @Override
    public void onBackPressed() {
        // Give the fragment a chance to handle the back key if it needs to
        if (mReaderFragment == null || !mReaderFragment.handleBackPressed()) {
            // If the fragment has not consumed the back key then fall back to the standard back key handling
            super.onBackPressed();
        }
    }

    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
