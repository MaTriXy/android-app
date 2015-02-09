// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.ImageView;

import com.blinkboxbooks.android.R;
import com.blinkboxbooks.android.util.BBBAnimationUtils;

/**
 * A LibraryHelpDialogFragment that overlays the library 22.1.0.0
 */
@SuppressWarnings("unused")
public class ReaderHelpDialogFragment extends DialogFragment {

    public static final String PARAM_ANIMATION_STARTING_POINT = "animation_starting_point";
    public static final String PARAM_REQUEST_LAST_POSITION = "request_last_position";
    public static final String TAG_READER_OVERLAY = "reader_overlay";

    private ReaderLastPositionListener mReaderLastPositionListener;

    public static ReaderHelpDialogFragment newInstance(int[] fromCoordinates, boolean requestLastPosition) {
        ReaderHelpDialogFragment f = new ReaderHelpDialogFragment();

        Bundle args = new Bundle();
        args.putIntArray(PARAM_ANIMATION_STARTING_POINT, fromCoordinates);
        args.putBoolean(PARAM_REQUEST_LAST_POSITION, requestLastPosition);
        f.setArguments(args);
        return f;
    }

    /**
     * This listener will be invoked when the reader should request the last position from the server
     */
    public interface ReaderLastPositionListener {
        public void requestLastPosition();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof ReaderLastPositionListener) {
            mReaderLastPositionListener = (ReaderLastPositionListener) activity;
        }
    }

    /**
     * If you call this constructor directly, you must call
     * int[] fromCoordinates;
     * Bundle args = new Bundle();
     * args.putIntArray(PARAM_ANIMATION_STARTING_POINT, fromCoordinates);
     * f.setArguments(args);
     */
    public ReaderHelpDialogFragment() {
        super();
    }

    private OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            dismiss();
        }
    };

    private ImageView mImageViewOverlay;

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity());

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        boolean requestLastPosition = false;
        View layout = View.inflate(getActivity(), R.layout.layout_reader_overlay, null);
        if (getArguments() != null) {
            int[] animationCoordinates = getArguments().getIntArray(PARAM_ANIMATION_STARTING_POINT);
            requestLastPosition = getArguments().getBoolean(PARAM_REQUEST_LAST_POSITION);

            if (animationCoordinates != null) {
                BBBAnimationUtils.zoomFromLocation(animationCoordinates, layout, null);
            }
        }

        Button dismissButton = (Button) layout.findViewById(R.id.button_thats_it);
        layout.setOnClickListener(mOnClickListener);
        dismissButton.setOnClickListener(mOnClickListener);

        mImageViewOverlay = (ImageView) layout.findViewById(R.id.imageview_overlay);

        dialog.setContentView(layout);
        dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        if (requestLastPosition) {
            dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (mReaderLastPositionListener != null) {
                        mReaderLastPositionListener.requestLastPosition();
                    }
                }
            });
        }

        return dialog;
    }
}
