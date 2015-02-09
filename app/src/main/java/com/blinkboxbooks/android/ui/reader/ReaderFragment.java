// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.WindowManager;

import com.blinkbox.java.book.model.BBBEPubBook;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.model.Bookmark;
import com.blinkboxbooks.android.model.ReaderSetting;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.ReaderBrightnessListener;
import com.blinkboxbooks.android.ui.reader.epub2.settings.EPub2ReaderSettingController.ReaderSettingListener;
import com.crashlytics.android.Crashlytics;

public abstract class ReaderFragment extends Fragment implements ReaderBrightnessListener {

    public abstract void setBook(BBBEPubBook ebook, Book book, Bookmark bookmark, String bookmarks, String highlights, boolean isOnlineSample);

    public abstract void setBook(Book book);

    public abstract void bookmarkPage();

    public abstract void goToBookmark(Bookmark bookmark, boolean keepCurrentReadingPosition);

    public abstract void highlightSelection(String content);

    public abstract void removeHighlight(String cfi);

    public abstract void goToChapter(String url, boolean keepCurrentReadingPosition);

    public abstract void goToProgress(float progress);

    public abstract void toggleReaderOverlay(boolean visible);

    public abstract void displayVersion();

    public abstract void setPreview(String url, Bookmark lastReadingPosition);

    public abstract void setTextSelectionListener(TextSelectionListener textSelectionListener);

    public abstract int getHeight();

    public abstract void setInteractionAllowed(boolean allowInteraction);

    public abstract void hideTextSelector();

    @Override
    public void onSetBrightness(float brightness) {
        try {
            WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
            lp.screenBrightness = brightness;
            getActivity().getWindow().setAttributes(lp);
        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        EPub2ReaderSettingController.getInstance().setReaderBrightnessListener(this);

        EPub2ReaderSettingController.getInstance().loadReaderSetting(new ReaderSettingListener() {
            @Override
            public void onReaderSettingLoaded(ReaderSetting readerSetting) {
                if (isResumed()) {
                    onSetBrightness(readerSetting.brightness);
                }
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();

        EPub2ReaderSettingController.getInstance().setReaderBrightnessListener(null);
    }


    /**
     * Offers the reader fragment a chance to handle the user pressing the back button.
     * @return true if the fragment has consumed the back button press.
     */
    public boolean handleBackPressed() {
        // By default the fragment just ignores the press
        return false;
    }
}