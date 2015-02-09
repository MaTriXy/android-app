// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;

import com.blinkboxbooks.android.ui.reader.TextSelectionListener;
import com.blinkboxbooks.android.util.LogUtils;

/**
 * Customised WebView used for displaying a book
 */
public class EPub2ReaderWebView extends WebView {

    private static final String TAG = EPub2ReaderWebView.class.getSimpleName();

    // The delay before we restore touch functionality when exiting action mode.
    private static final int RESTORE_TOUCH_DELAY_MS = 500;

    // The delay before invoking the action mode workaround check
    private static final int ACTION_MODE_WORKAROUND_DELAY_MS = 100;

    private ActionMode.Callback mSelectActionModeCallback;

    private ActionMode mActionMode;

    private TextSelectionListener mTextSelectionListener;

    private MotionEvent mLastTouch;

    private boolean mInterceptTouches = false;
    private boolean mIgnoreAllTouches = false;

    public EPub2ReaderWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    public EPub2ReaderWebView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public EPub2ReaderWebView(Context context) {
        super(context);
        init();
    }

    private GestureDetectorCompat mDetector;

    @SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    private void init() {
        setClickable(false);
        setHorizontalScrollBarEnabled(false);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(View.OVER_SCROLL_NEVER);

        setFocusable(false);
        setFocusableInTouchMode(false);

        if (isInEditMode()) {
            return;
        }

        WebSettings webSettings = getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webSettings.setPluginState(WebSettings.PluginState.ON);
        webSettings.setBuiltInZoomControls(false);

        mDetector = new GestureDetectorCompat(getContext(), new MyGestureListener());
    }

    public MotionEvent getLastTouch() {
        return mLastTouch;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mInterceptTouches;
    }

    // Simple gesture detector that looks out for single taps and long presses and closes the action mode
    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            closeActionMode();
            // Return false to allow the tap event through to the webview. Weird side effects can happen
            // if we don't allow this.
            return false;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            closeActionMode();
            // Double taps can cause a text selected callback on Android 4.1, therefore we return true here
            // so that the motionevent will be swallowed and not passed through
            return true;
        }

        @Override
        public void onLongPress(MotionEvent event) {
            closeActionMode();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        if (mIgnoreAllTouches) {
            return true;
        }
        mLastTouch = ev;

        if (mInterceptTouches) {

            // Pass the event to the detector. If it returns true we swallow the event at this point
            if (mDetector.onTouchEvent(ev)) {
                return true;
            }

            // In KITKAT and above we can just consume the touch event and the text selection handles still function
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                return true;
            }
        }

        // Allow the touch event to go through
        return super.onTouchEvent(ev);
    }

    public void setTextSelectionListener(TextSelectionListener mTextSelectionListener) {
        this.mTextSelectionListener = mTextSelectionListener;
    }

    public void callJSFunction(String javaScript) {
        LogUtils.d(EPub2ReaderJSHelper.TAG, "Javascript: " + javaScript);

        // Use evaluateJavascript in kitkat or the content will be replaced by the javascript return value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            loadUrlApi19(javaScript);
        } else {
            loadUrl(javaScript);
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void loadUrlApi19(String javascript) {
        evaluateJavascript(javascript, null);
    }

    /**
     * Close the action mode if it's currently set.
     */
    public void closeActionMode() {

        // Post to the message queue so this all happens in the UI thread
        post(new Runnable() {
            @Override
            public void run() {
                if(mActionMode != null) {
                    mActionMode.finish();
                    mActionMode = null;
                    mIgnoreAllTouches = true;
                }
            }
        });

        // We introduce a small delay before reinstating touch functionality. This is done to prevent a rare crash
        // that can happen (in the native action bar code) if the user swipes while the native action mode is
        // in the process of dismissing. The delay value is chosen as it seems to prevent the crash across a range
        // of devices without delaying any useful user interaction.
        postDelayed(new Runnable() {
            @Override
            public void run() {
                mIgnoreAllTouches = false;
            }
        }, RESTORE_TOUCH_DELAY_MS);
    }


    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        ViewParent parent = getParent();

        if (parent == null) {
            return null;
        }

        String name = callback.getClass().toString();
        if (name.contains("SelectActionModeCallback")) {
            mSelectActionModeCallback = callback;
        }

        return parent.startActionModeForChild(this, mActionModeCallback);
    }

    /**
     * Set the view to intercept or stop intercepting touches.
     * @param intercept set to true to intercept touches
     */
    public void setInterceptTouches(boolean intercept) {
        mInterceptTouches = intercept;
    }

    /**
     * This is a workaround for a bug seen on Android 4.4. If we select text quickly after changing page
     * sometimes the action mode will not be set but we still have the dictionary displayed. By calling
     * this method we can force the action mode to start so we don't get into a broken state.
     */
    public void startActionModeIfNotActive() {
        // At first invocation it is possible for this call to be invoked before the action mode callback has
        // been invoked, which can cause multiple action mode invocations. The side effect of this can be that the
        // action mode is dismissed when double invoked. The small delay on posting is a workaround for this.
        // While such sleeps are always undesirable we are battling against system level invocations that we have
        // no direct control over, and since this is a workaround for another issue this delay is the best compromise
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mActionMode == null) {
                    startActionMode(mActionModeCallback);
                }
            }
        }, ACTION_MODE_WORKAROUND_DELAY_MS);
    }

    private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mActionMode = mode;

            if (mTextSelectionListener != null) {
                mTextSelectionListener.actionModeCreated();
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                default:
                    if (mActionMode != null) {
                        mActionMode.finish();
                        mActionMode = null;
                    }
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearFocus();
            mActionMode = null;

            if(mTextSelectionListener != null) {
                mTextSelectionListener.actionModeCancelled();
            }

            if(mSelectActionModeCallback != null) {
                mSelectActionModeCallback.onDestroyActionMode(mode);
            }
        }
    };
}