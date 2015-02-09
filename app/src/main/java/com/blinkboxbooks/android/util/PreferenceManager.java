// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import com.blinkboxbooks.android.BBBApplication;

import java.util.Set;

/**
 * Helper class for saving and retrieving preferences
 */
public class PreferenceManager {

    private static PreferenceManager sInstance = new PreferenceManager();

    public static PreferenceManager getInstance() {
        return sInstance;
    }

    private static final String PREFERENCE_FILE_NAME = "com.blinkbox.books";

    public static final String PREF_KEY_LIBRARY_TAB_SELECTED = "library_tab_selected";
    public static final String PREF_KEY_CURRENT_USER = "key_userid";
    public static final String PREF_KEY_PREFERRED_NAME = "key_preferredname";
    public static final String PREF_KEY_EMBEDDED_BOOKS_VERSION = "key_embedded_books_version";
    public static final String PREF_KEY_SHOWN_WELCOME_PAGE = "key_shown_welcome_page";
    public static final String PREF_KEY_SHOWN_READER_HELP = "key_shown_reader_help";
    public static final String PREF_KEY_HAS_LAUNCHED = "key_has_launched";
    public static final String PREF_KEY_REMOVE_FROM_DEVICE_WARNING_COUNT = "key_remove_from_device_warning_count";
    public static final String PREF_KEY_REMOVE_SAMPLE_WARNING_COUNT = "key_remove_sample_warning_count";
    public static final String PREF_KEY_SHOW_REMINDER = "already_dismissed";
    public static final String PREF_KEY_MY_LIBRARY_SORT_OPTION = "my_library_sort_option";
    public static final String PREF_KEY_CRC_ERRORS_SHOWN = "crc_errors_shown";
    public static final String PREF_KEY_SHOW_PRELOAD_INFORMATION = "show_preload_information";
    public static final String PREF_KEY_STORED_APP_VERSION = "stored_app_version";

    private SharedPreferences mPreferences;

    private PreferenceManager() {
        mPreferences = BBBApplication.getApplication().getSharedPreferences(PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Registers a OnSharedPreferenceChangeListener so we can listen for changes to preferences
     *
     * @param listener
     */
    public void registerOnPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Unregisters a OnSharedPreferenceChangeListener so we can stop listening for changes to preferences
     *
     * @param listener
     */
    public void unregisterOnPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        mPreferences.unregisterOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Sets a String preference
     *
     * @param key   the preference key
     * @param value the preference value
     */
    public void setPreference(String key, String value) {
        mPreferences.edit().putString(key, value).apply();
    }

    /**
     * Sets a boolean preference
     *
     * @param key   the preference key
     * @param value the preference value
     */
    public void setPreference(String key, boolean value) {
        mPreferences.edit().putBoolean(key, value).apply();
    }

    /**
     * Sets an int preference
     *
     * @param key   the preference key
     * @param value the preference value
     */
    public void setPreference(String key, int value) {
        mPreferences.edit().putInt(key, value).apply();
    }

    /**
     * Sets a long preference
     *
     * @param key   the preference key
     * @param value the preference value
     */
    public void setPreference(String key, long value) {
        mPreferences.edit().putLong(key, value).apply();
    }

    /**
     * Sets a String set preference
     *
     * @param key   the preference key
     * @param value the preference value
     */
    public void setPreference(String key, Set<String> value) {
        mPreferences.edit().putStringSet(key, value).apply();
    }

    /**
     * Gets a String preference
     *
     * @param key          the preference key
     * @param defaultValue the default value to return if this preference does not exist
     * @return the String value
     */
    public String getString(String key, String defaultValue) {
        return mPreferences == null ? defaultValue : mPreferences.getString(key, defaultValue);
    }

    /**
     * Gets a boolean preference
     *
     * @param key          the preference key
     * @param defaultValue the default value to return if this preference does not exist
     * @return the boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return mPreferences.getBoolean(key, defaultValue);
    }

    /**
     * Gets an int preference
     *
     * @param key          the preference key
     * @param defaultValue the default value to return if this preference does not exist
     * @return the boolean value
     */
    public int getInt(String key, int defaultValue) {
        return mPreferences.getInt(key, defaultValue);
    }

    /**
     * Gets a long preference
     *
     * @param key          the preference key
     * @param defaultValue the default value to return if this preference does not exist
     * @return the boolean value
     */
    public long getLong(String key, long defaultValue) {
        return mPreferences.getLong(key, defaultValue);
    }

    /**
     * Gets a String set preference
     *
     * @param key   the preference key
     * @param defaultValue the default value to return if the preference does not exist
     */
    public Set<String> getPreference(String key, Set<String> defaultValue) {
        return mPreferences.getStringSet(key, defaultValue);
    }
}