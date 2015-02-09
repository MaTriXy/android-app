// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.EmailUtil;
import com.blinkboxbooks.android.util.IOUtils;
import com.blinkboxbooks.android.util.LogUtils;

import java.io.IOException;

/**
 * Activity for showing arbitrary web content. Must supply url or file via Intent.
 */
@SuppressLint("InflateParams")
public class WebContentActivity extends BaseActivity {

    private static final String TAG = WebContentActivity.class.getSimpleName();

    public static String PARAM_FILE = "file";
    public static String PARAM_URL = "url";
    public static String PARAM_FORMAT_ARGS = "format_args"; //can replace strings in a formatted html file
    public static String PARAM_TITLE = "title";
    public static String PARAM_SCREEN_NAME = "screen_name";

    private static final String MAIL_TO = "mailto:";

    private ProgressBar mProgressBar;

    private WebView mWebView;

    private String[] mArgs;
    private String[] mDomainLocks;

    private String mUrl;

    private String mFilePath;

    private boolean mLoadingLocalHtml = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        mUrl = intent.getStringExtra(PARAM_URL);
        mFilePath = intent.getStringExtra(PARAM_FILE);
        mDomainLocks = getResources().getStringArray(R.array.domain_locks);
        mScreenName = intent.getStringExtra(PARAM_SCREEN_NAME);

        if (mUrl == null && mFilePath == null) {
            finish();
            LogUtils.e(TAG, "No url or file specified. Finishing.");
        }

        setContentView(R.layout.activity_web_content);

        mProgressBar = (ProgressBar) findViewById(R.id.progressbar);

        mWebView = (WebView) findViewById(R.id.webview);
        mWebView.setWebViewClient(mWebViewClient);
        enableJavascript(mWebView);

        if (mUrl != null) {
            LogUtils.i(TAG, "Loading url: " + mUrl);
            mWebView.loadUrl(mUrl);
        } else if (mFilePath != null) {
            LogUtils.i(TAG, "Loading file: " + mFilePath);
            mArgs = intent.getStringArrayExtra(PARAM_FORMAT_ARGS);

            loadLocalHtml(mFilePath, mArgs);
        }

        String title = intent.getStringExtra(PARAM_TITLE);

        ActionBar actionBar = getSupportActionBar();

        if (TextUtils.isEmpty(title)) {
            actionBar.hide();
        } else {
            actionBar.setTitle(title);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                finish();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadLocalHtml(String filePath, Object[] args) {
        String html = null;

        mLoadingLocalHtml = true;

        try {
            html = IOUtils.toString(getResources().getAssets().open(mFilePath));

            if (args != null && args.length > 0) {
                html = String.format(html, args);
            }

        } catch (IOException e) {
            finish();
            LogUtils.e(TAG, e.getMessage(), e);
        }

        mWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "utf-8", null);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void enableJavascript(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
    }

    private WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onPageStarted(WebView webView, String url, Bitmap favicon) {
            super.onPageStarted(webView, url, favicon);

            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView webView, String url) {
            super.onPageFinished(webView, url);

            mProgressBar.setVisibility(View.GONE);

            if (mLoadingLocalHtml) {
                mLoadingLocalHtml = false;
                mWebView.clearHistory();
            }
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            if (url.startsWith(MAIL_TO)) {
                EmailUtil.openEmailClient(WebContentActivity.this, url.substring(MAIL_TO.length(), url.length()), getString(R.string.email_contact_us_subject), "", "");
                return true;
            } else if (mDomainLocks != null && arrayContains(mDomainLocks, url)) {
                return super.shouldOverrideUrlLoading(view, url);
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);

                return true;
            }
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {

            if (mFilePath != null && !mWebView.canGoBackOrForward(-2)) {
                loadLocalHtml(mFilePath, mArgs);
            } else {
                mWebView.goBack();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    private boolean arrayContains(String[] strings, String string) {

        if (string == null) {
            return false;
        }

        for (int i = 0; i < strings.length; i++) {

            if (string.contains(strings[i])) {
                return true;
            }
        }

        return false;
    }
}