// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.cache.TypefaceCache;

/**
 * Custom AlertDialogBuilder which allows us to customize styles which cannot be acheived via a theme
 */
public class BBBAlertDialogBuilder extends Builder {

    private View mDialogView;

    private TextView mTitle;

    private ImageView mIcon;

    private TextView mMessage;

    private View mViewTitleContainer;

    public BBBAlertDialogBuilder(Context context) {

        super(context, R.style.AlertDialog);

        mDialogView = View.inflate(context, R.layout.layout_alertdialog, null);
        super.setView(mDialogView);

        mTitle = (TextView) mDialogView.findViewById(R.id.textview_title);
        mMessage = (TextView) mDialogView.findViewById(R.id.textview_message);
        mIcon = (ImageView) mDialogView.findViewById(R.id.imageview_icon);
        mViewTitleContainer = mDialogView.findViewById(R.id.layout_title_container);
    }

    /**
     * Constructor with option to remove padding
     * @param context the context
     * @param removePadding if set to true the alert dialog will remove all default padding
     */
    public BBBAlertDialogBuilder(Context context, boolean removePadding) {

        this(context);

        if (removePadding) {
            mDialogView.findViewById(R.id.alert_dialog_container).setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public AlertDialog create() {

        final AlertDialog dialog = super.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface d) {
                Resources resources = getContext().getResources();
                Typeface typeFace = TypefaceCache.getInstance().getTypeface(getContext(), R.string.font_lola_medium);
                int dimen = getContext().getResources().getDimensionPixelOffset(R.dimen.text_size_buttons);

                Button buttonNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                Button buttonNeutral = dialog.getButton(DialogInterface.BUTTON_NEUTRAL);
                Button buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE);

                if (buttonNeutral.getVisibility() == View.VISIBLE) {
                    buttonNeutral.setEnabled(true);
                    buttonNeutral.setBackgroundResource(R.drawable.btn_grey_background_alert_dialog);
                    buttonNeutral.setTextColor(resources.getColor(R.color.btn_grey_textcolor));
                    buttonNeutral.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimen);
                    buttonNeutral.setTypeface(typeFace);
                }

                if (buttonNegative.getVisibility() == View.VISIBLE) {
                    buttonNegative.setEnabled(true);
                    buttonNegative.setBackgroundResource(R.drawable.btn_grey_background_alert_dialog);
                    buttonNegative.setTextColor(resources.getColor(R.color.btn_grey_textcolor));
                    buttonNegative.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimen);
                    buttonNegative.setTypeface(typeFace);
                }

                if (buttonPositive.getVisibility() == View.VISIBLE) {
                    buttonPositive.setTypeface(typeFace);
                    buttonPositive.setTextSize(TypedValue.COMPLEX_UNIT_PX, dimen);
                }
            }
        });

        return dialog;
    }

    @Override
    public BBBAlertDialogBuilder setTitle(CharSequence text) {
        mViewTitleContainer.setVisibility(text != null ? View.VISIBLE : View.GONE);

        mTitle.setText(text);
        return this;
    }

    @Override
    public BBBAlertDialogBuilder setTitle(int textResourceId) {
        mViewTitleContainer.setVisibility(View.VISIBLE);
        mTitle.setText(textResourceId);
        return this;
    }

    public BBBAlertDialogBuilder setTitleColor(String colorString) {
        mTitle.setTextColor(Color.parseColor(colorString));
        return this;
    }

    @Override
    public BBBAlertDialogBuilder setMessage(int textResId) {
        mMessage.setVisibility(View.VISIBLE);
        mMessage.setText(textResId);
        return this;
    }

    @Override
    public BBBAlertDialogBuilder setMessage(CharSequence text) {
        mMessage.setVisibility(text != null ? View.VISIBLE : View.GONE);
        mMessage.setText(text);
        return this;
    }

    @Override
    public BBBAlertDialogBuilder setIcon(int drawableResId) {
        mViewTitleContainer.setVisibility(View.VISIBLE);
        mIcon.setVisibility(View.VISIBLE);
        mIcon.setImageResource(drawableResId);
        return this;
    }

    @Override
    public BBBAlertDialogBuilder setIcon(Drawable icon) {

        mViewTitleContainer.setVisibility(icon != null ? View.VISIBLE : View.GONE);
        mIcon.setVisibility(View.VISIBLE);
        mIcon.setImageDrawable(icon);
        return this;
    }

    @Override
    public BBBAlertDialogBuilder setView(View view) {
        FrameLayout container = ((FrameLayout) mDialogView.findViewById(R.id.layout_container));
        container.setVisibility(View.VISIBLE);
        container.addView(view);
        return this;
    }

    private void setMargins(Button button) {
        MarginLayoutParams params = (MarginLayoutParams) button.getLayoutParams();

        int margin = (int) getContext().getResources().getDisplayMetrics().density * 4;
        params.setMargins(margin, margin, margin, margin / 2);

        button.setLayoutParams(params);
    }
}