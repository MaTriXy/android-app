package com.blinkboxbooks.android.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;

/**
 * A very simple layout that is used to display a temporary information message.
 */
public class InfoPanel extends FrameLayout {

    private TextView mInfoText;
    private OnClickListener mDismissClickLister;
    private ViewGroup mContainer;

    public InfoPanel(Context context) {
        this(context, null);
    }

    public InfoPanel(Context context, AttributeSet attrs) {
        super(context, attrs);

        LayoutInflater mInflater = LayoutInflater.from(context);
        mInflater.inflate(R.layout.view_information_panel, this, true);

        mInfoText = (TextView) findViewById(R.id.view_information_panel_text);
        mContainer = (ViewGroup) findViewById(R.id.view_information_panel_container);
        final ImageView mDismissButton = (ImageView) findViewById(R.id.view_information_panel_dismiss_button);

        // When the user clicks the dismiss button we animate the container up so it dissapears from view and
        // then remove this whole widget from its parent.
        mDismissButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View view) {

                int finalHeight = getHeight();

                TranslateAnimation hideAnimation = new TranslateAnimation(0, 0, 0, -finalHeight);
                hideAnimation.setDuration(300);

                hideAnimation.setAnimationListener(new Animation.AnimationListener() {

                    @Override
                    public void onAnimationStart(Animation animation) {}

                    @Override
                    public void onAnimationEnd(Animation animation) {

                        ViewGroup parent = (ViewGroup) getParent();
                        if (parent != null) {
                            parent.removeView(InfoPanel.this);
                        }
                        // If we have a dismiss click listener registered invoke it.
                        if (mDismissClickLister != null) {
                            mDismissClickLister.onClick(view);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {}
                });
                mContainer.startAnimation(hideAnimation);
            }
        });
    }

    /**
     * Set the text to display in the info panel and provide an optional click listener for when the text is pressed
     * @param text the text to display
     * @param textClickListener an optional listener to handle clicking the text (null for no listener)
     */
    public void setText(String text, OnClickListener textClickListener) {
        mInfoText.setText(text);
        mInfoText.setOnClickListener(textClickListener);
    }

    /**
     * Set a custom handler for when the user clicks on the dismiss button.
     * @param dismissClickListener an OnClickListener object that will be invoked when the dismiss button is pressed
     */
    public void setDismissClickListener(OnClickListener dismissClickListener) {
        mDismissClickLister = dismissClickListener;
    }

    @Override
    public void setBackgroundColor(int color) {
        // Set the color on the container (not this view)
        mContainer.setBackgroundColor(color);
    }
}
