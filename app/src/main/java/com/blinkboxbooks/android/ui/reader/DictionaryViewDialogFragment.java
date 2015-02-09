// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.DictionaryController;

import java.util.Locale;

/**
 * A custom dialog fragment that is used to overlay the reader with dictionary information. The dictionary
 * will be initialised at the top or bottom of the screen and supports a button to expand the dictionary
 * to fill the screen and display more detailed information.
 *
 * It is important that this functionality is in a dialog fragment as this will overlay the native text
 * edit control markers. A simple view will not overlay the text markers even if it has the correct z-order.
 */
public class DictionaryViewDialogFragment extends DialogFragment implements LoaderManager.LoaderCallbacks<DictionaryViewDialogFragment.DictionaryResult> {

    private static enum ViewType {
        MESSAGE,
        INITIALISATION,
        DEFINITION
    }

    /**
     * A very simple data storage class that encapsulates the results of a dictionary search
     */
    static class DictionaryResult {
        /** The html content to be displayed */
        public String html;

        /** Boolean indicating if no definition was found for the word */
        public boolean noDefinitionFound;

        /** Boolean indicating that a full detailed search was performed */
        public boolean fullSearch;

        /** Boolean indicating if the dictionary is available (false means not available and failed to copy) */
        public boolean dictionaryOk;

        /**
         * Construct a new Dictionary Result
         * @param html the html content to display
         * @param noDefinitionFound set to true if no definition was found in the dictionary
         * @param fullSearch set to true if a full definition search was performed
         * @param dictionaryOk set to false if the dictionary is not available
         */
        public DictionaryResult(String html, boolean noDefinitionFound, boolean fullSearch, boolean dictionaryOk) {
            this.html = html;
            this.noDefinitionFound = noDefinitionFound;
            this.fullSearch = fullSearch;
            this.dictionaryOk = dictionaryOk;
        }

    }

    /**
     * An interface that can be implemented and passed into this DialogFragment to be notified when the
     * dialog is dismissed.
     */
    public interface DictionaryDismissedListener {
        void dictionaryDismissed();
    }

    // Extra value keys used to pass in values to the DialogFragment
    private static final String EXTRA_WORD_TO_SEARCH_FOR = "WordToSearchFor";
    private static final String EXTRA_DISPLAY_AT_TOP = "DisplayAtTop";
    private static final String EXTRA_TOOLBAR_HEIGHT = "ToolbarHeight";

    private String mWord;
    private WebView mWebView;
    private View mViewInitialisation;
    private View mViewDefinition;
    private TextView mTextViewMessage;
    private ViewType mViewType;
    private View mViewDropshadowTop;
    private View mViewDropshadowBottom;
    private View mFullDefinitionButton;
    private View mRootView;
    private boolean mDisplayAtTop;

    private DictionaryDismissedListener mDictionaryDismissedListener;

    /**
     * Creates a new instance of a DictionaryViewDialogFragment
     *
     * @param word the word to be displayed in the dictionary
     * @param displayAtTop set to true if the dictionary should be initialised at the top of the screen
     * @param toolbarHeight the height of the toolbar in pixels
     * @return a DictionaryViewDialogFragment
     */
    public static DictionaryViewDialogFragment newInstance(String word, boolean displayAtTop, int toolbarHeight) {
        DictionaryViewDialogFragment dialogFragment = new DictionaryViewDialogFragment();
        Bundle arguments = new Bundle();
        arguments.putString(EXTRA_WORD_TO_SEARCH_FOR, word);
        arguments.putBoolean(EXTRA_DISPLAY_AT_TOP, displayAtTop);
        arguments.putInt(EXTRA_TOOLBAR_HEIGHT, toolbarHeight);
        dialogFragment.setArguments(arguments);
        return dialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWord = getArguments().getString(EXTRA_WORD_TO_SEARCH_FOR);
    }

    @Override
    public void onActivityCreated(Bundle arg0) {
        super.onActivityCreated(arg0);
        if (mDisplayAtTop) {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimationTop;
        } else {
            getDialog().getWindow().getAttributes().windowAnimations = R.style.DialogAnimationBottom;
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Window dialogWindow = getDialog().getWindow();

        dialogWindow.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
        dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int searchTopMargin = getArguments().getInt(EXTRA_TOOLBAR_HEIGHT);
        final int height = size.y - searchTopMargin;

        View layout = View.inflate(getActivity(), R.layout.fragment_dialog_dictionary, null);

        mRootView = layout;
        mWebView = (WebView) layout.findViewById(R.id.webview);
        mViewInitialisation = layout.findViewById(R.id.layout_initialisation);
        mViewDefinition = layout.findViewById(R.id.layout_definition);
        mTextViewMessage = (TextView) layout.findViewById(R.id.textview_message);
        mViewDropshadowTop = layout.findViewById(R.id.view_dropshadow_top);
        mViewDropshadowBottom = layout.findViewById(R.id.view_dropshadow_bottom);
        mFullDefinitionButton = layout.findViewById(R.id.button_see_full_definition);

        dialog.setContentView(layout);
        WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
        p.width = ActionBar.LayoutParams.MATCH_PARENT;
        p.height = getResources().getDimensionPixelOffset(R.dimen.dictionary_initial_height);
        mDisplayAtTop = getArguments().getBoolean(EXTRA_DISPLAY_AT_TOP);

        if (mDisplayAtTop) {
            mViewDropshadowBottom.setVisibility(View.VISIBLE);
            p.y = searchTopMargin;
            dialog.getWindow().setGravity(Gravity.TOP | Gravity.LEFT);
        } else {
            mViewDropshadowTop.setVisibility(View.VISIBLE);
            p.y = 0;
            dialog.getWindow().setGravity(Gravity.BOTTOM | Gravity.LEFT);
        }
        dialog.getWindow().setAttributes(p);

        // Handle the click on the full definition button by expanding the dialog and making the relevant controls visible/gone
        mFullDefinitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                searchForWord(true);
                mFullDefinitionButton.setVisibility(View.GONE);
                mViewDropshadowBottom.setVisibility(View.GONE);
                mViewDropshadowTop.setVisibility(View.GONE);
                mViewDefinition.setVisibility(View.GONE);
                mViewInitialisation.setVisibility(View.VISIBLE);
                clearWebViewContent();

                WindowManager.LayoutParams p = dialog.getWindow().getAttributes();
                p.height = height;
                dialog.getWindow().setAttributes(p);
            }
        });

        getLoaderManager().initLoader(0, null, this).forceLoad();

        return dialog;
    }

    /**
     * Register a listener to be notified when the dictionary dialog is dismissed.
     * @param listener the listener to be notified when the dialog is dismissed.
     */
    public void setDictionaryDismissedListener(DictionaryDismissedListener listener) {
        mDictionaryDismissedListener = listener;
    }

    /**
     * Refresh the current dictionary definition to a new word.
     * @param word the new word to lookup in the dictionary.
     */
    public void updateWord(String word) {
        mWord = word;

        if (word.contains(" ")) {
            mRootView.setVisibility(View.INVISIBLE);
        } else {
            mRootView.setVisibility(View.VISIBLE);
            searchForWord(false);
        }

        mViewInitialisation.setVisibility(View.VISIBLE);
        mViewDefinition.setVisibility(View.GONE);

        // Clear the web view content so we don't get any chance of annoying flicker when we later swap out the text
        clearWebViewContent();
    }

    @SuppressWarnings("deprecation")
    private void clearWebViewContent() {
        if (Build.VERSION.SDK_INT < 18) {
            mWebView.clearView();
        } else {
            // clearView is deprecated in V18 and this is the Google documented alternative
            mWebView.loadUrl("about:blank");
        }
    }

    private void showPrimaryDefinitionForWord(String html, boolean noDefinitionFound) {
        mWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", "");

        // Show/hide the full definition 'button' depending on if we found the any definition text
        if (noDefinitionFound) {
            mFullDefinitionButton.setVisibility(View.GONE);
        } else {
            mFullDefinitionButton.setVisibility(View.VISIBLE);
        }
        setViewType(ViewType.DEFINITION);
    }

    private void showFullDefinitionForWord(String html) {
        mWebView.loadDataWithBaseURL("file:///android_asset/", html, "text/html", "UTF-8", "");
        setViewType(ViewType.DEFINITION);
    }

    private void setViewType(ViewType viewType) {
        mViewType = viewType;

        switch (mViewType) {
            case INITIALISATION:
                mViewInitialisation.setVisibility(View.VISIBLE);
                mViewDefinition.setVisibility(View.GONE);
                mTextViewMessage.setVisibility(View.GONE);
                break;
            case DEFINITION:
                mViewDefinition.setVisibility(View.VISIBLE);
                mViewInitialisation.setVisibility(View.GONE);
                mTextViewMessage.setVisibility(View.GONE);
                break;
            case MESSAGE:
                mTextViewMessage.setVisibility(View.VISIBLE);
                mViewInitialisation.setVisibility(View.GONE);
                mViewDefinition.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);

        // If we have a listener registered inform it that dictionary has been dismissed
        if (mDictionaryDismissedListener != null) {
            mDictionaryDismissedListener.dictionaryDismissed();
        }
    }

    // Tell the loader to lookup a new word
    private void searchForWord(boolean fullDictionary) {
        Loader<DictionaryResult> loader = getLoaderManager().getLoader(0);
        DictionaryWordLoader dictionaryLoader = (DictionaryWordLoader) loader;
        dictionaryLoader.onWordChanged(mWord, fullDictionary);
    }

    @Override
    public Loader<DictionaryResult> onCreateLoader(int i, Bundle bundle) {
        return new DictionaryWordLoader(getActivity(), mWord, false);
    }

    @Override
    public void onLoadFinished(Loader<DictionaryResult> stringLoader, DictionaryResult result) {
        if (result.dictionaryOk) {
            if (result.fullSearch) {
                showFullDefinitionForWord(result.html);
            } else {
                showPrimaryDefinitionForWord(result.html, result.noDefinitionFound);
            }
        } else {
            // Display the insufficient space message so the user knows why the lookup failed
            mTextViewMessage.setText(R.string.error_insufficient_space_for_dictionary);
            setViewType(ViewType.MESSAGE);
        }
    }

    @Override
    public void onLoaderReset(Loader<DictionaryResult> stringLoader) {
        // Nothing to do
    }

    // An AsyncTaskLoader that will look up a supplied word from the dictionary in the background
    private static class DictionaryWordLoader extends AsyncTaskLoader<DictionaryResult> {

        boolean mFull;
        String mWord;
        boolean mNoDefinitionFound = false;
        boolean mDictionaryAvailable = false;

        /**
         * Construct a new DictionaryWordLoader
         * @param context the Context object
         * @param word the word to search for
         * @param full boolean indicating if this is a full definition search
         */
        public DictionaryWordLoader(Context context, String word, boolean full) {
            super(context);
            mFull = full;
            mWord = word;
        }

        /**
         * Called when the word is updated to look up the new word from the dictionary
         * @param word the word to search for
         * @param full boolean indicating if this is a full definition search
         */
        public void onWordChanged(String word, boolean full) {
            mWord = word;
            mFull = full;
            onContentChanged();
        }

        @Override
        public DictionaryResult loadInBackground() {
            String html;
            boolean noDefinitionFound = false;

            DictionaryController dictionaryController = DictionaryController.getInstance();

            // The first time around we check that the dictionary is there and attempt to unzip it if not
            if (! mDictionaryAvailable) {

                if(dictionaryController.dictionaryFileUnzipped()) {
                    mDictionaryAvailable = true;
                } else {

                    // If there is no dictionary on the device we attempt to unzip it
                    if (dictionaryController.hasSufficientSpaceForUnzippedDictionary()) {
                        mDictionaryAvailable =  dictionaryController.unZipDictionary(dictionaryController.getDatabaseFileFullPathLocation());
                    }

                    // If the dictionary has not been unzipped then we return a result indicating that the
                    // dictionary is not ok
                    if (!mDictionaryAvailable) {
                        dictionaryController.clearDictionaryFileIfExists();
                        DictionaryResult result = new DictionaryResult("", noDefinitionFound, mFull, false);
                        return result;
                    }
                }
            }

            try {
                if(mFull) {
                    html = dictionaryController.getFullDefinitionHtml(mWord.toLowerCase(Locale.US));
                } else {
                    html = dictionaryController.getPrimaryDefinitionHtml(mWord.toLowerCase(Locale.US));
                }
            } catch (DictionaryController.DefinitionNotFoundException e) {
                noDefinitionFound = true;
                html = DictionaryController.getNoDefinitionFoundText(getContext().getResources());
            } catch (SQLiteCantOpenDatabaseException e) {
                dictionaryController.clearDictionaryFileIfExists();
                DictionaryResult result = new DictionaryResult("", noDefinitionFound, mFull, false);
                return result;
            }

            DictionaryResult result = new DictionaryResult(html, noDefinitionFound, mFull, true);

            return result;
        }
    }
}