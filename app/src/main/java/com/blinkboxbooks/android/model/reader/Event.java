// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.model.reader;

import com.blinkbox.java.book.json.BBBEPubBookInfo;
import com.blinkbox.java.book.json.BBBSpineItem;

/**
 * An event notification given by the cross platfrom library
 */
public class Event {

    public static final int EVENT_END_OF_BOOK = 2;
    public static final int EVENT_LOADING = 5;
    public static final int EVENT_LOADED = 6;
    public static final int EVENT_STATUS = 7;
    public static final int EVENT_START_OF_BOOK = 8;
    public static final int EVENT_ERROR_MISSING_FILE = 9;
    public static final int EVENT_NOTICE_EXT_LINK = 17;
    public static final int EVENT_CONTENT_NOT_AVAILABLE = 18;
    public static final int EVENT_UNHANDLED_TOUCH_EVENT = 19;
    public static final int EVENT_INTERNAL_LINK_CLICKED = 20;
    public static final int EVENT_IMAGE_DOUBLE_CLICKED = 21;
    public static final int EVENT_TEXT_SELECTED = 22;
    public static final int EVENT_HIGHLIGHT_CLICKED = 26;
    public static final int EVENT_HIGHLIGHT_ADDED = 27;

    public static final String CALL_INIT = "init";
    public static final String CALL_SET_BOOKMARK = "setBookmark";
    public static final String CALL_SET_HIGHLIGHT = "setHighlight";
    public static final String CALL_GO_TO_PROGRESS = "goToProgress";
    public static final String CALL_PROGRESS_LOAD = "progressLoad";

    /**
     * the event code
     */
    public int code;

    /**
     * message describing the event
     */
    public String message;
    public String value;
    public String call;
    public int chapter;
    public int chapters;
    public CFI cfi;
    public float progress;

    public String[] bookmarksInPage;
    public String[] highlightsInPage;
    public String preview;
    public String version;
    public int page;
    public int pages;

    // It doesn't make much sense that the X,Y co-ords of the click point can be float but the browser in Android 5.0
    // sometimes gives us floats, so we just use a float instead of an int here
    public float clientX;
    public float clientY;

    public String href;
    public BBBSpineItem[] spine;
    public String src;
    public BBBEPubBookInfo book;
}