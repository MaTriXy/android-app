// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.text.Html;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.controller.LibraryController;
import com.blinkboxbooks.android.model.Book;

public class AboutBookItem extends LinearLayout {

    private TextView mTextViewTitle;
    private TextView mTextViewAuthor;

    private View mViewContainerText;

    private BookCover mBookCover;

    public AboutBookItem(Context context) {
        super(context);

        initialise(context);
    }

    public AboutBookItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        initialise(context);
    }

    public AboutBookItem(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initialise(context);
    }

    protected void initialise(Context context) {
        View.inflate(context, R.layout.view_book_about, this);

        mViewContainerText = findViewById(R.id.container_text);

        mBookCover = (BookCover) findViewById(R.id.bookcover);
        mTextViewTitle = (TextView) findViewById(R.id.textview_title);
        mTextViewAuthor = (TextView) findViewById(R.id.textview_author);
    }

    /**
     * Sets the width of this view
     * @param width
     */
    public void setBookCoverWidth(int width) {

        if (mBookCover != null) {
            ViewGroup.LayoutParams params = mBookCover.getLayoutParams();

            //Rect padding = mBookCover.getFramePadding();
            //width = width - padding.left - padding.right;

            params.width = width;
            params.height = (int) (width * LibraryController.WIDTH_HEIGHT_RATIO);

            mBookCover.setLayoutParams(params);
        }
    }

    /**
     * Sets the book to display
     *
     * @param book
     * @param showTitleAndAuthor
     */
    public void setBook(Book book, boolean showTitleAndAuthor) {

        if(book != null) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.INVISIBLE);
        }

        mBookCover.setBook(book);

        if(showTitleAndAuthor) {
            mViewContainerText.setVisibility(View.VISIBLE);

            if (book != null && book.title != null && book.author != null) {
                mTextViewTitle.setText(Html.fromHtml(book.title));
                mTextViewAuthor.setText(Html.fromHtml(book.author));
            } else {
                mTextViewTitle.setText("");
                mTextViewAuthor.setText("");
            }
        } else {
            mViewContainerText.setVisibility(View.GONE);
        }
    }
}