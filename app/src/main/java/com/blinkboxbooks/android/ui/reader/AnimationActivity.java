// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.ImageView;

import com.blinkbox.java.book.factory.BBBEPubFactory;
import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.ui.BaseActivity;
import com.blinkboxbooks.android.util.BBBAnimationUtils;
import com.blinkboxbooks.android.util.BBBAnimationUtils.AnimationListener;
import com.blinkboxbooks.android.util.BBBImageLoader;

/**
 * The Reader screen.
 */
public class AnimationActivity extends BaseActivity {

    public static final String PARAM_ANIMATION_RESOURCE = "PARAM_ANIMATION_RESOURCE";
    public static final String PARAM_ANIMATION_LOCATION = "PARAM_ANIMATION_LOCATION";

    /**
     * Check whether the activity can be started. If the activity is already running this will return false
     *
     * @return
     */
    public static boolean shouldStartActivity() {

        synchronized (sHasAlreadyLaunched) {

            if (sHasAlreadyLaunched) {
                return false;
            }

            return sHasAlreadyLaunched = true;
        }
    }

    private static Boolean sHasAlreadyLaunched = false;

    private ImageView mImageViewCover;

    private void performAnimation(String resourceUrl, AnimationListener animationListener) {
        Bundle bundle = getIntent().getExtras();
        int[] location = bundle.getIntArray(PARAM_ANIMATION_LOCATION);

        Bitmap bitmap = null;

        if (resourceUrl.startsWith("/")) {
            bitmap = BBBEPubFactory.getInstance().loadBitmapFromBook(this, resourceUrl, 0, 0, null);
        } else {
            bitmap = BBBImageLoader.getInstance().getCachedBitmap(resourceUrl);
        }

        if (bitmap == null) {
            // Cache miss - skip animation
            animationListener.onAnimationFinished();
            return;
        }

        mImageViewCover.setImageBitmap(bitmap);
        BBBAnimationUtils.zoomFromLocation(location, mImageViewCover, animationListener);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        synchronized (sHasAlreadyLaunched) {
            sHasAlreadyLaunched = false;
        }

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_animation);

        mImageViewCover = (ImageView) findViewById(R.id.imageview_cover);

        String resourceUrl = getIntent().getStringExtra(PARAM_ANIMATION_RESOURCE);
        if (savedInstanceState == null && resourceUrl != null) {
            performAnimation(resourceUrl, new AnimationListener() {
                public void onAnimationFinished() {
                    openBookAfterAnimation();
                }
            });
        } else {
            openBookAfterAnimation();
        }
    }

    private void openBookAfterAnimation() {
        Intent intent = getIntent();
        intent.setClass(this, ReaderActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        synchronized (sHasAlreadyLaunched) {
            sHasAlreadyLaunched = false;
        }
    }
}
