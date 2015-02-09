// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

public class BBBAnimationUtils {

    public interface AnimationListener {
        void onAnimationFinished();
    }

    private static final int ZOOM_ANIMATION_DURATION = 750;

    /**
     * Animation a view so that it zooms from a thumbnail
     *
     * @param thumbnailLocation The location to zoom from
     * @param view              The final view
     * @param listener          A listener that will be invoked when the animation completes
     */
    public static void zoomFromLocation(final int[] thumbnailLocation, final View view, final AnimationListener listener) {
        ViewTreeObserver observer = view.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {

            @Override
            public boolean onPreDraw() {
                view.getViewTreeObserver().removeOnPreDrawListener(this);

                final int[] imageLocation = getLocation(view);
                view.getLayoutParams().width = imageLocation[2];
                view.getLayoutParams().height = imageLocation[3];

                view.post(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(View.VISIBLE);

                        final int mLeftDelta = thumbnailLocation[0] - imageLocation[0];
                        final int mTopDelta = thumbnailLocation[1] - imageLocation[1];

                        // Scale factors to make the large version the same size as the thumbnail
                        float mWidthScale = (float) thumbnailLocation[2] / imageLocation[2];
                        float mHeightScale = (float) thumbnailLocation[3] / imageLocation[3];

                        // Set starting values for properties we're going to animate. These
                        // values scale and position the full size version down to the thumbnail
                        // size/location, from which we'll animate it back up
                        view.setPivotX(0);
                        view.setPivotY(0);
                        view.setScaleX(mWidthScale);
                        view.setScaleY(mHeightScale);
                        view.setTranslationX(mLeftDelta);
                        view.setTranslationY(mTopDelta);

                        // Animate scale and translation to go from thumbnail to full size
                        view.animate().setDuration(ZOOM_ANIMATION_DURATION).
                                scaleX(1).scaleY(1).
                                translationX(0).translationY(0).
                                setInterpolator(new DecelerateInterpolator()).
                                setListener(new AnimatorListener() {

                                    @Override
                                    public void onAnimationStart(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationRepeat(Animator animation) {

                                    }

                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (listener != null) {
                                            listener.onAnimationFinished();
                                        }
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {

                                    }
                                });
                    }
                });

                return true;
            }
        });
    }

    public static int[] getLocation(View view) {
        int[] imageLocation = new int[4];
        view.getLocationOnScreen(imageLocation);
        imageLocation[2] = view.getMeasuredWidth();
        imageLocation[3] = view.getMeasuredHeight();

        if (view instanceof ImageView) {
            ImageView imageView = (ImageView) view;

            int imageHeight = view.getMeasuredHeight();
            int imageWidth = view.getMeasuredWidth();
            int drawableHeight = imageView.getDrawable().getIntrinsicHeight();
            int drawableWidth = imageView.getDrawable().getIntrinsicWidth();

            float imageAspectRatio = (float) drawableHeight / (float) drawableWidth;
            float imageContainerAspectRatio = (float) imageHeight / (float) imageWidth;

            if (imageContainerAspectRatio < imageAspectRatio) {
                // tall thin image
                int newWidth = (int) (imageHeight / imageAspectRatio);
                imageLocation[0] += (imageWidth - newWidth) / 2;
                imageLocation[2] = newWidth;
            } else if (imageContainerAspectRatio > imageAspectRatio) {
                // short fat image
                int newHeight = (int) (imageWidth * imageAspectRatio);
                imageLocation[1] += (imageHeight - newHeight) / 2;
                imageLocation[3] = newHeight;
            }
        }

        return imageLocation;
    }

}
