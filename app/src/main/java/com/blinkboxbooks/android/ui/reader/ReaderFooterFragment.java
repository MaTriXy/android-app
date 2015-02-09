// Copyright (c) 2014 blinkbox Entertainment Limited. All rights reserved.
package com.blinkboxbooks.android.ui.reader;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.blinkbox.java.book.json.BBBSpineItem;
import com.blinkboxbooks.android.R;

import java.util.ArrayList;


/**
 * The reader footer fragment
 */
public class ReaderFooterFragment extends Fragment {

    private static final String PARAM_PROGRESS = "progress";
    private static final String PARAM_READER_SPINE = "readerSpine";
    private static final String PARAM_READER_CHAPTER = "readerChapter";
    private static final String PARAM_READER_PROGRESS = "readerProgress";

    private TextView mTextViewProgress;
    private TextView mTextViewChapter;
    private SeekBar mProgressBar;

    private ArrayList<BBBSpineItem>  mSpineItems;
    private SparseArray<BBBSpineItem> mSpineBoundaries;

    private int mProgress;
    private String mReaderChapter;
    private String mReaderProgress;


    public static ReaderFooterFragment newInstance(int progress, String readerChapter, String readerProgress, ArrayList<BBBSpineItem> spineItems) {
        ReaderFooterFragment readerFragment = new ReaderFooterFragment();

        Bundle args = new Bundle();

        args.putString(PARAM_READER_CHAPTER,readerChapter);
        args.putString(PARAM_READER_PROGRESS,readerProgress);
        args.putInt(PARAM_PROGRESS, progress);

        if(spineItems != null ) {
            args.putSerializable(PARAM_READER_SPINE, spineItems);
        }

        readerFragment.setArguments(args);

        return readerFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reader_footer, container, false);

        mTextViewProgress = (TextView) view.findViewById(R.id.textview_progress);
        mTextViewChapter = (TextView) view.findViewById(R.id.textview_chapter);
        mProgressBar = (SeekBar) view.findViewById(R.id.progress);

        Bundle bundle = getArguments();

        if(savedInstanceState != null) {
            bundle = savedInstanceState;
        }

        mProgress = bundle.getInt(PARAM_PROGRESS);
        mReaderChapter = bundle.getString(PARAM_READER_CHAPTER);
        mReaderProgress = bundle.getString(PARAM_READER_PROGRESS);

        mSpineItems = (ArrayList<BBBSpineItem>) bundle.getSerializable(PARAM_READER_SPINE);

        setSeekBarSections(mSpineItems);

        readerProgressUpdated(mProgress, mReaderChapter, mReaderProgress);
        view.bringToFront();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(PARAM_PROGRESS, mProgress);
        outState.putString(PARAM_READER_CHAPTER, mReaderChapter);
        outState.putString(PARAM_READER_PROGRESS, mReaderProgress);
        outState.putSerializable(PARAM_READER_SPINE, mSpineItems);
    }

    private void setSeekBarSections(ArrayList<BBBSpineItem> spineItems) {
        if(spineItems != null) {
            if (mSpineBoundaries == null) {
                mSpineBoundaries = new SparseArray<BBBSpineItem>(spineItems.size());
            }

            mSpineBoundaries.clear();

            for (BBBSpineItem spineItem:spineItems){
                if(mSpineBoundaries.get((int)spineItem.progress) == null) {
                    mSpineBoundaries.put((int)spineItem.progress, spineItem);
                }
            }
            mProgressBar.setMax(100);
        }
    }

    private BBBSpineItem getSpineItem(int progress) {
        if(mSpineBoundaries == null) {
            return null;
        }

        BBBSpineItem spineItem = mSpineBoundaries.get(progress);
        if (spineItem == null){
            int key;
            for(int i = mSpineBoundaries.size()-1; i >= 0  ; i--) {
                key = mSpineBoundaries.keyAt(i);
                if (key < progress){
                    spineItem = mSpineBoundaries.get(key);
                    break;
                }
            }
        }
        return spineItem;
    }

    public void readerProgressUpdated(int progress, final String readerChapter, final String readerProgress) {
        mProgress = progress;
        mReaderProgress = readerProgress;
        mReaderChapter = readerChapter;

        mTextViewChapter.setText(readerChapter);
        mTextViewProgress.setText(readerProgress);

        mProgressBar.setProgress(progress);
        mProgressBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
    }

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            BBBSpineItem spineItem = getSpineItem(progress);

            if (spineItem == null) {
                return;
            }

            mTextViewChapter.setText(spineItem.label);

            int percentage = (progress * 100)/seekBar.getMax();
            mTextViewProgress.setText(String.valueOf(percentage)+"%");
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {}

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            BBBSpineItem spineItem = getSpineItem(seekBar.getProgress());

            if(spineItem == null) {
                return;
            }

            Activity activity = getActivity();

            int seekBarProgress = seekBar.getProgress();
            float spineItemProgress = spineItem.progress;

            if(activity instanceof Reader) {
                ((Reader)activity).goToProgress(seekBarProgress == (int) spineItemProgress ?
                        (spineItemProgress > 100 ? 100 : spineItemProgress) : seekBarProgress);
            }
        }
    };
}