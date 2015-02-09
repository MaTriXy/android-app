// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.ui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.BBBUriManager;
import com.blinkboxbooks.android.util.BBBUriManager.BBBUri;

/*-
 * Terms and Conditions 16.1.0.0
 */
public class TermsAndConditionsActivity extends BaseActivity {

    private WebView mWebViewPolicy;
    private LinearLayout mLinearLayoutProgress;

    private String mUrl;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms_and_conditions);
        getSupportActionBar().setTitle(R.string.title_terms_and_conditions);
        getSupportActionBar().setDisplayShowTitleEnabled(true);

        mUrl = BBBUriManager.getInstance().getUri(BBBUri.TERMSANDCONDITIONS.stringValue).toString();
        mWebViewPolicy = (WebView) findViewById(R.id.webview_policy);
        mLinearLayoutProgress = (LinearLayout) findViewById(R.id.layout_progress);
        mWebViewPolicy.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        mWebViewPolicy.setWebViewClient(new LoadingWebViewClient());
        mWebViewPolicy.loadUrl(mUrl);
        mWebViewPolicy.getSettings().setJavaScriptEnabled(true);
        mWebViewPolicy.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
    }

    private class LoadingWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            mLinearLayoutProgress.setVisibility(View.VISIBLE);
            mWebViewPolicy.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            mLinearLayoutProgress.setVisibility(View.GONE);
            mWebViewPolicy.setVisibility(View.VISIBLE);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (mUrl.equals(url)) {
                return super.shouldOverrideUrlLoading(view, url);
            }
            BBBUriManager.getInstance().handleUri(TermsAndConditionsActivity.this, url);
            return true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
