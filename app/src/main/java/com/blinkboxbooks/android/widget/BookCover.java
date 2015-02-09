// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.os.Handler;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.android.volley.Request.Priority;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.blinkboxbooks.android.BusinessRules;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.model.Book;
import com.blinkboxbooks.android.provider.BBBContract;
import com.blinkboxbooks.android.util.BBBCalendarUtil;
import com.blinkboxbooks.android.util.BBBImageLoader;

/**
 * BookCover widget. Shows a book cover with configurable additional information including new/sample banner, download state/progress, info/error messages
 */
public class BookCover extends FrameLayout implements ImageListener {

    private static final BBBImageLoader IMAGE_LOADER = BBBImageLoader.getInstance();

    private static final int DOWNLOAD_VIEW_BUTTON = 0;
    private static final int DOWNLOAD_VIEW_ERROR = 1;
    private static final int DOWNLOAD_VIEW_PROGRESS = 2;

    private ImageView mBookImage;
    private TextView mTitle;
    private TextView mAuthor;
    private TextView mBookPosition;
    private ImageView mImageBanner;
    private ViewFlipper mDownloadButtonContainer;
    private ProgressBar mLoadingSpinner;
    private TextView mDownloadPercent;

    private Book mBook;

    private boolean mShowCoverText;
    private boolean mShowDownloadStateIcons;
    private boolean mShowLoadingIcon;
    private boolean mFadeImageIn;
    private boolean mShowComingSoon;

    public BookCover(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        loadCustomAttributes(context, attrs);
        init(context);
    }

    public BookCover(Context context, AttributeSet attrs) {
        super(context, attrs);
        loadCustomAttributes(context, attrs);
        init(context);
    }

    public BookCover(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {

        View.inflate(context, R.layout.view_book_cover, this);

        mBookImage = (ImageView) findViewById(R.id.imageview_book_cover);
        mTitle = (TextView) findViewById(R.id.textview_book_cover_title);
        mAuthor = (TextView) findViewById(R.id.textview_book_cover_author);
        mBookPosition = (TextView) findViewById(R.id.textview_book_position);
        mImageBanner = (ImageView) findViewById(R.id.imageview_banner);
        mDownloadButtonContainer = (ViewFlipper) findViewById(R.id.viewflipper_download_button);
        mLoadingSpinner = (ProgressBar) findViewById(R.id.progressbar_loading_spinner);
        mDownloadPercent = (TextView) findViewById(R.id.textview_download_percent);

        if (mShowCoverText) {
            mTitle.setVisibility(View.VISIBLE);
            mAuthor.setVisibility(View.VISIBLE);
        }
        if (mShowLoadingIcon) {
            mLoadingSpinner.setVisibility(View.VISIBLE);
        }
        if (mShowDownloadStateIcons) {
            mDownloadButtonContainer.setVisibility(View.VISIBLE);
        }

        setBackgroundColor(getResources().getColor(R.color.book_background));
    }

    private void loadCustomAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.BookCover, 0, 0);

        try {
            mShowCoverText = a.getBoolean(R.styleable.BookCover_showCoverText, false);
            mShowDownloadStateIcons = a.getBoolean(R.styleable.BookCover_showDownloadStateIcons, false);
            mShowLoadingIcon = a.getBoolean(R.styleable.BookCover_showLoadingIcon, false);
            mFadeImageIn = a.getBoolean(R.styleable.BookCover_fadeInImage, true);
            mShowComingSoon = a.getBoolean(R.styleable.BookCover_showComingSoon, true);
        } finally {
            a.recycle();
        }
    }

    /**
     * Enable/disable the image fade in functionality when displaying a new book cover.
     * @param fadeEnabled set to true to fade in new images
     */
    public void setImageFadeIn(boolean fadeEnabled) {
        mFadeImageIn = fadeEnabled;
    }

    /**
     * Load the image for the currently set book. This should be called when the image loading was suspended
     * when the book was set on this object.
     */
    public void resumeImageLoading() {
        if (mBook != null) {
            IMAGE_LOADER.get(mBook.getFormattedThumbnailUrl(), this, Priority.HIGH);
        }
    }

    /**
     * Sets the Book to display in this view.
     *
     * @param book the Book object
     */
    public void setBook(Book book) {
        setBook(book, false, false);
    }

    /**
     * Sets the Book to display in this view, with an additional parameter to suspend the image loading.
     *
     * @param book the Book object
     * @param suspendImageLoading set to true to suspend the image loading when setting the book
     * @param cancelExistingImageLoad if set to true any image that is still loading for this book will be cancelled.
     */
    public void setBook(Book book, boolean suspendImageLoading, boolean cancelExistingImageLoad) {

        if (book == null) {
            return;
        }

        boolean bookChanged = false;

        if (mBook == null || mBook.isbn == null || book.isbn == null ||!TextUtils.equals(mBook.isbn, book.isbn)) {
            setTag(book.id);
            bookChanged = true;
        }

        // Cancel loading of image for cover which is going off screen. If the cover has already been downloaded this has no effect.
        if (cancelExistingImageLoad && mBook != null && bookChanged && !TextUtils.isEmpty(mBook.cover_url)) {
            IMAGE_LOADER.cancel(mBook.getFormattedThumbnailUrl());
        }

        mBook = book;

        if (book.sample_book) {
            mImageBanner.setImageResource(R.drawable.ic_banner_sample);
            mImageBanner.setVisibility(View.VISIBLE);
        } else if (BBBCalendarUtil.isTimeWithinTimePeriodFromNow(book.purchase_date, BusinessRules.RECENT_BOOKS_TIME_PERIOD) &&
                (book.state == BBBContract.Books.BOOK_STATE_UNREAD || book.state == BBBContract.Books.BOOK_STATE_RECENTLY_PURCHASED) ) {
            mImageBanner.setImageResource(R.drawable.ic_banner_new);
            mImageBanner.setVisibility(View.VISIBLE);
        // In some cases we don't want to show coming soon (e.g. if the book is already in the library so check if we're allowed to display it)
        } else if (book.publication_date > System.currentTimeMillis() && mShowComingSoon) {
            mImageBanner.setImageResource(R.drawable.ic_banner_comingsoon);
            mImageBanner.setVisibility(View.VISIBLE);
        } else {
            mImageBanner.setVisibility(View.GONE);
        }

        if (mBook.getFormattedThumbnailUrl() != null && bookChanged) {
            setBitmap(null);
            if (! suspendImageLoading) {
                new Handler().post(new Runnable() {
                    public void run() {
                        IMAGE_LOADER.get(mBook.getFormattedThumbnailUrl(), BookCover.this, Priority.HIGH);
                    }
                });
            }
        }

        mTitle.setText(book.title);
        mAuthor.setText(book.author);

        postInvalidate();
    }

    /**
     * Force the size of the book image to a specific size
     * @param width the width to set
     * @param height the height to set
     */
    public void setBookImageSize(int width, int height) {
        mBookImage.setLayoutParams(new FrameLayout.LayoutParams(width, height));
    }

    /**
     * Sets the number to draw on the bottom right of the cover. set to null to hide number
     */
    public void setPosition(Integer position) {
        if (position != null) {
            mBookPosition.setText(String.valueOf(position));
            mBookPosition.setVisibility(View.VISIBLE);
        } else {
            mBookPosition.setVisibility(View.GONE);
        }
    }

    /**
     * Gets the Book object for this view
     *
     * @return the Book object
     */
    public Book getBook() {
        return mBook;
    }

    public void setBitmap(Bitmap bitmap) {

        if (mFadeImageIn) {
            Animation myFadeInAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.image_fade_in);
            mBookImage.startAnimation(myFadeInAnimation); //Set animation to your ImageView
        }
        mBookImage.setImageBitmap(bitmap);
    }

    /**
     * Sets the download progress which is displayed as a percentage. Should be between 0-100
     *
     * @param progress the current download progress
     */
    public void setDownloadProgress(int downloadStatus, double progress) {

        switch (downloadStatus) {
            case BBBContract.Books.DOWNLOADED:
                mDownloadButtonContainer.setVisibility(View.GONE);
                break;

            case BBBContract.Books.DOWNLOAD_FAILED:
                mDownloadButtonContainer.setVisibility(View.VISIBLE);
                mDownloadButtonContainer.setDisplayedChild(DOWNLOAD_VIEW_ERROR);
                break;

            case BBBContract.Books.NOT_DOWNLOADED:
                mDownloadButtonContainer.setVisibility(View.VISIBLE);
                mDownloadButtonContainer.setDisplayedChild(DOWNLOAD_VIEW_BUTTON);
                break;

            case BBBContract.Books.DOWNLOADING:
                mDownloadButtonContainer.setVisibility(View.VISIBLE);
                mDownloadButtonContainer.setDisplayedChild(DOWNLOAD_VIEW_PROGRESS);
                if (progress > 0 && progress <= 100) {
                    mDownloadPercent.setText(String.valueOf((int) progress));
                }
                break;
        }
    }

    //ImageListener callbacks
    @Override
    public void onErrorResponse(VolleyError error) {
    }

    @Override
    public void onResponse(ImageContainer container, boolean immediate) {

        if (container.getRequestUrl().equals(mBook.getFormattedThumbnailUrl())) {
            setBitmap(container.getBitmap());
        }
    }
}