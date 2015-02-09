// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader.epub2.settings;

public interface EPub2SettingsChangedListener {

    void onSetReaderTheme(String theme);

    void onSetFontSize(float em);

    void onSetTypeFace(String font_typeface);

    void onSetLineHeight(float em);

    void onSetMargin(float top, float left, float bottom, float right);

    void setHeaderVisibility(boolean visible);

    void onSetTextAlignment(String alignment);

    void onSetPublisherStylesEnabled(boolean enabled);

    void onSetPreferences(String theme, float fontSize, String fontTypeface, float lineHeight, String textAlignment, float topMargin, float leftMargin, float bottomMargin, float rightMargin, boolean publisherStyles);
}