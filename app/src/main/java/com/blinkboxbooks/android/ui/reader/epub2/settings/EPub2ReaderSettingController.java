// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2.settings;

import android.content.ContentValues;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.TypedValue;
import android.view.WindowManager;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.AccountController;
import com.blinkboxbooks.android.model.ReaderSetting;
import com.blinkboxbooks.android.model.helper.ReaderSettingHelper;
import com.blinkboxbooks.android.provider.BBBAsyncQueryHandler;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettings;
import com.blinkboxbooks.android.util.AnalyticsHelper;

public class EPub2ReaderSettingController {

    private static EPub2ReaderSettingController sInstance = null;
    private ReaderBrightnessListener mReaderBrightnessListener;
    private EPub2SettingsChangedListener mEPub2SettingsChangedListener;
    private ReaderSetting mReaderSetting;

    public enum SettingValue {
        FONT_SIZE(0.625f, 2.625f, 0.125f),
        BRIGHTNESS(0.05f, 1.0f, 0.05f),
        LINE_SPACE(1.333f, 6.083f, 0.25f);

        public final float minValue;
        public final float maxValue;
        public final float stepValue;

        private SettingValue(float minValue, float maxValue, float stepValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
            this.stepValue = stepValue;
        }

        public int getProgressMax() {
            return (int) ((maxValue - minValue) / stepValue);
        }

        public int getProgress(float value) {
            return (int) ((value - minValue) / stepValue);
        }

        public float getValue(int progress) {
            return (minValue + progress * stepValue);
        }
    }

    public enum FontType {
        SERIF(R.string.font_label_serif_font,"serif", Typeface.SERIF),
        SANS(R.string.font_label_sans_serif_font,"sans-serif", Typeface.SANS_SERIF),
        MONOSPACE(R.string.font_label_monospace_font,"monospace", Typeface.MONOSPACE);

        public final int label;
        public final String font_family;
        public final Typeface font_typeface;

        private FontType(int label, String font_family, Typeface font_typeface) {
            this.label = label;
            this.font_family = font_family;
            this.font_typeface = font_typeface;
        }
    }

    public enum ColorValue {
        BLACK(1, 1),
        WHITE(0, 0),
        SEPIA(2, 2);

        public final int background_color;
        public final int foreground_color;

        private ColorValue(int background_color, int foreground_color) {
            this.background_color = background_color;
            this.foreground_color = foreground_color;
        }
    }

    public enum TextAlign {
        LEFT(READER_SETTING_TEXT_ALIGN_LEFT, R.string.left),
        JUSTIFY(READER_SETTING_TEXT_ALIGN_JUSTIFY, R.string.justify);

        public String key;
        public int label;

        private TextAlign(String key, int label) {
            this.key = key;
            this.label = label;
        }
    }

    public static final String READER_SETTING_TEXT_ALIGN_LEFT = "left";
    public static final String READER_SETTING_TEXT_ALIGN_JUSTIFY = "justify";
    public static final String READER_SETTING_TEXT_MARGIN_WIDE = "max";
    public static final String READER_SETTING_TEXT_MARGIN_MEDIUM = "medium";
    public static final String READER_SETTING_TEXT_MARGIN_NARROW = "min";

    public static final float READER_SETTING_AUTOMATIC_BRIGHTNESS = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

    public static final int READER_SETTING_DEFAULT_BACKGROUND_COLOR = ColorValue.WHITE.background_color;
    public static final int READER_SETTING_DEFAULT_FOREGROUND_COLOR = ColorValue.WHITE.foreground_color;

    public static final float READER_SETTING_DEFAULT_BRIGHTNESS = READER_SETTING_AUTOMATIC_BRIGHTNESS;

    public static float READER_SETTING_DEFAULT_FONT_SIZE;
    public static float READER_SETTING_DEFAULT_FONT_SIZE_PREVIEW;

    public static float READER_SETTING_DEFAULT_LINE_SPACE;
    public static float READER_SETTING_DEFAULT_MARGIN_TOP;
    public static float READER_SETTING_DEFAULT_MARGIN_BOTTOM;

    public static final String READER_SETTING_DEFAULT_FONT_TYPEFACE = FontType.SERIF.font_family;
    public static final String READER_SETTING_DEFAULT_TEXT_ALIGN = READER_SETTING_TEXT_ALIGN_LEFT;

    public static final float[] READING_SETTING_FONT_SIZE = {0.875f, 1.0f , 1.125f, 1.25f, 1.375f, 1.5f};
    public static final float[] READING_SETTING_LINE_SPACES = {1.583f, 1.683f, 1.783f};

    public static EPub2ReaderSettingController getInstance() {

        if (sInstance == null) {
            sInstance = new EPub2ReaderSettingController();
        }

        return sInstance;
    }

    private EPub2ReaderSettingController() {
        Resources resources = BBBApplication.getApplication().getResources();

        TypedValue outValue = new TypedValue();

        resources.getValue(R.dimen.font_size, outValue, true);
        READER_SETTING_DEFAULT_FONT_SIZE = outValue.getFloat();

        resources.getValue(R.dimen.font_size_preview, outValue, true);
        READER_SETTING_DEFAULT_FONT_SIZE_PREVIEW = outValue.getFloat();

        resources.getValue(R.dimen.line_space, outValue, true);
        READER_SETTING_DEFAULT_LINE_SPACE = outValue.getFloat();

        resources.getValue(R.dimen.margin_top, outValue, true);
        READER_SETTING_DEFAULT_MARGIN_TOP = outValue.getFloat();

        resources.getValue(R.dimen.margin_bottom, outValue, true);
        READER_SETTING_DEFAULT_MARGIN_BOTTOM = outValue.getFloat();
    }

    public interface ReaderSettingListener {
        // Called when the reader settings has been loaded
        void onReaderSettingLoaded(ReaderSetting readerSetting);
    }

    public interface ReaderBrightnessListener {
        // Called when the settings are first loaded, and whenever the brightness is changed
        void onSetBrightness(float brightness);
    }

    /**
     * Asynchronously load the reader settings for a given account
     *
     * @param listener
     */
    public void loadReaderSetting(final ReaderSettingListener listener) {
        final String account = AccountController.getInstance().getUserId();

        if (mReaderSetting != null && mReaderSetting.account.equals(account)) {
            listener.onReaderSettingLoaded(mReaderSetting);
        } else {
            new AsyncTask<Void, Void, ReaderSetting>() {

                protected ReaderSetting doInBackground(Void... params) {
                    return getReaderSetting(account);
                }

                protected void onPostExecute(ReaderSetting result) {
                    mReaderSetting = result;
                    listener.onReaderSettingLoaded(result);
                }
            }.execute();
        }
    }

    public ReaderSetting getDefaultSettings(String account) {
        ReaderSetting readerSetting = new ReaderSetting();

        readerSetting.account = account;
        readerSetting.brightness = READER_SETTING_DEFAULT_BRIGHTNESS;
        readerSetting.font_size = READER_SETTING_DEFAULT_FONT_SIZE;
        readerSetting.line_space = READER_SETTING_DEFAULT_LINE_SPACE;
        readerSetting.text_align = READER_SETTING_DEFAULT_TEXT_ALIGN;
        readerSetting.font_typeface = READER_SETTING_DEFAULT_FONT_TYPEFACE;
        readerSetting.background_color = READER_SETTING_DEFAULT_BACKGROUND_COLOR;
        readerSetting.foreground_color = READER_SETTING_DEFAULT_FOREGROUND_COLOR;
        readerSetting.margin_bottom = READER_SETTING_DEFAULT_MARGIN_BOTTOM;
        readerSetting.margin_top = READER_SETTING_DEFAULT_MARGIN_TOP;
        readerSetting.margin_left = BBBApplication.getApplication().getResources().getInteger(R.integer.default_reader_margin);
        readerSetting.margin_right = BBBApplication.getApplication().getResources().getInteger(R.integer.default_reader_margin);
        readerSetting.publisher_styles = true;

        return readerSetting;
    }

    /**
     * resets all settings to their default values
     */
    public void resetToDefault(String account) {
        mReaderSetting = getDefaultSettings(account);
    }

    public void setReaderBrightnessListener(ReaderBrightnessListener mReaderBrightnessListener) {
        this.mReaderBrightnessListener = mReaderBrightnessListener;
    }

    public void setEPub2SettingsChangedListener(EPub2SettingsChangedListener mEPub2SettingsChangedListener) {
        this.mEPub2SettingsChangedListener = mEPub2SettingsChangedListener;
    }

    /**
     * Returns the reader settings of the current user.
     *
     * @return the ReaderSetting object
     */
    public ReaderSetting getReaderSetting() {
        return mReaderSetting;
    }

    public ReaderSetting getReaderSetting(String account) {
        mReaderSetting = ReaderSettingHelper.loadReaderSetting(account);
        // Load default settings
        if (mReaderSetting == null) {
            resetToDefault(account);
        }

        return mReaderSetting;
    }

    public void applyReaderSettings() {

        if (mReaderBrightnessListener != null) {
            mReaderBrightnessListener.onSetBrightness(mReaderSetting.brightness);
        }

        if (mEPub2SettingsChangedListener != null) {
            String theme = getReaderTheme(mReaderSetting.background_color);

            mEPub2SettingsChangedListener.onSetPreferences(theme, mReaderSetting.font_size, mReaderSetting.font_typeface, mReaderSetting.line_space,
                    mReaderSetting.text_align, mReaderSetting.margin_top, mReaderSetting.margin_left, mReaderSetting.margin_bottom, mReaderSetting.margin_right, mReaderSetting.publisher_styles);
        }

        saveReaderSetting();
    }

    public void saveReaderSetting() {
        Uri uri = ReaderSettings.buildReaderSettingAccountUri(mReaderSetting.account);
        ContentValues contentValues = ReaderSettingHelper.getContentValues(mReaderSetting);
        BBBAsyncQueryHandler.getInstance().startInsert(0, null, uri, contentValues);
    }

    public void setBrightness(float brightness) {
        AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_BRIGHTNESS,Float.toString(brightness), null);
        mReaderSetting.brightness = brightness;

        if (mReaderBrightnessListener != null) {
            mReaderBrightnessListener.onSetBrightness(brightness);
        }

        saveReaderSetting();
    }

    public void setReaderTheme(int backgroundColor, int foregroundColor) {
        mReaderSetting.background_color = backgroundColor;
        mReaderSetting.foreground_color = foregroundColor;

        if (mEPub2SettingsChangedListener != null) {
            String theme = getReaderTheme(backgroundColor);
            mEPub2SettingsChangedListener.onSetReaderTheme(theme);
        }

        saveReaderSetting();
    }

    public void setPublisherStylesEnabled(boolean enabled) {
        mReaderSetting.publisher_styles = enabled;

        if (mEPub2SettingsChangedListener != null) {
            mEPub2SettingsChangedListener.onSetPublisherStylesEnabled(enabled);
        }

        saveReaderSetting();
    }

    public float setReaderFontSize(float fontSize) {

        if (fontSize <= SettingValue.FONT_SIZE.minValue) {
            mReaderSetting.font_size = SettingValue.FONT_SIZE.minValue;
        } else if (fontSize >= SettingValue.FONT_SIZE.maxValue) {
            mReaderSetting.font_size = SettingValue.FONT_SIZE.maxValue;
        } else {
            mReaderSetting.font_size = fontSize;
        }

        if (mEPub2SettingsChangedListener != null) {
            AnalyticsHelper.getInstance().sendEvent(AnalyticsHelper.CATEGORY_READER_SETTINGS,AnalyticsHelper.GA_EVENT_ZOOM,Float.toString(fontSize), null);
            mEPub2SettingsChangedListener.onSetFontSize(mReaderSetting.font_size);
        }

        saveReaderSetting();

        return mReaderSetting.font_size;
    }

    public void setFontTypeface(FontType fontType) {
        mReaderSetting.font_typeface = fontType.font_family;

        if (mEPub2SettingsChangedListener != null) {
            mEPub2SettingsChangedListener.onSetTypeFace(mReaderSetting.font_typeface);
        }

        saveReaderSetting();
    }

    public float setLineSpace(float lineSpace) {

        if (lineSpace <= SettingValue.LINE_SPACE.minValue) {
            mReaderSetting.line_space = SettingValue.LINE_SPACE.minValue;
        } else if (lineSpace >= SettingValue.LINE_SPACE.maxValue) {
            mReaderSetting.line_space = SettingValue.LINE_SPACE.maxValue;
        } else {
            mReaderSetting.line_space = lineSpace;
        }

        if (mEPub2SettingsChangedListener != null) {
            mEPub2SettingsChangedListener.onSetLineHeight(mReaderSetting.line_space);
        }

        saveReaderSetting();

        return mReaderSetting.line_space;
    }

    public void setReaderTextAlign(String alignment) {
        mReaderSetting.text_align = alignment;

        if (mEPub2SettingsChangedListener != null) {
            mEPub2SettingsChangedListener.onSetTextAlignment(alignment);
        }

        saveReaderSetting();
    }

    public void setReaderTextMargin(String margin) {

        if (READER_SETTING_TEXT_MARGIN_WIDE.equals(margin)) {
            mReaderSetting.margin_left = 18;
            mReaderSetting.margin_right = 18;
        } else if (READER_SETTING_TEXT_MARGIN_NARROW.equals(margin)) {
            mReaderSetting.margin_left = 4;
            mReaderSetting.margin_right = 4;
        } else {
            mReaderSetting.margin_left = BBBApplication.getApplication().getResources().getInteger(R.integer.default_reader_margin);
            mReaderSetting.margin_right = BBBApplication.getApplication().getResources().getInteger(R.integer.default_reader_margin);
        }

        if (mEPub2SettingsChangedListener != null) {
            mEPub2SettingsChangedListener.onSetMargin(mReaderSetting.margin_top, mReaderSetting.margin_left, mReaderSetting.margin_bottom, mReaderSetting.margin_right);
        }

        saveReaderSetting();
    }

    // Here be dragons - TODO: upgrade db to match javascript interface - then we can remove all this cruft
    public static String getReaderTheme(int backgroundcolor) {
        // TODO: upgrade db
        String theme = "sepia";
        switch (backgroundcolor) {
            case 1:
                theme = "dark";
                break;
            case 2:
                theme = "sepia";
                break;
            default:
                theme = "light";
                break;
        }
        return theme;
    }
}
