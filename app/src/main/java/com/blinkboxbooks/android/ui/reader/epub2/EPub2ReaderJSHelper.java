// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkbox.java.book.model.BBBEPubContent;
import com.blinkboxbooks.android.BuildConfig;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.ReaderSetting;
import com.blinkboxbooks.android.model.reader.Event;
import com.blinkboxbooks.android.model.reader.HighlightAddedEvent;
import com.blinkboxbooks.android.model.reader.HighlightClickedEvent;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.ReaderSettingListener;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2SettingsChangedListener;
import com.blinkboxbooks.android.util.DebugUtils;
import com.blinkboxbooks.android.util.IOUtils;
import com.blinkboxbooks.android.util.LogUtils;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * Helper class for interacting with JS library
 */
public class EPub2ReaderJSHelper implements EPub2SettingsChangedListener {

    private static final String BOOK_SERVER_URL = "http://127.0.0.1";

    private static final String PARAM_CALLBACK = "/BBBCALLBACK";

    public static final String MIME_TYPE_HTML = "text/html";
    public static final String MIME_TYPE_JSON = "application/json";

    public static final String TAG = EPub2ReaderJSHelper.class.getSimpleName();

    private static final String BASE_HTML_FILE = "readerjs/reader.html";

    private static final String JS_GET_CFI = "javascript:READER.getCFI();";
    private static final String JS_NEXT_PAGE = "javascript:READER.next();";
    private static final String JS_PREV_PAGE = "javascript:READER.prev();";
    private static final String JS_GO_TO_CFI = "javascript:READER.goToCFI('%s');";
    private static final String JS_LOAD_CHAPTER = "javascript:READER.loadChapter('%s');";
    private static final String JS_SET_FONT_FAMILY = "javascript:READER.setFontFamily('%s');";
    private static final String JS_SET_THEME = "javascript:READER.setTheme('%s');";
    private static final String JS_SET_FONT_SIZE = "javascript:READER.setFontSize('%f');";
    private static final String JS_SET_LINE_HEIGHT = "javascript:READER.setLineHeight('%f');";
    private static final String JS_SET_TEXT_ALIGNMENT = "javascript:READER.setTextAlign('%s');";
    private static final String JS_SET_MARGIN = "javascript:READER.setMargin([%f,%f,%f,%f]);";
    private static final String JS_SET_PREFERENCES = "javascript:READER.setPreferences({ margin: [%f,%f,%f,%f], lineHeight: %f, fontSize: %f, textAlign: '%s', fontFamily: '%s', theme: '%s', publisherStyles: '%s', maxParallelRequests: 1})";
    private static final String JS_INIT = "javascript:READER.init({container:'%s', width: %.2f, height: %.2f, padding: %d, url: '%s', mobile:true, %s, isbn: %s, listener: function(e){ interface.event(JSON.stringify(e)); } });";
    private static final String JS_INIT_CFI = "initCFI: '%s'";
    private static final String JS_COLUMNS = "columns: %d";
    private static final String JS_BOOKMARKS = "bookmarks: [%s]";
    private static final String JS_HIGHLIGHTS = "highlights: [%s]";
    private static final String JS_PREFERENCES = "preferences: { margin: [%f,%f,%f,%f], lineHeight: %f, fontSize: %f, textAlign: '%s', fontFamily: '%s', theme: '%s', publisherStyles: %s, maxParallelRequests: 1}";
    private static final String JS_HIDE_HEADER = "javascript:READER.hideHeaderAndFooter();";
    private static final String JS_SHOW_HEADER = "javascript:READER.showHeaderAndFooter();";
    private static final String JS_SET_BOOKMARK = "javascript:READER.setBookmark();";
    private static final String JS_REMOVE_BOOKMARK = "javascript:READER.removeBookmark('%s');";
    private static final String JS_SET_BOOKMARKS = "javascript:READER.setBookmarks([%s]);";
    private static final String JS_SET_HIGHLIGHT = "javascript:READER.setHighlight();";
    private static final String JS_REMOVE_HIGHLIGHT = "javascript:READER.removeHighlight('%s');";
    private static final String JS_SET_HIGHLIGHTS = "javascript:READER.setHighlights([%s]);";
    private static final String JS_ENABLE_DEBUG = "javascript:READER.enableDebug();";
    private static final String JS_ENABLE_PUBLISHER_STYLES = "javascript:READER.setPreferences({ publisherStyles: %s });";
    private static final String JS_GO_TO_PROGRESS = "javascript:READER.goToProgress(%.10f);";

    private final EPub2ReaderWebView mWebView;

    private View mErrorView;

    private boolean mReaderPreview = false;

    private Bookmark mLastPosition;
    private String mBaseUrl;
    private String mInitUrl;
    private String mBookmarks;
    private String mHighlights;
    private boolean mIsOnlineSample;
    private int mColumnCount = 1;

    private String mISBN;
    private BBBEPubBook mBook;

    private EPub2ReaderJSCallbacks mEPub2ReaderJSCallbacks;

    private boolean mInitialising = false;

    private int mColumnPadding;

    @SuppressLint("AddJavascriptInterface")
    public EPub2ReaderJSHelper(EPub2ReaderWebView webView, EPub2ReaderJSCallbacks ePub2ReaderJSCallbacks) {
        this.mWebView = webView;
        mEPub2ReaderJSCallbacks = ePub2ReaderJSCallbacks;

        mWebView.setWebViewClient(readerWebViewClient);
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                LogUtils.i(TAG, "onConsoleMessage " + cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
                return true;
            }


        });

        mWebView.addJavascriptInterface(new JavaScriptInterface(), "interface");
    }

    public void setErrorViewVisible(boolean error) {

        if (mErrorView != null) {
            mWebView.setVisibility(error ? View.GONE : View.VISIBLE);
            mErrorView.setVisibility(error ? View.VISIBLE : View.GONE);
        }
    }

    public void setErrorView(View errorView) {
        mErrorView = errorView;
    }

    public void setPreview(String url, Bookmark lastPosition) {
        LogUtils.i(TAG, "setting preview " + url);

        mBaseUrl = BOOK_SERVER_URL;
        mInitUrl = url;
        mReaderPreview = true;
        mBook = null;
        mLastPosition = lastPosition;
        initialise();
    }

    /**
     * Sets the book and navigates to the reading position as indicated in the lastPosition
     */
    public void setBook(String isbn, BBBEPubBook book, Bookmark bookmark, String bookmarks, String highlights, boolean isOnlineSample) {
        mISBN = isbn;
        mBook = book;
        mReaderPreview = false;
        mLastPosition = bookmark;
        mBookmarks = bookmarks;
        mHighlights = highlights;
        mIsOnlineSample = isOnlineSample;
        mInitUrl = BOOK_SERVER_URL;
        mBaseUrl = mInitUrl;
        initialise();
    }

    private void initialise() {

        if(mInitialising) {
            return;
        }

        mInitialising = true;

        if (mReaderPreview) {
            mColumnCount = 1;
        } else {
            mColumnCount = mWebView.getContext().getResources().getInteger(R.integer.reader_column_count);
        }

        try {
            String html = IOUtils.toString(mWebView.getContext().getResources().getAssets().open(BASE_HTML_FILE));
            LogUtils.i(TAG, "load data with base url " + mBaseUrl);
            mWebView.loadDataWithBaseURL(mBaseUrl, html, MIME_TYPE_HTML, "UTF-8", null);
        } catch (IOException e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
    }

    /**
     * Initialise the javascript library.
     */
    private void initJS() {

        if (mReaderPreview) {
            final String account = AccountController.getInstance().getUserId();
            ReaderSetting readerSetting = EPub2ReaderSettingController.getInstance().getDefaultSettings(account);
            readerSetting.font_size = EPub2ReaderSettingController.READER_SETTING_DEFAULT_FONT_SIZE_PREVIEW;
            initJS(readerSetting);
        } else {
            EPub2ReaderSettingController.getInstance().loadReaderSetting(new ReaderSettingListener() {
                @Override
                public void onReaderSettingLoaded(ReaderSetting readerSetting) {
                    initJS(readerSetting);
                }
            });
        }
    }

    private void initJS(ReaderSetting readerSetting) {
        float density = mWebView.getContext().getResources().getDisplayMetrics().density;
        float width = (float) mWebView.getWidth() / density;
        float height = (float) mWebView.getHeight() / density;

        float lineSpace = readerSetting.line_space;
        float fontSize = readerSetting.font_size;

        mColumnPadding = mWebView.getContext().getResources().getDimensionPixelSize(R.dimen.gap_small);

        String container = "#reader";
        String theme = EPub2ReaderSettingController.getReaderTheme(readerSetting.background_color);

        float leftMargin = readerSetting.margin_left / mColumnCount;
        float rightMargin = readerSetting.margin_right / mColumnCount;

        leftMargin = leftMargin - mColumnPadding < 0 ? 0 : leftMargin - mColumnPadding;
        rightMargin = rightMargin - mColumnPadding < 0 ? 0 : rightMargin - mColumnPadding;

        String preferences = String.format(Locale.US, JS_PREFERENCES, readerSetting.margin_top, leftMargin, readerSetting.margin_bottom, rightMargin,
                lineSpace, fontSize, readerSetting.text_align, readerSetting.font_typeface, theme, String.valueOf(readerSetting.publisher_styles));

        StringBuilder arguments = new StringBuilder(preferences);
        if (mLastPosition != null && !TextUtils.isEmpty(mLastPosition.position)) {
            arguments.append(", ");
            arguments.append(String.format(Locale.US, JS_INIT_CFI, mLastPosition.position));
            arguments.append(", ");
            arguments.append(String.format(Locale.US, JS_BOOKMARKS, mBookmarks));
            arguments.append(", ");
            arguments.append(String.format(Locale.US, JS_HIGHLIGHTS, mHighlights));
        }

        if (mColumnCount > 1) {
            container = "#reader";
            arguments.append(", ");
            arguments.append(String.format(Locale.US, JS_COLUMNS, mColumnCount));
        }

        LogUtils.v(TAG, String.format("Reader: (%d,%d)(%.2f,%.2f); padding: %d", mWebView.getWidth(), mWebView.getHeight(), width, height, mColumnPadding));
        String javaScript = String.format(Locale.US, JS_INIT, container, width, height, mColumnPadding, mInitUrl, arguments.toString(), mISBN);

        DebugUtils.setJavascriptInit(javaScript);

        mWebView.callJSFunction(javaScript);

        if (BuildConfig.DEBUG) {
            mWebView.callJSFunction(JS_ENABLE_DEBUG);
        }
    }

    private final WebViewClient readerWebViewClient = new WebViewClient() {



        @Override
        public void onPageFinished(final WebView webView, String url) {
            LogUtils.d(TAG, "on page finished: "+url);

            mInitialising = false;
            initJS();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);

            LogUtils.d(TAG, "on received error: "+errorCode+" "+description+" "+failingUrl);

            setErrorViewVisible(true);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            LogUtils.d(TAG, "should intercept: "+url);

            if(mBook == null) {
                return super.shouldInterceptRequest(view, url);
            }

            if(url.startsWith(BOOK_SERVER_URL)) {
                LogUtils.d(TAG, "intercepting: "+url);

                String path = url.substring(BOOK_SERVER_URL.length());

                try {
                    path = URLDecoder.decode(path, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    return null;
                }

                if(path.startsWith(PARAM_CALLBACK)) {
                    return null;
                }

                BBBEPubContent content = mBook.getContentAtUrl(path);

                if(content == null) {
                    return super.shouldInterceptRequest(view, url);
                }

                String mimeType = content.getMimeType();

                if (path.endsWith("book-info.json")) {
                    mimeType = MIME_TYPE_JSON;
                }

                InputStream zipInputStream = content.getData();

                return new WebResourceResponse(mimeType, content.getEncoding(), zipInputStream);
            }

            return super.shouldInterceptRequest(view, url);
        }
    };

    private void handleCallback(String parameters) {
        LogUtils.d(TAG, "handling callback: " + parameters);

        Event event = parseEventString(parameters);

        if (event != null && mEPub2ReaderJSCallbacks != null) {
            mEPub2ReaderJSCallbacks.event(event);
        } else {
            LogUtils.d(TAG, "No handler for cross platform library callback " + parameters);
        }
    }

    private Event parseEventString(String str) {

        if (str == null) {
            return null;
        }

        try {
            final JSONObject jsonObject = new JSONObject(str);

            final Event event;

            final int code = jsonObject.getInt("code");

            switch (code) {
                case Event.EVENT_HIGHLIGHT_ADDED:
                    HighlightAddedEvent highlightAddedEvent = new Gson().fromJson(str,HighlightAddedEvent.class);
                    event = highlightAddedEvent.createEvent();
                    break;
                case Event.EVENT_HIGHLIGHT_CLICKED:
                    HighlightClickedEvent highlightClickedEvent = new Gson().fromJson(str,HighlightClickedEvent.class);
                    event = highlightClickedEvent.createEvent();
                    break;
                default:
                    event = new Gson().fromJson(str, Event.class);
            }

            return event;
        } catch (JSONException e) {
            LogUtils.e(TAG, "error parsing event string: " + str);
        }

        return null;
    }

    /**
     * Gets the CFI of the current page
     */
    public void getCFI() {
        mWebView.callJSFunction(JS_GET_CFI);
    }

    /**
     * Navigates to the next page
     */
    public void nextPage() {
        mWebView.callJSFunction(JS_NEXT_PAGE);
    }

    /**
     * Navigates to the previous page
     */
    public void prevPage() {
        mWebView.callJSFunction(JS_PREV_PAGE);
    }

    /**
     * Go to the page referenced by the cfi
     */
    public void goToCFI(String cfi) {
        String function = String.format(Locale.US, JS_GO_TO_CFI, cfi);
        mWebView.callJSFunction(function);
    }

    /**
     * Navigates to the specified percentage progress
     * @param progress
     */
    @SuppressLint("DefaultLocale")
    public void goToProgress(float progress) {
        String function = String.format(JS_GO_TO_PROGRESS, progress);
        mWebView.callJSFunction(function);
    }

    /**
     * Go to the page referenced by the url
     */
    public void goToUrl(String url) {
        String function = String.format(Locale.US, JS_LOAD_CHAPTER, url);
        mWebView.callJSFunction(function);
    }

    @Override
    public void onSetTypeFace(String font_typeface) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_FONT_FAMILY, font_typeface));
    }

    @Override
    public void onSetReaderTheme(String theme) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_THEME, theme));
    }

    @Override
    public void onSetFontSize(float em) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_FONT_SIZE, em));
    }

    @Override
    public void onSetLineHeight(float em) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_LINE_HEIGHT, em));
    }

    @Override
    public void onSetTextAlignment(String alignment) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_TEXT_ALIGNMENT, alignment));
    }

    @Override
    public void onSetMargin(float top, float left, float bottom, float right) {
        left = left / mColumnCount;
        right = right / mColumnCount;

        left = left - mColumnPadding < 0 ? 0 : left - mColumnPadding;
        right = right - mColumnPadding < 0 ? 0 : right - mColumnPadding;

        mWebView.callJSFunction(String.format(Locale.US, JS_SET_MARGIN, top, left, bottom, right));
    }

    @Override
    public void onSetPreferences(String theme, float fontSize, String fontTypeface, float lineHeight, String textAlignment, float topMargin, float leftMargin, float bottomMargin, float rightMargin, boolean publisherStyles) {
        leftMargin = leftMargin / mColumnCount;
        rightMargin = rightMargin / mColumnCount;

        leftMargin = leftMargin - mColumnPadding < 0 ? 0 : leftMargin - mColumnPadding;
        rightMargin = rightMargin - mColumnPadding < 0 ? 0 : rightMargin - mColumnPadding;

        String function = String.format(Locale.US, JS_SET_PREFERENCES, topMargin, leftMargin, bottomMargin, rightMargin,
                lineHeight, fontSize, textAlignment, fontTypeface, theme, String.valueOf(publisherStyles));

        mWebView.callJSFunction(function);
    }

    @Override
    public void onSetPublisherStylesEnabled(boolean enabled) {
        mWebView.callJSFunction(String.format(JS_ENABLE_PUBLISHER_STYLES, String.valueOf(enabled)));
    }

    @Override
    public void setHeaderVisibility(boolean visible) {

        if (visible) {
            mWebView.callJSFunction(JS_SHOW_HEADER);
        } else {
            mWebView.callJSFunction(JS_HIDE_HEADER);
        }
    }

    /**
     * Add a bookmark at the current CFI
     */
    public void setBookmark() {
        mWebView.callJSFunction(JS_SET_BOOKMARK);
    }

    /**
     * Remove a bookmark in the js library
     *
     * @param cfi
     */
    public void removeBookmark(String cfi) {
        mWebView.callJSFunction(String.format(Locale.US, JS_REMOVE_BOOKMARK, cfi));
    }

    /**
     * Set an array of bookmarks in the js library
     *
     * @param bookmarkArray
     */
    public void setBookmarks(String bookmarkArray) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_BOOKMARKS, bookmarkArray));
    }

    /**
     * Add a highlight at the current CFI
     */
    public void setHighlight() {
        mWebView.callJSFunction(JS_SET_HIGHLIGHT);
    }

    /**
     * Remove a highlight in the js library
     *
     * @param cfi
     */
    public void removeHighlight(String cfi) {
        mWebView.callJSFunction(String.format(Locale.US, JS_REMOVE_HIGHLIGHT, cfi));
    }

    /**
     * Set an array of highlights in the js library
     *
     * @param highlightArray
     */
    public void setHighlights(String highlightArray) {
        mWebView.callJSFunction(String.format(Locale.US, JS_SET_HIGHLIGHTS, highlightArray));
    }

    public class JavaScriptInterface {

        @JavascriptInterface
        public void event(String str) {
            handleCallback(str);
        }
    }
}