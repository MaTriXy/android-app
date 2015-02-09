// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.

package com.blinkboxbooks.android.model.helper;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

import com.blinkboxbooks.android.BBBApplication;
import com.blinkboxbooks.android.model.ReaderSetting;
import com.blinkboxbooks.android.provider.BBBAsyncQueryHandler;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.provider.BBBContract.ReaderSettings;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController;

/**
 * Helper class for converting a ReaderSetting object to and from a Cursor
 */
public class ReaderSettingHelper {

    /**
     * Transfer reader settings from one account to another
     *
     * @param accountFrom
     * @param accountTo
     */
    public static void transferReaderSetting(final String accountFrom, final String accountTo) {
        Uri uriTo = ReaderSettings.buildReaderSettingAccountUri(accountTo);
        ReaderSetting fromSettings = EPub2ReaderSettingController.getInstance().getReaderSetting(accountFrom);
        ReaderSetting toSettings = loadReaderSetting(accountTo);
        if (fromSettings != null && toSettings == null) {
            fromSettings.account = accountTo;
            ContentValues contentValues = ReaderSettingHelper.getContentValues(fromSettings);
            BBBAsyncQueryHandler.getInstance().startInsert(0, null, uriTo, contentValues);
        }
    }

    /**
     * Load a reader settings object from the db if it exists
     *
     * @param account The account name
     * @return ReaderSetting or null
     */
    public static ReaderSetting loadReaderSetting(final String account) {
        ReaderSetting readerSetting = null;
        Uri uri = ReaderSettings.buildReaderSettingAccountUri(account);
        Cursor cursor = BBBApplication.getApplication().getContentResolver().query(uri, null, null, null, null);
        if (cursor.moveToFirst()) {
            readerSetting = ReaderSettingHelper.createReaderSetting(cursor);
        }
        cursor.close();
        return readerSetting;
    }

    public static ReaderSetting createReaderSetting(Cursor cursor) {
        ReaderSetting readerSetting = new ReaderSetting();

        readerSetting.account = cursor.getString(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_ACCOUNT));
        readerSetting.background_color = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_BACKGROUND_COLOR));
        readerSetting.foreground_color = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_FOREGROUND_COLOR));
        readerSetting.font_size = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_FONT_SIZE));
        readerSetting.brightness = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_BRIGHTNESS));
        readerSetting.font_typeface = cursor.getString(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_FONT_TYPEFACE));
        readerSetting.line_space = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_LINE_SPACE));
        readerSetting.orientation_lock = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_ORIENTATION_LOCK)) > 0;
        readerSetting.show_header = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_SHOW_HEADER)) > 0;
        readerSetting.show_footer = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_SHOW_FOOTER)) > 0;
        readerSetting.cloud_bookmark = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_CLOUD_BOOKMARK)) > 0;
        readerSetting.text_align = cursor.getString(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_TEXT_ALIGN));
        readerSetting.margin_top = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_MARGIN_TOP));
        readerSetting.margin_bottom = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_MARGIN_BOTTOM));
        readerSetting.margin_left = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_MARGIN_LEFT));
        readerSetting.margin_right = cursor.getFloat(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_MARGIN_RIGHT));
        readerSetting.reader_orientation = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_ORIENTATION));
        readerSetting.publisher_styles = cursor.getInt(cursor.getColumnIndex(BBBContract.ReaderSettingsColumns.READER_PUBLISHER_STYLES)) > 0;

        return readerSetting;
    }

    /**
     * Returns a ContentValues instance (a map) for this ReaderSetting instance. This is useful for inserting a ReaderSetting into a database.
     *
     * @param readerSetting
     * @return the ContentValues object
     */
    public static ContentValues getContentValues(ReaderSetting readerSetting) {
        ContentValues contentValues = new ContentValues();

        contentValues.put(BBBContract.ReaderSettingsColumns.READER_ACCOUNT, readerSetting.account);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_BACKGROUND_COLOR, readerSetting.background_color);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_FOREGROUND_COLOR, readerSetting.foreground_color);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_FONT_SIZE, readerSetting.font_size);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_BRIGHTNESS, readerSetting.brightness);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_FONT_TYPEFACE, readerSetting.font_typeface);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_LINE_SPACE, readerSetting.line_space);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_ORIENTATION_LOCK, readerSetting.orientation_lock);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_SHOW_HEADER, readerSetting.show_header);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_SHOW_FOOTER, readerSetting.show_footer);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_CLOUD_BOOKMARK, readerSetting.cloud_bookmark);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_TEXT_ALIGN, readerSetting.text_align);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_MARGIN_TOP, readerSetting.margin_top);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_MARGIN_BOTTOM, readerSetting.margin_bottom);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_MARGIN_LEFT, readerSetting.margin_left);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_MARGIN_RIGHT, readerSetting.margin_right);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_ORIENTATION, readerSetting.reader_orientation);
        contentValues.put(BBBContract.ReaderSettingsColumns.READER_PUBLISHER_STYLES, readerSetting.publisher_styles);

        return contentValues;
    }
}
