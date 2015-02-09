// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import com.blinkboxbooks.android.model.BookType;
import com.blinkboxbooks.android.ui.reader.epub2.EPub2ReaderFragment;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingsFragment;
import com.blinkboxbooks.android.ui.reader.epub3.EPub3ReaderFragment;
import com.blinkboxbooks.android.ui.reader.epub3.EPub3ReaderSettingsFragment;
import com.blinkboxbooks.android.ui.reader.pdf.PDFReaderFragment;
import com.blinkboxbooks.android.ui.reader.pdf.PDFReaderSettingsFragment;

public class ReaderFragmentFactory {

    public static ReaderFragment createReaderFragment(BookType bookType) {
        ReaderFragment fragment = null;

        switch (bookType) {
            case EPUB2:
                fragment = new EPub2ReaderFragment();
                break;
            case EPUB3:
                fragment = new EPub3ReaderFragment();
                break;
            case PDF:
                fragment = new PDFReaderFragment();
                break;
        }

        return fragment;
    }

    public static EPub2ReaderSettingsFragment createReaderSettingFragment(BookType bookType) {
        EPub2ReaderSettingsFragment fragment = null;

        switch (bookType) {
            case EPUB2:
                fragment = new EPub2ReaderSettingsFragment();
                break;
            case EPUB3:
                fragment = new EPub3ReaderSettingsFragment();
                break;
            case PDF:
                fragment = new PDFReaderSettingsFragment();
                break;
        }

        return fragment;
    }
}