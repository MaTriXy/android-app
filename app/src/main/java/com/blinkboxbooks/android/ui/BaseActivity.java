// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.http.HttpResponseCache;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MotionEvent;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.dialog.AlertDialogFragment;
import com.blinkboxbooks.android.dialog.ForceUpgradeDialogFragment;
import com.blinkboxbooks.android.dialog.GenericDialogFragment;
import com.blinkboxbooks.android.dialog.ProgressDialogFragment;
import com.blinkboxbooks.android.model.Library;
import com.blinkboxbooks.android.model.helper.LibraryHelper;
import com.blinkboxbooks.android.util.AnalyticsHelper;
import com.blinkboxbooks.android.util.LogUtils;

/**
 * The base activity for all blinkbox books activities. Used to detect whether the app is currently in the foreground
 */
@SuppressLint("InflateParams")
public abstract class BaseActivity extends ActionBarActivity {

    public static final String ACTION_CLIENT_LIMIT_EXCEEDED = "action_client_limit_exceeded";
    public static final String ACTION_INSUFFICIENT_STORAGE_SPACE = "action_insufficient_storage_space";

    public static final String ACTION_REFRESH = "refresh";
    public static final String ACTION_FORCE_REFRESH = "force_refresh";


    private static final String TAG_PROGRESS_DIALOG = "progress_dialog";
    private static final String TAG_ERROR_DIALOG = "error_dialog";

    private static boolean sIsInBackground = true;
    private static BackgroundHandler sBackgroundHandler = null;

    private ProgressDialogFragment mProgressDialogFragment;

    protected String mScreenName;

    private boolean mIsDestroyed = false;
    private boolean mHasSavedInstanceState = false;
    private boolean mHideProgressOnResume = false;
    /**
     * This is true from the time that commit a fragment transaction & until the backstack has changed.
     * While this is true, we ignore the back button getting pressed & all other touch events, so this
     * should help stop quick double taps by the user that can result in multiple dialogs, or fragment
     * already added crashes.
     */
    private boolean mIsWaitingForDialogToShow = false;
    /**
     * This is true from the time that startActivity is called until onPause on the active activity is called.
     * While this is true, we ignore the back button getting pressed & all other touch events, so this
     * should help stop quick double taps by the user that can result in multiple activities created.
     */
    private boolean mIsWaitingForActivityToShow = false;

    private static class BackgroundHandler extends Handler {

        private final static long BACKGROUND_DELAY = 1000;
        private final static int MESSAGE_BACKGROUND = 0;

        public void setInBackground(boolean background) {

            if (background) {
                sendMessageDelayed(Message.obtain(this, MESSAGE_BACKGROUND), BACKGROUND_DELAY);
            } else {
                removeMessages(MESSAGE_BACKGROUND);
            }
        }

        public void handleMessage(Message msg) {

            if (msg.what == MESSAGE_BACKGROUND) {
                sIsInBackground = true;
                LogUtils.d(getClass().getSimpleName(), "Application went into the background");
            }
        }
    }

    /**
     * Check whether app is in background
     *
     * @return true if app is in background else false
     */
    public static boolean isAppInBackground() {
        return sIsInBackground;
    }

    /**
     * Hides the progress dialog if it is currently showing
     */
    public void hideProgress() {

        // For safety we ensure that the progress dialog is dismissed from the UI thread. If we are already running
        // in the UI thread the code will be run immediately
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (! mHasSavedInstanceState) {
                    ProgressDialogFragment fragment = (ProgressDialogFragment) (getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS_DIALOG));
                    if (fragment != null) {
                        fragment.dismissAllowingStateLoss();
                    }
                } else {
                    mHideProgressOnResume = true;
                }
            }
        });
    }

    /**
     * Shows a progress dialog with the given text. If the dialog is already showing the text is updated
     *
     * @param textResourceId the resource id of the text you want to display
     */
    public void showProgress(final int textResourceId) {
        // For safety we ensure that the progress dialog is shown from the UI thread. If we are already running
        // in the UI thread the code will be run immediately
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!mHasSavedInstanceState && mProgressDialogFragment != null) {
                    mProgressDialogFragment.setMessage(textResourceId);
                }
            }
        });
        hideProgress();

        if (isFinishing()) {
            return;
        }

        // For safety we ensure that the progress dialog is shown from the UI thread. If we are already running
        // in the UI thread the code will be run immediately
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (! mHasSavedInstanceState) {
                    mProgressDialogFragment = ProgressDialogFragment.newInstance(textResourceId);
                    showDialog(mProgressDialogFragment, TAG_PROGRESS_DIALOG, false);
                }
            }
        });
    }

    /**
     * Shows an alert dialog with the given title and message
     *
     * @param title   the title you want to display
     * @param message the message you want to display
     */
    public void showMessage(String title, String message) {
        final AlertDialogFragment alertDialogFragment = AlertDialogFragment.newInstance(title, message);
        showDialog(alertDialogFragment, TAG_ERROR_DIALOG, false);
    }

    private boolean mIsInForeground;

    @Override
    protected void onPause() {
        super.onPause();
        sBackgroundHandler.setInBackground(true);

        mIsInForeground = false;
        mIsWaitingForActivityToShow = false;

        LocalBroadcastManager.getInstance(this).unregisterReceiver(clientLimitExceededBroadCastReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(insufficientSizeBroadcastReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mIsInForeground = true;
        mIsWaitingForActivityToShow = false;
        mIsWaitingForDialogToShow = false;

        sIsInBackground = false;

        if (sBackgroundHandler == null) {
            sBackgroundHandler = new BackgroundHandler();
        }

        sBackgroundHandler.setInBackground(false);

        LocalBroadcastManager.getInstance(this).registerReceiver(clientLimitExceededBroadCastReceiver, new IntentFilter(ACTION_CLIENT_LIMIT_EXCEEDED));
        LocalBroadcastManager.getInstance(this).registerReceiver(insufficientSizeBroadcastReceiver, new IntentFilter(ACTION_INSUFFICIENT_STORAGE_SPACE));

        mHasSavedInstanceState = false;

        // This is a workaround for situations where the hideProgress method is called after the activities onSaveInstanceState
        // method has been called. The mHidProgressOnResume flag gets set so we ensure that the dialog does get hidden to avoid
        // a permanent progress dialog that cannot be hidden.
        if (mHideProgressOnResume) {
            ProgressDialogFragment fragment = (ProgressDialogFragment) (getSupportFragmentManager().findFragmentByTag(TAG_PROGRESS_DIALOG));

            if (fragment != null) {
                fragment.dismissAllowingStateLoss();
            }
            mHideProgressOnResume = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //force portrait mode if app is running on a mobile device
        if (!getResources().getBoolean(R.bool.isTablet)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                mIsWaitingForDialogToShow = false;
            }
        });
    }

    @Override
    public void setContentView(int layoutResID) {
        setContentViewAndToolbarState(layoutResID, true);
    }

    protected void setContentViewAndToolbarState(int layoutResID, boolean showTitle) {
        super.setContentView(layoutResID);
        Toolbar toolBar = (Toolbar) findViewById(R.id.toolbar);
        if (toolBar != null) {
            setSupportActionBar(toolBar);
            final ActionBar supportActionBar = getSupportActionBar();
            supportActionBar.setIcon(R.drawable.actionbar_icon);
            supportActionBar.setDisplayShowTitleEnabled(showTitle);
        }
    }


    @Override
    protected void onDestroy() {
        mIsDestroyed = true;
        super.onDestroy();

        HttpResponseCache cache = HttpResponseCache.getInstalled();

        if (cache != null) {
            cache.flush();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mHasSavedInstanceState = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent != null && intent.getData() != null) {
            String item = intent.getData().getLastPathSegment();

            if (ACTION_REFRESH.equals(item)) {
                onRefresh();
            } else if (ACTION_FORCE_REFRESH.equals(item)) {
                //Explicitly set the bookmark/library sync time to epoch time before synching
                final String userId = AccountController.getInstance().getUserId();
                final Library library = LibraryHelper.getLibraryForUserId(userId);
                final String epochTime =  "1970-01-01T00:00:00Z";
                library.date_library_last_sync = epochTime;
                library.date_bookmark_last_sync = epochTime;

                LibraryHelper.updateLibrary(library);

                onRefresh();
            }
        }
    }

    /**
     * Returns whether this Activity is currently in the foreground
     *
     * @return is the Activity in the foreground
     */
    public boolean isInForeground() {
        return mIsInForeground;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!TextUtils.isEmpty(mScreenName)) {
            AnalyticsHelper.getInstance().startTrackingUIComponent(mScreenName);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!TextUtils.isEmpty(mScreenName)) {
            AnalyticsHelper.getInstance().stopTrackingUIComponent(mScreenName);
        }
    }

    private final BroadcastReceiver insufficientSizeBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Runnable runnable = new Runnable() {
                public void run() {
                    final GenericDialogFragment genericDialogFragment = GenericDialogFragment.newInstance(getString(R.string.title_insufficient_space), getString(R.string.error_insufficient_space_for_book),
                            getString(R.string.ok), null, null, null, null, null, null);
                    showDialog(genericDialogFragment, GenericDialogFragment.TAG_INSUFFICIENT_SPACE, false);
                }
            };

            runOnUiThread(runnable);
        }
    };

    private final BroadcastReceiver clientLimitExceededBroadCastReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            Runnable runnable = new Runnable() {

                public void run() {

                    if (mIsInForeground) {
                        final GenericDialogFragment genericDialogFragment = GenericDialogFragment.newInstance(getString(R.string.title_client_limit_reached), getString(R.string.dialog_client_limit_reached),
                                getString(R.string.button_go_to_your_devices), null, getString(R.string.button_cancel), null, null, null, null);
                        showDialog(genericDialogFragment, GenericDialogFragment.TAG_CLIENT_LIMIT_EXCEEDED, false);
                    }
                }
            };

            runOnUiThread(runnable);
        }
    };

    public void navigateToParent() {
        Intent upIntent = NavUtils.getParentActivityIntent(this);

        if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
            // This activity is NOT part of this app's task, so create a new task
            // when navigating up, with a synthesized back stack.
            TaskStackBuilder.create(this)
                    // Add all of this activity's parents to the back stack
                    .addNextIntentWithParentStack(upIntent)
                            // Navigate up to the closest parent
                    .startActivities();
        } else {
            // This activity is part of this app's task, so simply
            // navigate up to the logical parent activity.
            NavUtils.navigateUpTo(this, upIntent);
            upIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(upIntent);
        }
    }

    /**
     * Handler for when refresh button is clicked.
     */
    public void onRefresh() {
        AccountController.getInstance().requestSynchronisation();
    }


    @Override
    public boolean isDestroyed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            return super.isDestroyed();
        } else {
            return mIsDestroyed;
        }
    }

    /**
     * Checks if this activity has either been destroyed or is currently finishing.
     * @return true if the activity is destroyed or finishing
     */
    public boolean isDestroyedOrFinishing() {
        return isFinishing() || isDestroyed();
    }

    /**
     * Check if the activity has saved its instance state.
     * @return true if the instance state has been saved
     */
    public boolean hasSavedInstanceState() {
        return mHasSavedInstanceState;
    }

    /**
     * Shows the given dialog with the given tag. If there is already a fragment with this tag,
     * or the activity has already saved it's instance state, we ignore this operation.
     * In the time between this and the dialog is shown (as commits are not synchronous,
     * this activity ignores touch events - which prevents double taps from occuring).
     *
     * @param fragment The DialogFragment to show.
     * @param tag The tag to identify the fragment with.
     * @param addToBackStack true if the dialog should be added to the backstack.
     */
    public void showDialog(DialogFragment fragment, String tag, boolean addToBackStack) {
        if (!mHasSavedInstanceState) {
            final FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.findFragmentByTag(tag) == null) {
                final FragmentTransaction transaction = fragmentManager.beginTransaction();
                if (addToBackStack) {
                    transaction.addToBackStack(null);
                }
                fragment.show(transaction, tag);
                //we get no notification of the back stack change if addToBackStack is false
                mIsWaitingForDialogToShow = addToBackStack;
            }
        }
    }

    @Override
    public void startActivity(Intent intent) {
        super.startActivity(intent);
        mIsWaitingForActivityToShow = true;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mIsWaitingForDialogToShow || mIsWaitingForActivityToShow) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        if (!mIsWaitingForDialogToShow && !mIsWaitingForActivityToShow) {
            super.onBackPressed();
        }
    }

    /**
     * Presents a dialog to the user forcing them to either upgrade the app (via button link to store) or close (kill app)
     * @param url
     * @param title
     * @param message
     */
    public void forceUpgradeReminder(String url, String title, String message) {
        if(! (this instanceof SplashActivity)) {
            ForceUpgradeDialogFragment dialogFragment = ForceUpgradeDialogFragment.newInstance(url, title, message);
            showDialog(dialogFragment, "ForceUpgradeReminder", false);
        }
    }
}