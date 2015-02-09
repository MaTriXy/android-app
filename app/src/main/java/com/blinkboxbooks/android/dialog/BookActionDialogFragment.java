// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.util.BBBAlertDialogBuilder;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.widget.BookCover;

/**
 * Shows a dialog that has book cover, title, author, publication date and the usual title, message, positive and negative buttons
 */
public class BookActionDialogFragment extends DialogFragment {

    private static final String PARAM_BOOK = "book";
    private static final String PARAM_TITLE = "title";
    private static final String PARAM_MESSAGE = "message";
    private static final String PARAM_POSITIVE_BUTTON = "positive_button";
    private static final String PARAM_NEGATIVE_BUTTON = "negative_button";

    public static BookActionDialogFragment newInstance(Book book, String title, String message, String positiveButtonText, String negativeButtonText) {
        BookActionDialogFragment fragment = new BookActionDialogFragment();

        Bundle args = new Bundle();
        args.putSerializable(PARAM_BOOK, book);
        args.putString(PARAM_TITLE, title);
        args.putString(PARAM_MESSAGE, message);
        args.putString(PARAM_POSITIVE_BUTTON, positiveButtonText);
        args.putString(PARAM_NEGATIVE_BUTTON, negativeButtonText);
        fragment.setArguments(args);

        return fragment;
    }

    private Book mBook;
    private String mTitle;
    private String mMessage;
    private String mPositiveButtonText;
    private String mNegativeButtonText;

    public BookActionDialogFragment() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle arguments = getArguments();
        if (arguments != null) {
            mBook = (Book) arguments.get(PARAM_BOOK);
            mTitle = arguments.getString(PARAM_TITLE);
            mMessage = arguments.getString(PARAM_MESSAGE);
            mPositiveButtonText = arguments.getString(PARAM_POSITIVE_BUTTON);
            mNegativeButtonText = arguments.getString(PARAM_NEGATIVE_BUTTON);
        }

        BBBAlertDialogBuilder builder = new BBBAlertDialogBuilder(getActivity());

        View layout = View.inflate(getActivity(), R.layout.fragment_dialog_bookaction, null);
        builder.setView(layout);

        TextView textViewTitle = (TextView) layout.findViewById(R.id.textview_title);
        TextView textViewAuthor = (TextView) layout.findViewById(R.id.textview_author);
        TextView textViewPublished = (TextView) layout.findViewById(R.id.textview_published);
        TextView textViewMessage = (TextView) layout.findViewById(R.id.textview_message);
        BookCover bookCover = (BookCover) layout.findViewById(R.id.bookcover);

        textViewTitle.setText(mBook.title);
        textViewAuthor.setText(mBook.author);

        String publicationDate = String.format(getString(R.string.date_of_publication), BBBCalendarUtil.formatDate(mBook.publication_date));

        textViewPublished.setText(publicationDate);
        textViewMessage.setText(mMessage);

        bookCover.setBook(mBook);

        if (!TextUtils.isEmpty(mTitle)) {
            builder.setTitle(mTitle);
        }

        if (!TextUtils.isEmpty(mPositiveButtonText)) {
            builder.setPositiveButton(mPositiveButtonText, null);
        }

        if (!TextUtils.isEmpty(mNegativeButtonText)) {
            builder.setNeutralButton(mNegativeButtonText, null);
        }

        return builder.create();
    }
}